"""
Erzeugt die 3D-Modelle, Texturen und Item-Definitionen für das Resource Pack.

Ausführen:  python tools/generate_assets.py
Die erzeugten .json-Modelle lassen sich direkt in Blockbench öffnen und weiterbearbeiten.

Koordinaten-Hinweis für Hüte: Im Kopf-Slot wird das Modell so skaliert, dass der Spielerkopf
etwa von 1.6 bis 14.4 reicht. Die Oberkante des Kopfes liegt also bei y = 14.4.
"""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "pack"
NS = "nexus"
HEAD_TOP = 14.4


# ---------------------------------------------------------------- PNG-Helfer
def write_png(path: Path, pixels):
    """pixels: Liste von Zeilen, jede Zeile eine Liste von (r, g, b, a)."""
    height, width = len(pixels), len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in pixels)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def canvas(color, width=16, height=16):
    return [[color for _ in range(width)] for _ in range(height)]


def fill(img, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img[y][x] = color


# ------------------------------------------------------------- Modell-Helfer
def cube(name, frm, to, uv):
    """Ein Quader, dessen sechs Seiten alle denselben Texturbereich (uv) nutzen."""
    return {
        "name": name,
        "from": [round(v, 3) for v in frm],
        "to": [round(v, 3) for v in to],
        "faces": {face: {"uv": uv, "texture": "#0"}
                  for face in ("north", "east", "south", "west", "up", "down")},
    }


def display(center_y, gui_scale):
    """Positioniert das Modell in Menü, auf dem Boden und im Rahmen passend."""
    offset = center_y - 8
    return {
        "head": {},
        "gui": {"rotation": [30, 225, 0],
                "translation": [0, round(-offset * 0.866 * gui_scale, 2), 0],
                "scale": [gui_scale] * 3},
        "ground": {"translation": [0, round(2 - offset * 0.5, 2), 0], "scale": [0.5] * 3},
        "fixed": {"translation": [0, round(-offset * 0.6, 2), 0], "scale": [0.6] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0],
                                  "translation": [0, round(2.5 - offset * 0.375, 2), 0],
                                  "scale": [0.375] * 3},
    }


def write_cosmetic(cosmetic_id, elements, texture, center_y=8, gui_scale=1.0, display_settings=None, animation=None):
    tex = f"{NS}:item/{cosmetic_id}"
    model = {
        "credit": "NexusCosmetics",
        "textures": {"0": tex, "particle": tex},
        "elements": elements,
        "display": display_settings or display(center_y, gui_scale),
    }
    base = ROOT / "assets" / NS
    (base / "models" / "item").mkdir(parents=True, exist_ok=True)
    (base / "models" / "item" / f"{cosmetic_id}.json").write_text(json.dumps(model, indent=2))

    # Item-Definition: verbindet das Item (item_model = nexus:<id>) mit dem Modell
    (base / "items").mkdir(parents=True, exist_ok=True)
    (base / "items" / f"{cosmetic_id}.json").write_text(json.dumps(
        {"model": {"type": "minecraft:model", "model": f"{NS}:item/{cosmetic_id}"}}, indent=2))

    write_png(base / "textures" / "item" / f"{cosmetic_id}.png", texture)
    if animation:
        # Animierte Textur: Die Einzelbilder liegen untereinander im PNG
        (base / "textures" / "item" / f"{cosmetic_id}.png.mcmeta").write_text(
            json.dumps({"animation": animation}, indent=2))


