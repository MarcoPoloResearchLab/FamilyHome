"""Exercise the installed game toolbar through Android UI automation."""
import os
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
import pytest

SERIAL = os.environ["ANDROID_SERIAL"]
assert SERIAL.startswith("emulator-"), "Use a dedicated emulator."
ADB = [os.environ["ANDROID_SDK_ROOT"] + "/platform-tools/adb", "-s", SERIAL]
GAMES = {"blocks": "com.blockdrop.game/.MainActivity", "tiles": "net.vantulder.tessel/.MainActivity"}

def command(*args: str) -> str:
    return subprocess.check_output(ADB + list(args), text=True)

def snapshot() -> ET.Element:
    command("shell", "uiautomator", "dump", "/sdcard/games-toolbar.xml")
    return ET.fromstring(command("exec-out", "cat", "/sdcard/games-toolbar.xml"))

def control(root: ET.Element, label: str) -> ET.Element:
    nodes = [n for n in root.iter("node") if n.get("bounds") != "[0,0][0,0]"]
    found = next((n for n in nodes if n.get("content-desc") == label), None)
    if found is None:
        found = next((n for n in nodes if n.get("text", "").casefold() == label.casefold()), None)
    assert found is not None, f"Missing toolbar control: {label}"
    return found

def box(node: ET.Element) -> list[int]:
    return list(map(int, re.findall(r"\d+", node.attrib["bounds"])))

def tap(node: ET.Element) -> None:
    x1, y1, x2, y2 = box(node)
    command("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

def check_toolbar(root: ET.Element) -> None:
    back, home = control(root, "Back"), control(root, "Home")
    a, b = box(back), box(home)
    assert a[1] == b[1] and a[3] == b[3], "Back and Home use different rows"
    assert a[1] < 16 and a[3] <= 72, f"Extra top row: {a}"
    for node in (back, home):
        x1, y1, x2, y2 = box(node)
        assert x2-x1 >= 48 and y2-y1 >= 48, "Navigation touch target too small"

@pytest.fixture(scope="session", autouse=True)
def familyhome_profile() -> None:
    command("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME", "-n", "com.mprlab.portal/.MainActivity")
    root = snapshot()
    if any(n.get("content-desc") == "Open settings" for n in root.iter("node")):
        tap(control(root, "Open settings"))
        root = snapshot()
    if any(n.get("text") == "No child spaces yet" for n in root.iter("node")):
        tap(control(root, "＋  Add a child"))
        root = snapshot()
        name = next(n for n in root.iter("node") if n.get("class") == "android.widget.EditText")
        tap(name)
        command("shell", "input", "text", "ToolbarTest")
        command("shell", "input", "keyevent", "4")
        tap(control(snapshot(), "Save"))
        root = snapshot()
    tap(control(root, "Home"))
    snapshot()

@pytest.mark.parametrize("game", GAMES)
def test_game_toolbar(game: str) -> None:
    command("shell", "am", "force-stop", GAMES[game].split("/")[0])
    command("shell", "am", "start", "-W", "-n", GAMES[game])
    # Wait for the WebView or Flutter accessibility tree after the launch screen.
    for attempt in range(5):
        root = snapshot()
        if any(n.get("content-desc") == "Back" for n in root.iter("node")):
            break
    check_toolbar(root)
    if game == "blocks":
        for label in ("Settings",):
            tap(control(root, label))
            root = snapshot()
            check_toolbar(root)
        tap(control(root, "Back"))
        root = snapshot()
        check_toolbar(root)
        status = [n for n in root.iter("node") if "Score:" in n.get("content-desc", "")]
        assert status and box(status[0])[3] <= 64, "Score uses a second row"
    else:
        if any(n.get("content-desc") == "Show menu" for n in root.iter("node")):
            tap(control(root, "Back"))
            root = snapshot()
            tap(control(root, "Back to menu"))
            root = snapshot()
        for label in ("All Games", "Paint", "Settings", "How to play?", "Statistics", "Tiles menu"):
            tap(control(root, label))
            root = snapshot()
            check_toolbar(root)
            assert box(control(root, label))[3] <= 64, f"Tiles menu outside toolbar: {label}"
        # First triangle game card on the fixed Portal viewport.
        command("shell", "input", "tap", "195", "400")
        root = snapshot()
        check_toolbar(root)
        assert box(control(root, "Show menu"))[3] <= 64, "Game menu uses a second row"
        tap(control(root, "Back"))
        root = snapshot()
        control(root, "Back to menu")
        tap(control(root, "Show menu"))
        root = snapshot()
    directory = Path("android/build/toolbar-audit")
    (directory / f"{game}-toolbar.png").write_bytes(subprocess.check_output(ADB + ["exec-out", "screencap", "-p"]))
    tap(control(root, "Home"))
    snapshot()
    state = command("shell", "dumpsys", "activity", "activities")
    assert re.search(r"(?:mResumedActivity|topResumedActivity).*com.mprlab.portal/.MainActivity", state), "Home did not return to FamilyHome"
