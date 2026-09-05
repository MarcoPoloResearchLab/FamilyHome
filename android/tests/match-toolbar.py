#!/usr/bin/env python3
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

serial = os.environ['ANDROID_SERIAL']
if not serial.startswith('emulator-'):
    raise SystemExit('Use a dedicated emulator for the Match toolbar test.')
adb = [os.environ['ANDROID_SDK_ROOT'] + '/platform-tools/adb', '-s', serial]
package = 'org.secuso.privacyfriendlymemory'
portal_package = 'com.mprlab.portal'

def command(*args: str) -> str:
    return subprocess.check_output(adb + list(args), text=True)

def snapshot() -> ET.Element:
    command('shell', 'uiautomator', 'dump', '/sdcard/toolbar-test.xml')
    return ET.fromstring(command('exec-out', 'cat', '/sdcard/toolbar-test.xml'))

def find(root: ET.Element, label: str, app_package: str = package) -> ET.Element | None:
    return next((node for node in root.iter('node') if node.get('package') == app_package
                 and (node.get('content-desc') == label or node.get('text', '').casefold() == label.casefold())), None)

def bounds(node: ET.Element) -> list[int]:
    return list(map(int, re.findall(r'\d+', node.get('bounds'))))

def click(node: ET.Element) -> None:
    left, top, right, bottom = bounds(node)
    command('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))

def wait_label(label: str, app_package: str = package) -> tuple[ET.Element, ET.Element]:
    for _ in range(8):
        root = snapshot()
        node = find(root, label, app_package)
        if node is not None:
            return root, node
        time.sleep(.25)
    raise AssertionError(f'Match control missing: {label}')

def open_match() -> None:
    _, games = wait_label('Games', portal_package)
    click(games)
    _, match = wait_label('Match', portal_package)
    click(match)

def prepare_familyhome() -> None:
    command('shell', 'am', 'start', '-W', '-n', portal_package + '/.MainActivity')
    root = snapshot()
    settings = find(root, 'Open settings', portal_package)
    if settings is not None:
        click(settings)
        root = snapshot()
    if find(root, 'No child spaces yet', portal_package) is not None:
        _, add = wait_label('＋  Add a child', portal_package)
        click(add)
        root = snapshot()
        name = next(node for node in root.iter('node') if node.get('class') == 'android.widget.EditText')
        click(name)
        command('shell', 'input', 'text', 'ToolbarTest')
        # Save through the dialog even when the keyboard changes its geometry.
        _, save = wait_label('Save', portal_package)
        click(save)
    _, home = wait_label('Home', portal_package)
    click(home)

def game_activity() -> str:
    state = command('shell', 'dumpsys', 'activity', 'activities')
    activity = re.search(r'(?:mResumedActivity|topResumedActivity)[^\n]*(ActivityRecord\{[^}\n]* '
                         + re.escape(package + '/.ui.MemoActivity') + r' t\d+\})', state)
    assert activity is not None, 'The active Match game is not in the foreground'
    return activity.group(1)

def toolbar() -> tuple[ET.Element, ET.Element, ET.Element]:
    root, back = wait_label('Back')
    home = find(root, 'Home')
    assert home is not None, 'Match Home control missing'
    back_box, home_box = bounds(back), bounds(home)
    assert back_box[1] == home_box[1] and back_box[3] == home_box[3], 'Navigation rows differ'
    assert back_box[1] < 16 and back_box[3] <= 72, f'Toolbar must occupy only the first row: {back_box}'
    assert back_box[2] - back_box[0] >= 48 and back_box[3] - back_box[1] >= 48, 'Back touch target too small'
    return root, back, home

def test_match_toolbar() -> None:
    command('shell', 'am', 'force-stop', package)
    prepare_familyhome()
    open_match()
    root = snapshot()
    welcome = find(root, 'Okay')
    if welcome is not None:
        click(welcome)
    root, back, home = toolbar()
    print('Match toolbar geometry passed on the game menu.')
    # Enter the running game through its public menu.
    root = snapshot()
    play = next(n for n in root.iter('node') if n.get('resource-id', '').endswith('/playButton'))
    click(play)
    root, back, home = toolbar()
    assert any(n.get('resource-id', '').endswith('/timerView') for n in root.iter('node')), 'Game timer missing'
    assert any(n.get('resource-id', '').endswith('/difficultyText') for n in root.iter('node')), 'Difficulty missing'
    menu = find(root, 'Game menu')
    assert menu is not None, 'Game menu missing'
    click(menu)
    _, help_item = wait_label('Help')
    click(help_item)
    toolbar()
    _, back = wait_label('Back')
    click(back)
    root, back, home = toolbar()
    output = Path('android/build/match-portal')
    output.mkdir(parents=True, exist_ok=True)
    (output / 'combined-toolbar.png').write_bytes(subprocess.check_output(adb + ['exec-out', 'screencap', '-p']))
    click(back)
    root, cancel = wait_label('No')
    click(cancel)
    toolbar()
    root, back, home = toolbar()
    active_game = game_activity()
    click(home)
    for _ in range(20):
        activity = command('shell', 'dumpsys', 'activity', 'activities')
        if re.search(r'(?:mResumedActivity|topResumedActivity).*com.mprlab.portal/.MainActivity', activity):
            break
        time.sleep(.25)
    else:
        raise AssertionError('Home did not return to FamilyHome')
    assert re.search(r'\* Hist\s+#\d+: ' + re.escape(active_game), activity), 'Home destroyed the active Match game'
    open_match()
    toolbar()
    assert game_activity() == active_game, 'The Games tile replaced the active Match game'
    assert any(node.get('resource-id', '').endswith('/timerView') for node in snapshot().iter('node')), 'The Games tile did not resume the active game'
    print('Match toolbar passed: one row, game status, Back confirmation, Home, and game resume.')
