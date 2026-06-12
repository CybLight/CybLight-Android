from pathlib import Path
from PIL import Image

SRC = Path(__file__).resolve().parents[1] / ".." / "CybLight" / "images" / "favicon_192.png"
RES = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"

src = Image.open(SRC).convert("RGBA")

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


def save_scaled(folder: str, name: str, size: int) -> None:
    out_dir = RES / folder
    out_dir.mkdir(parents=True, exist_ok=True)
    img = src.resize((size, size), Image.Resampling.LANCZOS)
    img.save(out_dir / name, format="PNG", optimize=True)
    print(f"  {folder}/{name} -> {size}x{size}")


def main() -> None:
    print(f"Source: {SRC}")
    print("Foreground layers:")
    for folder, size in foreground_sizes.items():
        save_scaled(folder, "ic_launcher_foreground.png", size)

    print("Mipmap launchers:")
    for folder, size in mipmap_sizes.items():
        save_scaled(folder, "ic_launcher.png", size)
        save_scaled(folder, "ic_launcher_round.png", size)


if __name__ == "__main__":
    main()
