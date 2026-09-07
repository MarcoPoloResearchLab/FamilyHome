"""Compile source-owned engine interfaces against pinned upstream Android engines."""

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import tempfile
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
CACHE = ROOT / "android/build/engine-style/cache"
SOURCES = json.loads((Path(__file__).parent / "sources.json").read_text())
COLORS = {
    "paper": "#fffbef",
    "ink": "#000000",
    "yellow": "#ffd65c",
    "mint": "#77ddb5",
    "blue": "#8dd4ff",
    "purple": "#c4aaff",
    "coral": "#ff8597",
    "disabled": "#deded8",
}


def run(*args: object) -> None:
    subprocess.run([str(arg) for arg in args], check=True)


def fetch(name: str, destination: Path) -> Path:
    source = SOURCES[name]
    if not destination.is_file():
        urllib.request.urlretrieve(source["url"], destination)
    actual = hashlib.sha256(destination.read_bytes()).hexdigest()
    if actual != source["sha256"]:
        raise ValueError(f"{name} SHA-256 mismatch: {actual}")
    return destination


def surface(
    path: Path,
    color: str,
    *,
    checked: bool = False,
    arrow: str = "",
    size: tuple[int, int] = (96, 64),
) -> None:
    image = Image.new("RGBA", size)
    draw = ImageDraw.Draw(image)
    width, height = size
    draw.rounded_rectangle((4, 4, width - 1, height - 1), radius=8, fill=COLORS["ink"])
    draw.rounded_rectangle(
        (1, 1, width - 6, height - 6),
        radius=6,
        fill=color,
        outline=COLORS["ink"],
        width=3,
    )
    if checked:
        draw.line(
            [
                (width * 0.25, height * 0.48),
                (width * 0.43, height * 0.66),
                (width * 0.72, height * 0.28),
            ],
            fill=COLORS["ink"],
            width=5,
        )
    if arrow:
        points = {
            "left": [(0.62, 0.25), (0.32, 0.5), (0.62, 0.75)],
            "right": [(0.32, 0.25), (0.62, 0.5), (0.32, 0.75)],
            "up": [(0.25, 0.62), (0.5, 0.32), (0.75, 0.62)],
            "down": [(0.25, 0.32), (0.5, 0.62), (0.75, 0.32)],
        }[arrow]
        draw.line(
            [(int(width * x), int(height * y)) for x, y in points],
            fill=COLORS["ink"],
            width=5,
        )
    image.save(path)


def xml(path: Path) -> ET.ElementTree:
    # STK permits bare ampersands in attributes; normalize this dialect at the file boundary.
    text = re.sub(r"&(?!#?[a-zA-Z0-9]+;)", "&amp;", path.read_text())
    return ET.ElementTree(ET.fromstring(text))


