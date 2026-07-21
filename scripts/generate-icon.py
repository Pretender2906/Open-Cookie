from PIL import Image, ImageDraw

size = 512
img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
draw.ellipse([0, 0, size - 1, size - 1], fill=(255, 140, 0, 255))

cx = size // 2
cy = size // 2
points = [
    (cx, cy - 124),
    (cx + 30, cy - 34),
    (cx + 126, cy - 24),
    (cx + 54, cy + 40),
    (cx + 76, cy + 136),
    (cx, cy + 84),
    (cx - 76, cy + 136),
    (cx - 54, cy + 40),
    (cx - 126, cy - 24),
    (cx - 30, cy - 34),
]
draw.polygon(points, fill=(255, 255, 255, 255))
img.save(r"D:\Open-Cookie\android\website\icon.png")
print("icon.png created")