# ---------------------------------------------------------------- Zylinder
def top_hat():
    black, light, dark = (30, 30, 36, 255), (52, 52, 62, 255), (16, 16, 20, 255)
    red, red_light = (150, 24, 30, 255), (190, 45, 50, 255)
    brim = (38, 38, 46, 255)

    img = canvas(black)
    # Krempe (rechter Bereich)
    fill(img, 10, 0, 16, 16, brim)
    # Hutseiten: links heller Glanzstreifen, rechts Schatten
    fill(img, 1, 0, 3, 6, light)
    fill(img, 8, 0, 10, 6, dark)
    # Rotes Hutband
    fill(img, 0, 6, 10, 7, red_light)
    fill(img, 0, 7, 10, 8, red)
    # Hutdeckel
    fill(img, 0, 9, 10, 16, (36, 36, 44, 255))
    fill(img, 1, 10, 4, 12, light)

    t = HEAD_TOP
    elements = [
        cube("Krempe", [0.5, t, 0.5], [15.5, t + 1, 15.5], [10, 0, 16, 1]),
        cube("Hut", [3, t + 1, 3], [13, t + 9.6, 13], [0, 0, 10, 8.5]),
    ]
    # Deckel mit eigener Textur
    elements[1]["faces"]["up"]["uv"] = [0, 9, 10, 16]
    elements[0]["faces"]["up"]["uv"] = [10, 1, 16, 16]
    elements[0]["faces"]["down"]["uv"] = [10, 1, 16, 16]
    write_cosmetic("top_hat", elements, img, center_y=t + 5, gui_scale=0.6)


# ------------------------------------------------------------------- Krone
def crown():
    gold, gold_light, gold_dark = (232, 182, 42, 255), (255, 224, 96, 255), (176, 124, 18, 255)
    ruby, ruby_light = (190, 22, 40, 255), (240, 80, 90, 255)
    sapphire, sapphire_light = (30, 80, 200, 255), (90, 150, 250, 255)

    img = canvas(gold)
    fill(img, 0, 0, 12, 2, gold_light)       # obere Kante glänzt
    fill(img, 0, 6, 12, 8, gold_dark)        # untere Kante im Schatten
    fill(img, 0, 8, 12, 16, gold)            # Zacken
    fill(img, 0, 8, 12, 9, gold_light)
    fill(img, 12, 0, 16, 4, ruby)            # Rubin
    fill(img, 12, 0, 14, 2, ruby_light)
    fill(img, 12, 4, 16, 8, sapphire)        # Saphir
    fill(img, 12, 4, 14, 6, sapphire_light)

    t = HEAD_TOP
    wall = [0, 0, 12, 8]
    point = [0, 8, 12, 16]
    ruby_uv, sapphire_uv = [12, 0, 16, 4], [12, 4, 16, 8]
    # Der Ring umschließt den Kopf (Kopf: 1.6 bis 14.4) und sitzt etwas tiefer als die Kopfoberkante
    a, b, w = 1.0, 15.0, 1.2      # Außenkanten und Wandstärke
    y0 = t - 1.5                  # Unterkante des Rings
    h = t + 2.5                   # Oberkante des Rings
    mid = 8

    elements = [
        cube("Ring vorne", [a, y0, a], [b, h, a + w], wall),
        cube("Ring hinten", [a, y0, b - w], [b, h, b], wall),
        cube("Ring links", [a, y0, a + w], [a + w, h, b - w], wall),
        cube("Ring rechts", [b - w, y0, a + w], [b, h, b - w], wall),
    ]
    # Zacken an den Ecken
    c = 2.2
    for x, z in ((a, a), (b - c, a), (a, b - c), (b - c, b - c)):
        elements.append(cube("Zacke", [x, h, z], [x + c, h + 3, z + c], point))
    # Hohe Spitzen in der Mitte jeder Seite
    s = 1.2
    elements += [
        cube("Spitze vorne", [mid - s, h, a], [mid + s, h + 4.5, a + w], point),
        cube("Spitze hinten", [mid - s, h, b - w], [mid + s, h + 4.5, b], point),
        cube("Spitze links", [a, h, mid - s], [a + w, h + 4.5, mid + s], point),
        cube("Spitze rechts", [b - w, h, mid - s], [b, h + 4.5, mid + s], point),
    ]
    # Edelsteine, die leicht aus dem Ring herausstehen
    g, gy0, gy1, p = 1.5, y0 + 1, y0 + 3.2, 0.6
    elements += [
        cube("Rubin vorne", [mid - g, gy0, a - p], [mid + g, gy1, a], ruby_uv),
        cube("Rubin hinten", [mid - g, gy0, b], [mid + g, gy1, b + p], ruby_uv),
        cube("Saphir links", [a - p, gy0, mid - g], [a, gy1, mid + g], sapphire_uv),
        cube("Saphir rechts", [b, gy0, mid - g], [b + p, gy1, mid + g], sapphire_uv),
    ]
    write_cosmetic("crown", elements, img, center_y=t + 2.5, gui_scale=0.65)