def kart(decoded: Path, font: Path) -> None:
    data = decoded / "assets/data"
    skin = data / "skins/classic"
    tree = xml(skin / "stkskin.xml")
    tree.getroot().attrib = {
        "base_theme_name": "FamilyHome",
        "variant_name": "Bright",
        "author": "Marco Polo Research Lab",
    }
    for index, element in enumerate(tree.getroot().findall("element")):
        kind = element.attrib["type"]
        state = element.get("state", "neutral")
        color = (
            "paper"
            if any(
                part in kind.lower()
                for part in [
                    "background",
                    "dialog",
                    "window",
                    "section",
                    "bubble",
                    "bar",
                    "tooltip",
                ]
            )
            else "yellow"
        )
        if any(part in state for part in ["focused", "down"]) or (
            "checked" in state and "unchecked" not in state
        ):
            color = "mint"
        if "deactivated" in state:
            color = "disabled"
        if "error" in kind:
            color = "coral"
        path = skin / f"portal-{index}.png"
        if kind == "background":
            Image.new("RGB", (1280, 800), COLORS["paper"]).save(path)
        else:
            arrow = next(
                (
                    direction
                    for direction in ("left", "right", "up", "down")
                    if direction in kind.lower() and "arrow" in kind.lower()
                ),
                "",
            )
            size = (
                (1280, 96)
                if kind == "bottom-bar"
                else ((192, 192) if "halo" in kind.lower() else (96, 64))
            )
            surface(
                path,
                COLORS[color],
                checked="checked" in state and "unchecked" not in state,
                arrow=arrow,
                size=size,
            )
            if "halo" in kind.lower():
                image = Image.new("RGBA", size)
                draw = ImageDraw.Draw(image)
                draw.rounded_rectangle(
                    (2, 2, size[0] - 6, size[1] - 6),
                    radius=16,
                    outline=COLORS["ink"],
                    width=3,
                )
                image.save(path)
        element.attrib.pop("common", None)
        element.set("image", path.name)
        for edge in ["left_border", "right_border", "top_border", "bottom_border"]:
            if edge in element.attrib:
                element.set(edge, "8")
        if kind in {"spinner", "spinner_rainbow"}:
            element.set("left_border", "24")
            element.set("right_border", "24")
            image = Image.open(path)
            draw = ImageDraw.Draw(image)
            draw.line([(17, 23), (9, 31), (17, 39)], fill=COLORS["ink"], width=3)
            draw.line([(77, 23), (85, 31), (77, 39)], fill=COLORS["ink"], width=3)
            image.save(path)
        for key in ["hborder_out_portion", "vborder_out_portion"]:
            if key in element.attrib:
                element.set(key, "0")
        if "preserve_h_aspect_ratios" in element.attrib:
            element.set("preserve_h_aspect_ratios", "false")
    for color in tree.getroot().findall("color"):
        for channel in ["r", "g", "b"]:
            color.set(channel, "0")
        color.set("a", "255")
        if color.get("type") == "font":
            for channel, value in zip(("r", "g", "b"), (255, 214, 92)):
                color.set(channel, str(value))
        if "background" in color.get("state", ""):
            for channel, value in zip(("r", "g", "b"), (255, 251, 239)):
                color.set(channel, str(value))
    tree.write(skin / "stkskin.xml", encoding="utf-8", xml_declaration=True)
    # One selectable appearance; common contains engine-owned icons shared by the skin.
    for sibling in (data / "skins").iterdir():
        if sibling.is_dir() and sibling.name not in {"classic", "common"}:
            shutil.rmtree(sibling)
    fonts = data / "ttf"
    shutil.copyfile(font, fonts / "Fredoka-Bold.ttf")
    shutil.copyfile(
        ROOT / "android/app/src/main/res/raw/fredoka_license.txt",
        fonts / "fredoka_license.txt",
    )
    config = xml(data / "stk_config.xml")
    fonts_node = config.getroot().find("fonts-list")
    fonts_node.set(
        "normal-ttf",
        "Fredoka-Bold.ttf "
        + fonts_node.get("normal-ttf").replace("Cantarell-Regular.otf ", ""),
    )
    fonts_node.set("digit-ttf", "Fredoka-Bold.ttf")
    config.write(data / "stk_config.xml", encoding="utf-8", xml_declaration=True)
    # Large buttons also retain their size in dialogs that use fixed layout dimensions.
    for path in (data / "gui").rglob("*.stkgui"):
        text = path.read_text()

        def height(match: re.Match[str]) -> str:
            tag = re.sub(r'height="(?:fit|1(?:\.\d+)?f)"', 'height="60"', match[0])
            return re.sub(
                r'height="(\d+)"',
                lambda h: 'height="' + str(max(60, int(h[1]))) + '"',
                tag,
            )

        text = re.sub(
            r"<(?:button|icon-button|checkbox|spinner|textbox)\b[^>]*>", height, text
        )
        path.write_text(text)
    # The native results renderer owns white and bright score colors.
    # Give that table a dark panel while surrounding actions keep the cream skin.
    scoreboard = Image.new("RGBA", (1280, 620), COLORS["ink"])
    draw = ImageDraw.Draw(scoreboard)
    draw.rounded_rectangle(
        (2, 2, 1277, 617), radius=14, outline=COLORS["yellow"], width=4
    )
    scoreboard.save(data / "gui/icons/portal-scoreboard.png")
    results = data / "gui/screens/race_result.stkgui"
    results_text = results.read_text()
    marker = '<div id="result-table" width="100%" height="80%">'
    if results_text.count(marker) != 1:
        raise ValueError("Kart results table changed")
    results.write_text(
        results_text.replace(
            marker,
            marker.replace('height="80%"', 'height="76%"')
            + '\n<icon x="0" y="0" width="100%" height="100%" icon="gui/icons/portal-scoreboard.png"/>',
        )
    )
    entries = [data, *sorted(data.rglob("*"))]
    (decoded / "assets/files.txt").write_text(
        "".join(
            str(path.relative_to(decoded / "assets"))
            + ("/" if path.is_dir() else "")
            + "\n"
            for path in entries
        )
    )
    digest = hashlib.sha256()
    for path in entries:
        if path.is_file():
            digest.update(str(path.relative_to(data)).encode())
            digest.update(path.read_bytes())
    (decoded / "assets/portal-interface-id").write_text(digest.hexdigest())
    entry = decoded / "smali/org/supertuxkart/stk/SuperTuxKartActivity.smali"
    original = "    invoke-super {p0, p1}, Lorg/libsdl/app/SDLActivity;->onCreate(Landroid/os/Bundle;)V"
    content = entry.read_text()
    if content.count(original) != 1:
        raise ValueError("Kart entry point changed")
    entry.write_text(
        content.replace(
            original,
            "    invoke-static {p0}, Lorg/supertuxkart/stk/PortalAppearance;->install(Landroid/app/Activity;)V\n\n"
            + original,
        )
    )


