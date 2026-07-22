"""
Generate Open Cookie launcher icons and website icon from intact_cookie.png.
"""
from __future__ import annotations

import math
import os
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
SRC_COOKIE = ROOT / "intact_cookie.png"
APP_RES = ROOT.parent / "app" / "src" / "main" / "res"
WEBSITE_ICON = ROOT.parent / "website" / "icon.png"

ADAPTIVE_FOREGROUND = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}
LAUNCHER_ICON = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Theme tokens from Theme.kt
ESPRESSO_TOP = (0x1E, 0x15, 0x12)
ESPRESSO_MID = (0x13, 0x0D, 0x0B)
ESPRESSO_DEEP = (0x08, 0x06, 0x05)
COOKIE_GOLD = (0xE4, 0xB8, 0x76)
COOKIE_SCALE = 0.72


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def lerp_rgb(c1: tuple[int, int, int], c2: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return (
        int(lerp(c1[0], c2[0], t)),
        int(lerp(c1[1], c2[1], t)),
        int(lerp(c1[2], c2[2], t)),
    )


def radial_background(size: int) -> Image.Image:
    img = Image.new("RGB", (size, size))
    px = img.load()
    center = (size - 1) / 2.0
    max_r = size * 0.72
    for y in range(size):
        for x in range(size):
            d = min(1.0, math.hypot(x - center, y - center) / max_r)
            if d < 0.42:
                t = d / 0.42
                color = lerp_rgb(COOKIE_GOLD, ESPRESSO_TOP, t * 0.55)
            elif d < 0.78:
                t = (d - 0.42) / 0.36
                color = lerp_rgb(ESPRESSO_TOP, ESPRESSO_MID, t)
            else:
                t = (d - 0.78) / 0.22
                color = lerp_rgb(ESPRESSO_MID, ESPRESSO_DEEP, t)
            px[x, y] = color
    return img


def crop_to_content(im: Image.Image, pad: int = 12) -> Image.Image:
    bbox = im.split()[3].getbbox()
    if bbox is None:
        return im
    left, top, right, bottom = bbox
    left = max(0, left - pad)
    top = max(0, top - pad)
    right = min(im.width, right + pad)
    bottom = min(im.height, bottom + pad)
    return im.crop((left, top, right, bottom))


def load_cookie() -> Image.Image:
    return crop_to_content(Image.open(SRC_COOKIE).convert("RGBA"))


def place_cookie(canvas_size: int, cookie: Image.Image) -> Image.Image:
    target = int(canvas_size * COOKIE_SCALE)
    scale = min(target / cookie.width, target / cookie.height)
    resized = cookie.resize(
        (max(1, int(cookie.width * scale)), max(1, int(cookie.height * scale))),
        Image.Resampling.LANCZOS,
    )
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    x = (canvas_size - resized.width) // 2
    y = (canvas_size - resized.height) // 2
    canvas.alpha_composite(resized, (x, y))
    return canvas


def compose_icon(size: int, cookie: Image.Image) -> Image.Image:
    background = radial_background(size)
    foreground = place_cookie(size, cookie)
    icon = background.convert("RGBA")
    icon.alpha_composite(foreground)
    return icon


def save_png(path: Path, image: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if image.mode == "RGBA" and path.name.endswith(".png"):
        image.save(path, optimize=True)
    else:
        image.convert("RGB").save(path, optimize=True)


def main() -> None:
    cookie = load_cookie()

    for density, size in ADAPTIVE_FOREGROUND.items():
        save_png(
            APP_RES / f"drawable-{density}" / "ic_launcher_foreground.png",
            place_cookie(size, cookie),
        )

    for density, size in LAUNCHER_ICON.items():
        icon = compose_icon(size, cookie)
        save_png(APP_RES / f"mipmap-{density}" / "ic_launcher.png", icon)
        save_png(APP_RES / f"mipmap-{density}" / "ic_launcher_round.png", icon)

    save_png(WEBSITE_ICON, compose_icon(512, cookie))
    print("Launcher icons and website icon generated.")


if __name__ == "__main__":
    main()
