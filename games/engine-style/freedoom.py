"""Build the Freedoom menu, font, instruction, and status resources."""

from pathlib import Path
import json
import shutil
import xml.etree.ElementTree as ET
import zipfile
from PIL import Image, ImageDraw, ImageFont


def create(decoded: Path, font_path: Path, build: Path, root: Path) -> Path:
    source = build / "java/net/nullsum/freedoom"
    source.mkdir(parents=True)
    shutil.copyfile(
        root / "games/freedoom-portal/src/net/nullsum/freedoom/PortalGameActivity.java",
        source / "PortalGameActivity.java",
    )
    style = (
        (root / "android/app/src/main/java/com/mprlab/portal/PortalStyle.java")
        .read_text()
        .replace("package com.mprlab.portal;", "package net.nullsum.freedoom;")
    )
    (source / "PortalStyle.java").write_text(style)
    public = ET.parse(decoded / "res/values/public.xml")
    font_id = (max(int(n.attrib["id"], 16) >> 16 for n in public.getroot()) + 1) << 16
    tag_id = (
        max(int(n.attrib["id"], 16) for n in public.getroot() if n.get("type") == "id")
        + 1
    )
    ET.SubElement(
        public.getroot(),
        "public",
        {"type": "font", "name": "fredoka", "id": hex(font_id)},
    )
    ET.SubElement(
        public.getroot(),
        "public",
        {"type": "id", "name": "portal_text_role", "id": hex(tag_id)},
    )
    public.write(
        decoded / "res/values/public.xml", encoding="utf-8", xml_declaration=True
    )
    (decoded / "res/font").mkdir()
    shutil.copyfile(font_path, decoded / "res/font/fredoka.ttf")
    (decoded / "res/values/portal_style.xml").write_text(
        '<resources><item type="id" name="portal_text_role"/></resources>'
    )
    (source / "R.java").write_text(
        f"package net.nullsum.freedoom; final class R {{ static class font {{ static final int fredoka={font_id}; }} static class id {{ static final int portal_text_role={tag_id}; }} }}"
    )
    shutil.copyfile(
        root / "games/freedoom-portal/AndroidManifest.xml",
        decoded / "AndroidManifest.xml",
    )
    for scale in (1.0, 1.3):
        pack = build / f"ui-{scale}"
        (pack / "graphics").mkdir(parents=True)
        fontdefs = []
        textures = []
        for prefix, names, size in [
            ("S", ("SmallFont", "SmallFont2"), 7),
            ("B", ("BigFont",), 12),
            ("H", ("HUDFONT_DOOM",), 12),
            ("I", ("INDEXFONT_DOOM",), 6),
            ("W", ("IntermissionFont_Doom",), 12),
        ]:
            font = ImageFont.truetype(str(font_path), round(size * 4 * scale))
            ascent, descent = font.getmetrics()
            rows = []
            for code in range(32, 127):
                char = chr(code)
                width = max(4, round(font.getlength(char)) + 2)
                height = ascent + descent + 2
                glyph = Image.new("RGBA", (width, height))
                draw = ImageDraw.Draw(glyph)
                draw.text((1, ascent + 1), char, font=font, fill="black", anchor="ls")
                stem = f"{prefix}{code:03}"
                glyph.save(pack / f"graphics/{stem}.png")
                textures.append(
                    f'Graphic {stem}, {width}, {height}\n{{ XScale 4 YScale 4 Patch "graphics/{stem}.png", 0, 0 }}'
                )
                key = chr(92) + "glyph" if code == 92 else json.dumps(char + "glyph")
                rows.append(f" {key} {stem}")
            rows.append(" NOTRANSLATION " + " ".join(str(i) for i in range(256)))
            for name in names:
                fontdefs.append(name + "\n{\n" + "\n".join(rows) + "\n}")
        (pack / "FONTDEFS").write_text("\n".join(fontdefs))
        (pack / "TEXTURES").write_text("\n".join(textures))
        paper = "#fffbef"
        yellow = "#ffd65c"
        mint = "#77ddb5"
        title_font = ImageFont.truetype(str(font_path), round(34 * scale))
        control_font = ImageFont.truetype(str(font_path), round(26 * scale))
        for name, label, color in [
            ("M_DOOM", "Freedoom", paper),
            ("M_NGAME", "New game", yellow),
            ("M_OPTION", "Options", mint),
            ("M_LOADG", "Load game", yellow),
            ("M_SAVEG", "Save game", yellow),
            ("M_RDTHIS", "How to play", mint),
            ("M_QUITG", "Exit game", "#ff8597"),
        ]:
            image = Image.new("RGBA", (640, 80))
            draw = ImageDraw.Draw(image)
            draw.rounded_rectangle((4, 4, 639, 79), radius=16, fill="black")
            draw.rounded_rectangle(
                (1, 1, 634, 74), radius=14, fill=color, outline="black", width=3
            )
            draw.text(
                (320, 36),
                label,
                font=title_font if name == "M_DOOM" else control_font,
                anchor="mm",
                fill="black",
            )
            image.save(pack / f"graphics/{name}.png")
            textures.append(
                f'Graphic {name}, 640, 80\n{{ XScale 4 YScale 4 Patch "graphics/{name}.png", 0, 0 }}'
            )
        for name in ["TITLEPIC", "INTERPIC", "BOSSBACK"]:
            Image.new("RGB", (320, 200), paper).save(pack / f"graphics/{name}.png")
        status = Image.new("RGB", (320, 32), paper)
        draw = ImageDraw.Draw(status)
        for left, right, color in [
            (0, 48, yellow),
            (49, 110, mint),
            (111, 168, "#8dd4ff"),
            (169, 236, "#c4aaff"),
            (237, 319, yellow),
        ]:
            draw.rounded_rectangle(
                (left, 0, right, 31), radius=3, fill=color, outline="black", width=1
            )
        status.save(pack / "graphics/STBAR.png")
        help_page = Image.new("RGB", (1280, 800), paper)
        draw = ImageDraw.Draw(help_page)
        draw.text(
            (640, 100),
            "How to play",
            font=ImageFont.truetype(str(font_path), round(48 * scale)),
            anchor="mm",
            fill="black",
        )
        for y, text in [
            (250, "Left side: move"),
            (340, "Right side: look around"),
            (430, "Tap Fire to shoot"),
            (520, "Tap Use to open doors"),
            (610, "Back opens the game menu"),
        ]:
            draw.text(
                (640, y),
                text,
                font=ImageFont.truetype(str(font_path), round(32 * scale)),
                anchor="mm",
                fill="black",
            )
        help_page.save(pack / "graphics/HELP.png")
        textures.append(
            'Graphic HELP, 1280, 800 { XScale 4 YScale 4 Patch "graphics/HELP.png", 0, 0 }'
        )
        (pack / "TEXTURES").write_text("\n".join(textures))
        (pack / "MENUDEF").write_text("""OptionMenuSettings { Linespacing 16 }
ListMenu "MainMenu"
{
 StaticPatch 80, 8, "M_DOOM"
 Position 80, 38
 Linespacing 24
 Selector "M_SKULL1", -32, 0
 PatchItem "M_NGAME", "n", "PlayerclassMenu"
 PatchItem "M_LOADG", "l", "LoadGameMenu"
 PatchItem "M_SAVEG", "s", "SaveGameMenu"
 PatchItem "M_OPTION", "o", "OptionsMenu"
 PatchItem "M_RDTHIS", "r", "ReadThisMenu"
 PatchItem "M_QUITG", "q", "QuitMenu"
}
""")
        shutil.copyfile(
            root / "android/app/src/main/res/raw/fredoka_license.txt",
            pack / "fredoka_license.txt",
        )
        with zipfile.ZipFile(
            decoded / f"assets/portal-ui-{scale}.pk3",
            "w",
            compression=zipfile.ZIP_DEFLATED,
        ) as archive:
            for path in sorted(pack.rglob("*")):
                if path.is_file():
                    item = zipfile.ZipInfo(
                        str(path.relative_to(pack)), date_time=(2026, 1, 1, 0, 0, 0)
                    )
                    item.compress_type = zipfile.ZIP_DEFLATED
                    archive.writestr(item, path.read_bytes())
    return build / "java"