def compile_java(source: Path, output: Path, sdk: Path) -> Path:
    classes = output / "classes"
    classes.mkdir()
    tools = sdk / "build-tools/36.1.0"
    android = sdk / "platforms/android-36/android.jar"
    run(
        "javac",
        "-source",
        "8",
        "-target",
        "8",
        "-classpath",
        android,
        "-d",
        classes,
        *source.rglob("*.java"),
    )
    jar = output / "interface.jar"
    run("jar", "cf", jar, "-C", classes, ".")
    dex = output / "dex"
    dex.mkdir()
    run(tools / "d8", "--min-api", "28", "--lib", android, "--output", dex, jar)
    return dex / "classes.dex"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("game", choices=("kart",))
    args = parser.parse_args()
    game = args.game
    CACHE.mkdir(parents=True, exist_ok=True)
    apk = fetch(game, CACHE / f"{game}-upstream.apk")
    apktool = fetch("apktool", CACHE / "apktool.jar")
    output = ROOT / f"android/build/{game}-portal"
    output.mkdir(parents=True, exist_ok=True)
    build = Path(tempfile.mkdtemp(prefix="interface.", dir=output))
    decoded = build / "decoded"
    run("java", "-jar", apktool, "d", apk, "-o", decoded)
    font = instantiateVariableFont(
        TTFont(
            ROOT / "android/app/src/main/res/font/fredoka.ttf", recalcTimestamp=False
        ),
        {"wght": 700},
        inplace=True,
    )
    font_path = build / "Fredoka-Bold.ttf"
    font.save(font_path)
    source = ROOT / f"games/{game}-portal/src"
    kart(decoded, font_path)
    config = decoded / "apktool.yml"
    text = config.read_text()
    spec = SOURCES[game]
    text = re.sub(r"  versionCode: .*", f"  versionCode: {spec['version_code']}", text)
    text = re.sub(r"  versionName: .*", f"  versionName: {spec['version_name']}", text)
    config.write_text(text)
    sdk = Path(os.environ["ANDROID_SDK_ROOT"])
    dex = compile_java(source, build, sdk)
    unsigned = build / "interface.apk"
    run("java", "-jar", apktool, "b", decoded, "-o", unsigned)
    with zipfile.ZipFile(unsigned, "a") as archive:
        count = 1
        while f"classes{count if count > 1 else ''}.dex" in archive.namelist():
            count += 1
        archive.write(dex, f"classes{count}.dex")
    # The adaptation must preserve every native engine library exactly.
    with zipfile.ZipFile(apk) as upstream, zipfile.ZipFile(unsigned) as adapted:
        for name in upstream.namelist():
            if (
                name.startswith("lib/")
                and name.endswith(".so")
                and upstream.read(name) != adapted.read(name)
            ):
                raise ValueError(f"Native engine changed: {name}")
    title = game.capitalize()
    aligned = output / f"{title}-Portal-unsigned.apk"
    run(sdk / "build-tools/36.1.0/zipalign", "-f", "-p", "4", unsigned, aligned)
    if "PORTAL_KEYSTORE" in os.environ:
        signed = output / f"{title}-Portal.apk"
        run(
            sdk / "build-tools/36.1.0/apksigner",
            "sign",
            "--ks",
            os.environ["PORTAL_KEYSTORE"],
            "--ks-pass",
            "env:PORTAL_KEYSTORE_PASSWORD",
            "--key-pass",
            "env:PORTAL_KEY_PASSWORD",
            "--out",
            signed,
            aligned,
        )
        run(sdk / "build-tools/36.1.0/apksigner", "verify", signed)
    source_files = [
        ROOT / "Makefile",
        ROOT / "android/STYLING.md",
        ROOT / "android/app/src/main/res/font/fredoka.ttf",
        ROOT / "android/app/src/main/res/raw/fredoka_license.txt",
        ROOT / "android/app/src/main/java/com/mprlab/portal/PortalStyle.java",
    ]
    for directory in (ROOT / "games/engine-style", ROOT / f"games/{game}-portal"):
        source_files.extend(
            path
            for path in directory.rglob("*")
            if path.is_file() and "__pycache__" not in path.parts
        )
    with zipfile.ZipFile(
        output / f"{title}-Portal-interface-source.zip",
        "w",
        compression=zipfile.ZIP_DEFLATED,
    ) as archive:
        for path in sorted(source_files):
            item = zipfile.ZipInfo(
                str(path.relative_to(ROOT)), date_time=(2026, 1, 1, 0, 0, 0)
            )
            item.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(item, path.read_bytes())
    record = {
        "game": game,
        "upstream": SOURCES[game],
        "apktool": SOURCES["apktool"],
        "unsigned_sha256": hashlib.sha256(aligned.read_bytes()).hexdigest(),
        "native_libraries_unchanged": True,
        "source_sha256": {
            str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in sorted(source_files)
        },
    }
    (output / "interface-build.json").write_text(json.dumps(record, indent=2) + "\n")
    print(f"{title} interface APK: {aligned}")


if __name__ == "__main__":
    main()
