import os
from pathlib import Path
import shlex
import subprocess

import pytest


PACKAGE = 'com.mprlab.portal.cameraqualification'
ANDROID = Path(__file__).resolve().parents[1]


def installed_package(adb: Path) -> bool:
    result = subprocess.run([str(adb), 'shell', 'pm', 'path', PACKAGE], capture_output=True, text=True)
    assert result.returncode in (0, 1) and not result.stderr.strip(), result.stderr
    return result.stdout.startswith('package:')


@pytest.mark.parametrize('failure', ['qualification', 'export', 'both'])
def test_camera_failure_preserves_evidence(tmp_path: Path, failure: str) -> None:
    serial = os.environ['ANDROID_SERIAL']
    assert serial.startswith('emulator-'), 'Camera recovery tests require a dedicated emulator'
    adb = Path(os.environ['ANDROID_SDK_ROOT']) / 'platform-tools/adb'
    assert not installed_package(adb), (
        'Preserve the installed qualification application before running recovery tests')
    sources = [ANDROID / 'tests/camera-qualification.sh',
               ANDROID / 'app/src/main/java/com/mprlab/portal/PortalCamera.java',
               *(ANDROID / 'tests/camera-qualification').glob('*')]
    for source in sources:
        destination = tmp_path / 'android' / source.relative_to(ANDROID)
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(source.read_bytes())
    hook = tmp_path / 'adb-failure.bash'
    hook.write_text('function ' + shlex.quote(str(adb)) + '''() {
    if [[ $# -ge 3 && "$1 $2 $3" == 'shell am instrument' && "$*" == *'-e phase capture '* ]]; then
        command "$CAMERA_TEST_ADB" "$@" > "$CAMERA_TEST_CAPTURE_LOG"
        if [[ "$CAMERA_TEST_FAILURE" == qualification || "$CAMERA_TEST_FAILURE" == both ]]; then
            sed '/Camera qualification passed:/d' "$CAMERA_TEST_CAPTURE_LOG"
            echo 'Injected qualification failure after capture'
        else
            cat "$CAMERA_TEST_CAPTURE_LOG"
        fi
        return 0
    fi
    if [[ "$1 $2" == 'exec-out run-as' && "$CAMERA_TEST_FAILURE" != qualification ]]; then
        echo 'incomplete transfer'
        echo 'Injected evidence export failure' >&2
        return 37
    fi
    command "$CAMERA_TEST_ADB" "$@"
}
''')
    environment = dict(os.environ, BASH_ENV=str(hook), CAMERA_TEST_ADB=str(adb),
                       CAMERA_TEST_FAILURE=failure, CAMERA_TEST_CAPTURE_LOG=str(tmp_path / 'capture.txt'))
    try:
        result = subprocess.run(['bash', str(tmp_path / 'android/tests/camera-qualification.sh')],
                                env=environment, capture_output=True, text=True, timeout=180)
        (tmp_path / 'runner.log').write_text(result.stdout + result.stderr)
        assert (tmp_path / 'capture.txt').exists(), result.stdout + result.stderr
        assert 'Camera qualification passed:' in (tmp_path / 'capture.txt').read_text()
        assert result.returncode != 0, result.stdout + result.stderr
        installed = installed_package(adb)
        output = tmp_path / 'android/build/tests/camera-qualification'
        if failure == 'qualification':
            assert result.returncode == 1, 'The runner must preserve the qualification failure status'
            assert (output / 'camera-qualification.jpg').read_bytes().startswith(b'\xff\xd8')
            assert (output / 'camera-qualification.png').read_bytes().startswith(b'\x89PNG')
            assert not installed, 'Successful evidence export must allow test application cleanup'
        else:
            assert installed, 'An export failure must retain the application and its original evidence'
            assert not (output / 'camera-qualification.jpg').exists(), 'A partial export must not replace evidence'
            assert subprocess.check_output([str(adb), 'exec-out', 'run-as', PACKAGE,
                                            'cat', 'files/camera-qualification.jpg']).startswith(b'\xff\xd8')
            if failure == 'both':
                assert result.returncode == 1, 'An export failure must not replace the qualification failure status'
    finally:
        if installed_package(adb):
            subprocess.run([str(adb), 'uninstall', PACKAGE], check=True, capture_output=True)
