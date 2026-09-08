"""Verify interaction assets in the APK produced by make build-kart."""

import io
import xml.etree.ElementTree as ET
import zipfile
from collections import Counter
from collections.abc import Iterator
from pathlib import Path

import pytest
from PIL import Image, ImageDraw, ImageFont

APK = Path("android/build/kart-portal/Kart-Portal-unsigned.apk")
SKIN = "assets/data/skins/classic/"
OUTPUT = Path("android/build/engine-style/skin-evidence")
Skin = tuple[zipfile.ZipFile, ET.Element]


@pytest.fixture
def skin() -> Iterator[Skin]:
    with zipfile.ZipFile(APK) as apk:
        yield apk, ET.fromstring(apk.read(SKIN + "stkskin.xml"))


@pytest.mark.parametrize("scale", (1.0, 1.3))
def test_selected_text_remains_readable(skin: Skin, scale: float) -> None:
    apk, root = skin
    marked = root.find("color[@type='text_field'][@state='background_marked']")
    rgba = tuple(int(marked.attrib[key]) for key in ("r", "g", "b", "a"))
    assert 0 < rgba[3] < 255, "Text selection must remain translucent over the glyphs"
    background = root.find("color[@type='text_field'][@state='background_focused']")
    paper = tuple(int(background.attrib[key]) for key in ("r", "g", "b", "a"))
    font = ImageFont.truetype(
        io.BytesIO(apk.read("assets/data/ttf/Fredoka-Bold.ttf")), round(32 * scale)
    )
    image = Image.new("RGBA", (640, 100), paper)
    draw = ImageDraw.Draw(image)
    draw.text((20, 20), "Player One", font=font, fill="black")
    # The native renderer composites the selection AFTER drawing the glyphs.
    selected = Image.alpha_composite(image, Image.new("RGBA", image.size, rgba))
    selected_background = selected.getpixel((0, 0))[:3]
    text_pixels = [
        selected.getpixel((x, y))[:3]
        for y in range(100)
        for x in range(640)
        if image.getpixel((x, y))[:3] == (0, 0, 0)
    ]
    assert text_pixels, "The test must render actual text from the packaged font"
    assert contrast(selected_background, text_pixels[0]) >= 4.5, (
        "Selected text needs readable contrast"
    )
    assert sum(abs(a - b) for a, b in zip(selected_background, paper[:3])) >= 50, (
        "Selection must differ from the field background"
    )
    OUTPUT.mkdir(parents=True, exist_ok=True)
    selected.save(OUTPUT / f"selected-text-{scale}.png")


def contrast(first: tuple[int, ...], second: tuple[int, ...]) -> float:
    def luminance(color: tuple[int, ...]) -> float:
        channels = [c / 255 for c in color]
        linear = [
            c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
            for c in channels
        ]
        return sum(a * b for a, b in zip(linear, (0.2126, 0.7152, 0.0722)))

    values = sorted((luminance(first), luminance(second)))
    return (values[1] + 0.05) / (values[0] + 0.05)


def test_player_focus_matches_distinct_player_spinners(skin: Skin) -> None:
    apk, root = skin
    identities = []
    preview = Image.new("RGBA", (5 * 220, 300), "#fffbef")
    for player in range(1, 6):
        colors = []
        for kind, y in ((f"squareFocusHalo{player}", 0), (f"spinner{player}", 210)):
            element = root.find(f"element[@type='{kind}']")
            image = Image.open(
                io.BytesIO(apk.read(SKIN + element.attrib["image"]))
            ).convert("RGBA")
            pixels = list(image.getdata())
            colored = Counter(
                pixel[:3]
                for pixel in pixels
                if pixel[3] == 255 and max(pixel[:3]) - min(pixel[:3]) > 40
            )
            assert colored, f"{kind} must expose a player color"
            colors.append(colored.most_common(1)[0][0])
            assert (0, 0, 0, 255) in pixels, f"{kind} must retain its black outline"
            if kind.startswith("squareFocusHalo"):
                assert image.getpixel((image.width // 2, image.height // 2))[3] == 0, (
                    "A focus halo must not obscure its selection"
                )
                # The color must reach the edge slice that the engine stretches.
                border = int(element.attrib["top_border"])
                assert any(
                    image.getpixel((image.width // 2, y))[:3] == colors[-1]
                    for y in range(border)
                ), "The edge slice must retain the player color"
            preview.alpha_composite(image, ((player - 1) * 220 + 10, y))
        assert colors[0] == colors[1], (
            f"Player {player} halo and name spinner must match"
        )
        identities.append(colors[0])
    assert len(set(identities)) == 5, "Each player must have a distinct selection color"
    OUTPUT.mkdir(parents=True, exist_ok=True)
    preview.save(OUTPUT / "player-identities.png")