# ------------------------------------------------------------------- Capes
# Textur-Aufteilung (32x32 Pixel pro Bild):
#   x 0-19  Außenseite (sieht man von hinten)
#   x 20-29 Innenfutter
#   x 30-31 Kanten
CAPE_W, CAPE_H = 32, 32
CAPE_DISPLAY = {
    "gui": {"rotation": [10, 20, 0], "scale": [0.85] * 3},
    "ground": {"translation": [0, 2, 0], "scale": [0.5] * 3},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55] * 3},
}


def cape_element():
    edge = [15, 0, 16, 16]
    return [{
        "name": "Cape",
        "from": [3, 0, 7.5],
        "to": [13, 16, 8.5],
        "faces": {
            "south": {"uv": [0, 0, 10, 16], "texture": "#0"},   # Außenseite
            "north": {"uv": [10, 0, 15, 16], "texture": "#0"},  # Innenfutter
            "east": {"uv": edge, "texture": "#0"},
            "west": {"uv": edge, "texture": "#0"},
            "up": {"uv": [15, 0, 16, 1], "texture": "#0"},
            "down": {"uv": [15, 0, 16, 1], "texture": "#0"},
        },
    }]


# Die Cape wird im Spiel aus Segmenten zusammengesetzt, die einzeln schwingen.
# Muss zu SEGMENT_HEIGHTS in FakeCape.java passen!
CAPE_SEGMENTS = [5, 5, 6]


def write_cape_segments(cape_id):
    base = ROOT / "assets" / NS
    tex = f"{NS}:item/{cape_id}"
    v0 = 0
    for index, height in enumerate(CAPE_SEGMENTS):
        v1 = v0 + height
        edge = [15, v0, 16, v1]
        element = {
            "name": f"Segment {index}",
            "from": [3, 16 - height, 7.5],   # Oberkante liegt immer bei y = 16 (Drehpunkt)
            "to": [13, 16, 8.5],
            "faces": {
                "south": {"uv": [0, v0, 10, v1], "texture": "#0"},
                "north": {"uv": [10, v0, 15, v1], "texture": "#0"},
                "east": {"uv": edge, "texture": "#0"},
                "west": {"uv": edge, "texture": "#0"},
                "up": {"uv": [15, 0, 16, 1], "texture": "#0"},
                "down": {"uv": [15, 0, 16, 1], "texture": "#0"},
            },
        }
        model_id = f"{cape_id}_{index}"
        model = {"textures": {"0": tex, "particle": tex}, "elements": [element]}
        (base / "models" / "item" / f"{model_id}.json").write_text(json.dumps(model, indent=2))
        (base / "items" / f"{model_id}.json").write_text(json.dumps(
            {"model": {"type": "minecraft:model", "model": f"{NS}:item/{model_id}"}}, indent=2))
        v0 = v1


def royal_cape():
    red, red_dark, red_light = (150, 22, 34, 255), (118, 15, 25, 255), (178, 36, 46, 255)
    gold, gold_dark = (228, 178, 46, 255), (176, 128, 24, 255)
    ermine, spot = (242, 240, 232, 255), (25, 25, 28, 255)

    img = canvas(red, CAPE_W, CAPE_H)
    # Stofffalten
    for x in range(20):
        shade = red_dark if x % 5 == 0 else red_light if x % 5 == 2 else red
        fill(img, x, 0, x + 1, 32, shade)
    # Goldrand links, rechts und unten
    fill(img, 0, 0, 1, 32, gold)
    fill(img, 19, 0, 20, 32, gold)
    fill(img, 0, 30, 20, 32, gold)
    fill(img, 1, 29, 19, 30, gold_dark)
    # Hermelinkragen mit schwarzen Tupfen
    fill(img, 0, 0, 20, 5, ermine)
    for x, y in ((2, 1), (7, 1), (12, 1), (17, 1), (4, 3), (9, 3), (14, 3)):
        img[y][x] = spot
    fill(img, 0, 5, 20, 6, gold)
    # Kronen-Emblem in der Mitte
    for x in (7, 9, 10, 12):
        img[13][x] = gold
    fill(img, 7, 14, 13, 17, gold)
    fill(img, 7, 16, 13, 17, gold_dark)
    img[15][9] = img[15][10] = (200, 20, 40, 255)
    # Innenfutter und Kanten
    fill(img, 20, 0, 30, 32, (92, 12, 20, 255))
    fill(img, 20, 0, 30, 5, ermine)
    fill(img, 30, 0, 32, 32, gold)

    write_cosmetic("royal_cape", cape_element(), img, display_settings=CAPE_DISPLAY)
    write_cape_segments("royal_cape")


