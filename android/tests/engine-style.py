"""Check engine screens reached through FamilyHome's public Games tile."""

import importlib.util
import json
from pathlib import Path
import re
import struct
import subprocess
import pytest

spec = importlib.util.spec_from_file_location(
    "game_navigation", Path(__file__).with_name("games-toolbar.py")
)
nav = importlib.util.module_from_spec(spec)
spec.loader.exec_module(nav)
OUTPUT = Path("android/build/engine-style/evidence")
OCR = Path("android/build/engine-style/ocr")


def capture(name: str) -> tuple[Path, list[dict]]:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    scale = nav.command("shell", "settings", "get", "system", "font_scale").strip()
    path = OUTPUT / f"{name}-{scale}.png"
    path.write_bytes(subprocess.check_output(nav.ADB + ["exec-out", "screencap", "-p"]))
    return path, json.loads(subprocess.check_output([str(OCR), str(path)], text=True))


def wait_text(name: str, label: str) -> list[dict]:
    for attempt in range(30):
        path, words = capture(name)
        if any(
            re.sub(r"[^a-z0-9]", "", label.casefold())
            in re.sub(r"[^a-z0-9]", "", word["text"].casefold())
            for word in words
        ):
            return words
        # The offline choice is part of the game's first-run fixture.
        if any("privacy policy" in word["text"] for word in words):
            nav.command("shell", "input", "tap", "458", "600")
    raise AssertionError(f"{label} was not readable in {path}")


def settled_capture(name: str) -> None:
    """Wait for text bounds to stop moving during the engine panel animation."""
    previous = None
    for attempt in range(12):
        path, words = capture(name)
        bounds = [
            (
                word["text"],
                round(word["x"]),
                round(word["y"]),
                round(word["width"]),
                round(word["height"]),
            )
            for word in words
        ]
        if bounds == previous:
            return
        previous = bounds
    raise AssertionError(f"The text did not settle: {path}")


def cream_surface() -> None:
    raw = subprocess.check_output(nav.ADB + ["exec-out", "screencap"])
    width, height, fmt = struct.unpack_from("<III", raw)
    assert fmt == 1, "Expected RGBA screenshot"
    pixels = raw[-width * height * 4 :]
    cream = sum(
        pixels[i : i + 3] == bytes((255, 251, 239)) for i in range(0, len(pixels), 16)
    )
    assert cream > width * height // 16, (
        "The engine menu does not use the cream appearance"
    )


@pytest.mark.parametrize(
    "game,package",
    [("Kart", "org.supertuxkart.stk"), ("Freedoom", "net.nullsum.freedoom")],
)
def test_engine_screens(game: str, package: str) -> None:
    nav.command("shell", "am", "force-stop", package)
    nav.command("shell", "am", "start", "-W", "-n", "com.mprlab.portal/.MainActivity")
    # This test requires a child profile and the supported 1280 x 800 Portal layout.
    nav.command("shell", "input", "tap", "884", "679")
    nav.tap(nav.control(nav.snapshot(), game))
    wait_text(
        game.lower() + "-launch", "New game" if game == "Freedoom" else "Singleplayer"
    )
    state = nav.command("shell", "dumpsys", "activity", "activities")
    assert re.search(r"mResumedActivity:.*" + re.escape(package), state), (
        f"{game} did not launch"
    )
    if game == "Freedoom":
        wait_text("freedoom-menu", "New game")
        cream_surface()
        return
    words = wait_text("kart-menu", "Singleplayer")
    cream_surface()
    for label in ["Story Mode", "Singleplayer", "Online", "Addons"]:
        word = next(word for word in words if word["text"] == label)
        assert word["height"] >= 20, f"{label} is too small"
    nav.command("shell", "input", "tap", "408", "508")
    wait_text("kart-selection", "Choose a kart")
    cream_surface()
    nav.command("shell", "input", "tap", "1161", "678")
    words = wait_text("kart-race-setup", "Novice")
    cream_surface()
    # The Normal Race icon, followed by the fixed Nessie's Pond track fixture.
    nav.command("shell", "input", "tap", "140", "520")
    wait_text("kart-tracks", "All Tracks")
    nav.command("shell", "input", "tap", "636", "490")
    words = wait_text("kart-track-options", "Nessie's Pond")
    cream_surface()
    nav.command("shell", "input", "tap", "1066", "690")
    wait_text("kart-countdown", "Ready")
    nav.command("shell", "input", "keyevent", "4")
    wait_text("kart-pause", "Paused")
    cream_surface()
    settled_capture("kart-pause")
    # Home must preserve the active game task for the next Games launch.
    nav.command("shell", "am", "start", "-W", "-n", "com.mprlab.portal/.MainActivity")
    nav.command("shell", "input", "tap", "884", "679")
    nav.tap(nav.control(nav.snapshot(), "Kart"))
    wait_text("kart-resumed", "Paused")
    nav.command("shell", "input", "tap", "1035", "505")
    wait_text("kart-help", "General")
    cream_surface()
    nav.command("shell", "input", "tap", "794", "320")
    wait_text("kart-help-expanded", "gauge")
    cream_surface()
    settled_capture("kart-help-expanded")


def test_kart_results() -> None:
    """Reach results through the public Follow the Leader elimination rule."""
    nav.command("shell", "am", "force-stop", "org.supertuxkart.stk")
    nav.command("shell", "am", "start", "-W", "-n", "com.mprlab.portal/.MainActivity")
    nav.command("shell", "input", "tap", "884", "679")
    nav.tap(nav.control(nav.snapshot(), "Kart"))
    wait_text("kart-results-menu", "Singleplayer")
    nav.command("shell", "input", "tap", "408", "508")
    wait_text("kart-results-selection", "Choose a kart")
    nav.command("shell", "input", "tap", "1161", "678")
    wait_text("kart-results-setup", "Novice")
    nav.command("shell", "input", "tap", "425", "520")
    wait_text("kart-results-tracks", "All Tracks")
    nav.command("shell", "input", "tap", "636", "490")
    wait_text("kart-results-track", "Nessie's Pond")
    nav.command("shell", "input", "tap", "1066", "690")
    wait_text("kart-results-countdown", "Ready")
    # An idle kart finishes last and is eliminated by the real game clock.
    import time

    deadline = time.monotonic() + 120
    while time.monotonic() < deadline:
        path, words = capture("kart-results")
        if any("back to the menu" in word["text"].casefold() for word in words):
            score_contrast()
            return
    raise AssertionError(f"The elimination did not reach readable results: {path}")


def score_contrast() -> None:
    raw = subprocess.check_output(nav.ADB + ["exec-out", "screencap"])
    width, height, fmt = struct.unpack_from("<III", raw)
    assert fmt == 1
    pixels = raw[-width * height * 4 :]
    samples = [
        pixels[(y * width + x) * 4 : (y * width + x) * 4 + 3]
        for y in range(60, 550, 4)
        for x in range(80, 1190, 4)
    ]
    assert sum(pixel == bytes((0, 0, 0)) for pixel in samples) > len(samples) * 0.8, (
        "The fixed white score text needs a black score panel"
    )
