"""
Generate left/right broken-cookie halves and cropped hero assets for Open Cookie.

The halves are derived from the real intact_cookie.png so they keep the same lighting,
texture, scale and perspective. The torn edge is intentionally solid and warm: no wide
alpha feather, no white/gray erase effect, only a believable baked crumb interior.
"""
import os
import math
import random
import numpy as np
from PIL import Image

SRC_DIR = os.path.dirname(__file__)
OUT_DIR = os.path.join(SRC_DIR, "..", "app", "src", "main", "res", "drawable-nodpi")
os.makedirs(OUT_DIR, exist_ok=True)

random.seed(73)


def crop_to_content(im, pad=8):
    bbox = im.split()[3].getbbox()
    l, t, r, b = bbox
    l = max(0, l - pad); t = max(0, t - pad)
    r = min(im.width, r + pad); b = min(im.height, b + pad)
    return im.crop((l, t, r, b))


def build_tear(h, w):
    """A jagged, slightly leaning tear line with larger solid irregularities."""
    center = 0.5 * w
    lean = 0.035 * w
    n = 18
    ctrl = [
        center + lean * (i / (n - 1) - 0.5) + random.uniform(-0.045, 0.045) * w
        for i in range(n)
    ]
    ys = np.linspace(0, h - 1, n)
    boundary = np.interp(np.arange(h), ys, ctrl)

    coarse = np.array([random.uniform(-0.018, 0.018) * w for _ in range(h)])
    coarse = np.convolve(coarse, np.ones(13) / 13, mode="same")

    fine = np.array([random.uniform(-0.009, 0.009) * w for _ in range(h)])
    fine = np.convolve(fine, np.ones(5) / 5, mode="same")

    scallop = (
        np.sin(np.linspace(0.0, math.tau * 6.5, h) + 0.7) * 0.0065 * w
        + np.sin(np.linspace(0.0, math.tau * 13.0, h) + 1.2) * 0.003 * w
    )
    return boundary + coarse + fine + scallop


def make_half(arr, boundary, side, edge_depth):
    """Cut one half and shade the torn edge as a solid, warm crumb interior."""
    h, w, _ = arr.shape
    xg = np.broadcast_to(np.arange(w), (h, w)).astype(float)
    bx = boundary[:, None]
    out = arr.copy()
    if side == "left":
        keep = xg < bx
        dist = bx - xg  # >0 inside piece, small near edge
    else:
        keep = xg >= bx
        dist = xg - bx

    alpha = out[..., 3] * keep
    out[..., 3] = alpha

    rng = np.random.default_rng(73 if side == "left" else 91)
    depth_profile = np.array(
        [random.uniform(0.82, 1.16) for _ in range(h)],
        dtype=float,
    )
    depth_profile = np.interp(
        np.arange(h),
        np.linspace(0, h - 1, len(depth_profile)),
        depth_profile,
    )[:, None]
    depth_profile = np.convolve(depth_profile[:, 0], np.ones(25) / 25, mode="same")[:, None]
    depth_profile = edge_depth * depth_profile

    crumb = np.clip(1.0 - dist / depth_profile, 0.0, 1.0) * keep
    crumb = crumb ** 1.2
    root = np.clip(1.0 - dist / (depth_profile * 0.28), 0.0, 1.0) * keep
    highlight = np.clip(1.0 - dist / (depth_profile * 0.62), 0.0, 1.0) * keep

    light = np.array([244.0, 205.0, 132.0])
    warm = np.array([226.0, 168.0, 83.0])
    root_shadow = np.array([173.0, 103.0, 41.0])
    speckle = rng.normal(0.0, 1.0, (h, w))
    speckle = np.clip(speckle, -1.6, 1.6)

    for c in range(3):
        base = out[..., c]
        base = base * (1.0 - crumb * 0.55) + warm[c] * crumb * 0.55
        base = base * (1.0 - highlight * 0.16) + light[c] * highlight * 0.16
        base = base * (1.0 - root * 0.22) + root_shadow[c] * root * 0.22
        base = base + crumb * speckle * 10.0
        out[..., c] = np.clip(base, 0, 255)

    return out


def main():
    intact = Image.open(os.path.join(SRC_DIR, "intact_cookie.png")).convert("RGBA")
    intact = crop_to_content(intact)
    w, h = intact.size
    arr = np.asarray(intact, dtype=float)
    arr[..., 3] = np.where(arr[..., 3] < 6, 0, arr[..., 3])

    boundary = build_tear(h, w)
    edge_depth = 0.10 * w

    left = make_half(arr, boundary, "left", edge_depth)
    right = make_half(arr, boundary, "right", edge_depth)

    Image.fromarray(left.round().astype(np.uint8), "RGBA").save(
        os.path.join(OUT_DIR, "cookie_left_half.png"))
    Image.fromarray(right.round().astype(np.uint8), "RGBA").save(
        os.path.join(OUT_DIR, "cookie_right_half.png"))
    intact.save(os.path.join(OUT_DIR, "intact_cookie.png"))

    paper = Image.open(os.path.join(SRC_DIR, "fortune_paper.png")).convert("RGBA")
    paper = crop_to_content(paper)
    paper.save(os.path.join(OUT_DIR, "fortune_paper.png"))

    print("intact", intact.size)
    print("wrote halves + intact + paper to", os.path.abspath(OUT_DIR))


if __name__ == "__main__":
    main()
