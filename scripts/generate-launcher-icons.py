from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"

# Smaller value = smaller letters inside the icon safe zone.
CONTENT_SCALE = 0.56

BACKGROUND_COLOR = (0xF2, 0x74, 0x1F, 255)

SRC_CANDIDATES = [
    ROOT.parent / "CybLight" / "images" / "favicon_192.png",
    RES / "mipmap-xxxhdpi" / "ic_launcher.png",
    RES / "drawable-xxxhdpi" / "ic_launcher_foreground.png",
]

foreground_sizes = {
    "drawable-mdpi": 108,
    "drawable-hdpi": 162,
    "drawable-xhdpi": 216,
    "drawable-xxhdpi": 324,
    "drawable-xxxhdpi": 432,
}

mipmap_sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# In-app logo (CybLightLogo composable, up to ~112dp).
logo_sizes = {
    "drawable-mdpi": 96,
    "drawable-hdpi": 144,
    "drawable-xhdpi": 192,
    "drawable-xxhdpi": 288,
    "drawable-xxxhdpi": 384,
}


def resolve_source() -> Path:
    for candidate in SRC_CANDIDATES:
        if candidate.exists():
            return candidate
    raise FileNotFoundError(f"No launcher source image found. Tried: {SRC_CANDIDATES}")


def scaled_content(src: Image.Image, canvas_size: int) -> Image.Image:
    content_size = max(1, int(canvas_size * CONTENT_SCALE))
    scaled = src.resize((content_size, content_size), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    offset = (canvas_size - content_size) // 2
    canvas.paste(scaled, (offset, offset), scaled)
    return canvas


def save_foreground(folder: str, size: int, src: Image.Image) -> None:
    out_dir = RES / folder
    out_dir.mkdir(parents=True, exist_ok=True)
    img = scaled_content(src, size)
    img.save(out_dir / "ic_launcher_foreground.png", format="PNG", optimize=True)
    print(f"  {folder}/ic_launcher_foreground.png -> {size}x{size} (content {CONTENT_SCALE:.0%})")


def save_mipmap(folder: str, name: str, size: int, src: Image.Image) -> None:
    out_dir = RES / folder
    out_dir.mkdir(parents=True, exist_ok=True)
    canvas = Image.new("RGBA", (size, size), BACKGROUND_COLOR)
    foreground = scaled_content(src, size)
    canvas.alpha_composite(foreground)
    canvas.save(out_dir / name, format="PNG", optimize=True)
    print(f"  {folder}/{name} -> {size}x{size} (content {CONTENT_SCALE:.0%})")


def main() -> None:
    src_path = resolve_source()
    src = Image.open(src_path).convert("RGBA")
    print(f"Source: {src_path}")
    print(f"Content scale: {CONTENT_SCALE:.0%}")
    print("Foreground layers:")
    for folder, size in foreground_sizes.items():
        save_foreground(folder, size, src)

    print("Mipmap launchers:")
    for folder, size in mipmap_sizes.items():
        save_mipmap(folder, "ic_launcher.png", size, src)
        save_mipmap(folder, "ic_launcher_round.png", size, src)

    print("In-app logo:")
    for folder, size in logo_sizes.items():
        save_mipmap(folder, "ic_cyblight_logo.png", size, src)


if __name__ == "__main__":
    main()