def galaxy_cape():
    import math
    import random
    rng = random.Random(42)
    frames = 24

    def mix(a, b, t):
        t = max(0.0, min(1.0, t))
        return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

    def background(x, y):
        color = mix((16, 8, 42), (44, 12, 82), y / 31)
        for cx, cy, r, tint in ((5, 9, 6, (120, 45, 170)), (14, 21, 7, (40, 90, 190)), (9, 28, 4, (170, 60, 150))):
            d = math.hypot(x - cx, y - cy)
            if d < r:
                color = mix(color, tint, (1 - d / r) * 0.75)
        return color

    stars = [(rng.randint(1, 18), rng.randint(6, 29), rng.random() * math.tau, rng.random() < 0.25)
             for _ in range(24)]
    static_stars = [(rng.randint(21, 28), rng.randint(2, 30)) for _ in range(6)]

    sheet = []
    for f in range(frames):
        img = [[background(x, y) if x < 20 else (0, 0, 0, 255) for x in range(CAPE_W)] for y in range(CAPE_H)]
        # Funkelnde Sterne
        for sx, sy, phase, big in stars:
            b = 0.5 + 0.5 * math.sin(math.tau * f / frames * 2 + phase)
            img[sy][sx] = mix(img[sy][sx], (255, 255, 255), 0.35 + 0.65 * b)
            if big and b > 0.7:
                for nx, ny in ((sx - 1, sy), (sx + 1, sy), (sx, sy - 1), (sx, sy + 1)):
                    if 1 <= nx <= 18:
                        img[ny][nx] = mix(img[ny][nx], (200, 210, 255), (b - 0.7) * 2.5)
        # Sternschnuppe (in den ersten 10 Bildern)
        if f < 10:
            hx, hy = 3 + f * 1.6, 7 + f * 1.4
            for i in range(4):
                tx, ty = int(hx - i * 1.2), int(hy - i * 1.05)
                if 1 <= tx <= 18 and 6 <= ty <= 30:
                    img[ty][tx] = mix(img[ty][tx], (255, 250, 220), 1 - i * 0.28)
        # Leuchtender, pulsierender Rand
        glow = mix((110, 80, 230), (190, 170, 255), 0.5 + 0.5 * math.sin(math.tau * f / frames))
        fill(img, 0, 0, 1, 32, glow)
        fill(img, 19, 0, 20, 32, glow)
        fill(img, 0, 31, 20, 32, glow)
        fill(img, 0, 0, 20, 1, glow)
        # Innenfutter und Kanten
        fill(img, 20, 0, 30, 32, (24, 12, 54, 255))
        for sx, sy in static_stars:
            img[sy][sx] = (200, 200, 255, 255)
        fill(img, 30, 0, 32, 32, glow)
        sheet.extend(img)

    write_cosmetic("galaxy_cape", cape_element(), sheet, display_settings=CAPE_DISPLAY,
                   animation={"frametime": 3, "interpolate": True})
    write_cape_segments("galaxy_cape")


def pack_meta():
    meta = {"pack": {"description": "NexusCosmetics – 3D-Cosmetics", "min_format": 97, "max_format": 100}}
    ROOT.mkdir(parents=True, exist_ok=True)
    (ROOT / "pack.mcmeta").write_text(json.dumps(meta, indent=2, ensure_ascii=False), encoding="utf-8")


if __name__ == "__main__":
    pack_meta()
    top_hat()
    crown()
    royal_cape()
    galaxy_cape()
    print("Assets erzeugt in", ROOT)
