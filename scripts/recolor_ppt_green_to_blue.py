from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import colorsys
import re

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
UNPACKED = ROOT / "output" / "recolor-blue" / "unpacked"
OUT = ROOT / "output" / "实训答辩2-蓝色系.pptx"


XML_COLOR_MAP = {
    "315A22": "0B3D91",  # dark green -> deep blue
    "4F8A34": "2F80ED",  # medium green -> bright blue
    "E7F1DF": "E8F3FF",  # light green -> light blue
    "DDEED5": "DCEEFF",  # pale green -> pale blue
    "B8C8B0": "B9D7F2",  # green gray line -> blue gray line
    "457200": "0B3D91",
    "548235": "1E6BCB",
    "70AD47": "2F80ED",
    "A9D18E": "B9D7F2",
    "E2F0D9": "E8F3FF",
    "9BBB59": "5CA8FF",
}

PIXEL_TARGETS = [
    ((0x31, 0x5A, 0x22), (0x0B, 0x3D, 0x91), 42),
    ((0x4F, 0x8A, 0x34), (0x2F, 0x80, 0xED), 48),
    ((0xE7, 0xF1, 0xDF), (0xE8, 0xF3, 0xFF), 34),
    ((0xDD, 0xEE, 0xD5), (0xDC, 0xEE, 0xFF), 42),
    ((0xB8, 0xC8, 0xB0), (0xB9, 0xD7, 0xF2), 40),
    ((0x58, 0xB3, 0x68), (0x4A, 0xA3, 0xFF), 50),
]


def dist2(a, b):
    return sum((a[i] - b[i]) ** 2 for i in range(3))


def recolor_rgb(r, g, b):
    src = (r, g, b)
    for target, repl, tol in PIXEL_TARGETS:
        if dist2(src, target) <= tol * tol:
            return repl

    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    deg = h * 360
    # Recolor green theme elements, including darker olive borders and pale green fills.
    if 65 <= deg <= 155 and s >= 0.12 and v >= 0.20:
        nh = 212 / 360
        ns = max(0.28, min(0.72, s * 0.92 + 0.10))
        nv = min(1.0, max(v, 0.45))
        nr, ng, nb = colorsys.hsv_to_rgb(nh, ns, nv)
        return int(nr * 255), int(ng * 255), int(nb * 255)

    if 75 <= deg <= 145 and s >= 0.05 and v >= 0.70:
        # Very pale green backgrounds become a cool pale blue.
        return 232, 243, 255

    return src


def recolor_images():
    media_dir = UNPACKED / "ppt" / "media"
    for path in media_dir.glob("*.png"):
        im = Image.open(path).convert("RGBA")
        pixels = []
        changed = 0
        for r, g, b, a in im.getdata():
            nr, ng, nb = recolor_rgb(r, g, b)
            if (nr, ng, nb) != (r, g, b):
                changed += 1
            pixels.append((nr, ng, nb, a))
        if changed:
            im.putdata(pixels)
            im.save(path)


def recolor_xml():
    pattern = re.compile(r'(<a:srgbClr val=")([0-9A-Fa-f]{6})(")')
    for path in (UNPACKED / "ppt").rglob("*.xml"):
        text = path.read_text(encoding="utf-8", errors="ignore")

        def repl(match):
            color = match.group(2).upper()
            return match.group(1) + XML_COLOR_MAP.get(color, color) + match.group(3)

        new_text = pattern.sub(repl, text)
        if new_text != text:
            path.write_text(new_text, encoding="utf-8")


def pack():
    if OUT.exists():
        OUT.unlink()
    with ZipFile(OUT, "w", ZIP_DEFLATED) as zf:
        for path in sorted(UNPACKED.rglob("*")):
            if path.is_file():
                zf.write(path, path.relative_to(UNPACKED).as_posix())
    print(OUT)


def main():
    recolor_xml()
    recolor_images()
    pack()


if __name__ == "__main__":
    main()
