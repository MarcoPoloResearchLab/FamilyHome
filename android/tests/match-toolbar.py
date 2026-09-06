#!/usr/bin/env python3
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET
import pytest

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

def board_fills_available_space(root: ET.Element, card_count: int) -> None:
    grid = next(node for node in root.iter('node') if node.get('resource-id', '').endswith('/gridview'))
    cards = list(grid)
    assert len(cards) == card_count, f'Expected all {card_count} cards without scrolling, got {len(cards)}'
    boxes = [bounds(card) for card in cards]
    screen = bounds(next(root.iter('node')))
    stats_bottom = max(bounds(node)[3] for node in root.iter('node')
                       if '/player_' in node.get('resource-id', ''))
    left = min(box[0] for box in boxes)
    top = min(box[1] for box in boxes)
    right = max(box[2] for box in boxes)
    bottom = max(box[3] for box in boxes)
    available = min(screen[2] - screen[0], screen[3] - stats_bottom)
    assert bottom - top >= available * .94, (
        f'Board must use available space: board={[left, top, right, bottom]}, '
        f'screen={screen}, stats_bottom={stats_bottom}')
    assert top >= stats_bottom and bottom <= screen[3], 'Cards overlap scores or leave the screen'
    assert left >= screen[0] and right <= screen[2], 'Cards leave the screen horizontally'
    assert abs((left - screen[0]) - (screen[2] - right)) <= 4, 'Board is not centered'
    for box in boxes:
        assert abs((box[2] - box[0]) - (box[3] - box[1])) <= 1, f'Card is not square: {box}'
    print(f'Match board passed: {card_count} cards, bounds={[left, top, right, bottom]}')

def match_menu(difficulty: int) -> None:
    command('shell', 'am', 'force-stop', package)
    prepare_familyhome()
    open_match()
    root = snapshot()
    welcome = find(root, 'Okay')
    if welcome is not None:
        click(welcome)
    root = snapshot()
    rating = next(node for node in root.iter('node') if node.get('resource-id', '').endswith('/difficultyBar'))
    left, top, right, bottom = bounds(rating)
    command('shell', 'input', 'tap', str(left + (right - left) * (2 * difficulty - 1) // 6),
            str((top + bottom) // 2))

def test_match_toolbar() -> None:
    match_menu(1)
    root, back, home = toolbar()
    print('Match toolbar geometry passed on the game menu.')
    # Enter the running game through its public menu.
    root = snapshot()
    play = next(n for n in root.iter('node') if n.get('resource-id', '').endswith('/playButton'))
    click(play)
    root, back, home = toolbar()
    assert any(n.get('resource-id', '').endswith('/timerView') for n in root.iter('node')), 'Game timer missing'
    assert any(n.get('resource-id', '').endswith('/difficultyText') for n in root.iter('node')), 'Difficulty missing'
    board_fills_available_space(root, 16)
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

@pytest.mark.parametrize(('screen_size', 'difficulty', 'card_count'), [
    ('1280x800', 2, 36),
    ('1920x1080', 3, 64),
    ('800x1280', 1, 16),
])
def test_match_board(screen_size: str, difficulty: int, card_count: int) -> None:
    original_size = command('shell', 'wm', 'size')
    override = re.search(r'Override size: (\d+x\d+)', original_size)
    try:
        command('shell', 'wm', 'size', screen_size)
        match_menu(difficulty)
        root = snapshot()
        click(next(node for node in root.iter('node') if node.get('resource-id', '').endswith('/playButton')))
        root, _, _ = toolbar()
        board_fills_available_space(root, card_count)
        grid = next(node for node in root.iter('node') if node.get('resource-id', '').endswith('/gridview'))
        click(list(grid)[0])
        root = snapshot()
        grid = next(node for node in root.iter('node') if node.get('resource-id', '').endswith('/gridview'))
        click(list(grid)[-1])
        root = snapshot()
        tries = next(node for node in root.iter('node') if node.get('resource-id', '').endswith('/player_one_tries_value'))
        assert tries.get('text') == '1', 'Cards at opposite board corners must respond to touch'
        output = Path('android/build/match-portal')
        output.mkdir(parents=True, exist_ok=True)
        (output / f'board-{screen_size}-{card_count}.png').write_bytes(
            subprocess.check_output(adb + ['exec-out', 'screencap', '-p']))
    finally:
        command('shell', 'wm', 'size', override.group(1) if override else 'reset')
