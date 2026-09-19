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


def write_cape_segments(cape_id, segments=None, x0=3, x1=13, top_extras=()):
    """Zerlegt eine Cape in Segmente. top_extras: zusätzliche Teile am obersten Segment (z. B. Kapuze)."""
    segments = segments or CAPE_SEGMENTS
    total = sum(segments)
    base = ROOT / "assets" / NS
    tex = f"{NS}:item/{cape_id}"
    top = 0
    for index, height in enumerate(segments):
        v0 = round(top / total * 16, 3)
        v1 = round((top + height) / total * 16, 3)
        top += height
        edge = [15, v0, 16, v1]
        element = {
            "name": f"Segment {index}",
            "from": [x0, 16 - height, 7.5],   # Oberkante liegt immer bei y = 16 (Drehpunkt)
            "to": [x1, 16, 8.5],
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
        elements = [element] + (list(top_extras) if index == 0 else [])
        model = {"textures": {"0": tex, "particle": tex}, "elements": elements}
        (base / "models" / "item" / f"{model_id}.json").write_text(json.dumps(model, indent=2))
        (base / "items" / f"{model_id}.json").write_text(json.dumps(
            {"model": {"type": "minecraft:model", "model": f"{NS}:item/{model_id}"}}, indent=2))


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


# ---------------------------------------------------------------- Haustiere
# Wichtig: Item-Displays drehen Modelle um 180°. Deshalb zeigt bei Haustieren die
# NORD-Seite des Modells (kleines z) im Spiel nach vorne. Der Kopf liegt also im Norden.
PET_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "scale": [0.7] * 3},
    "ground": {"translation": [0, 2, 0], "scale": [0.5] * 3},
    "fixed": {"rotation": [0, 180, 0], "scale": [0.8] * 3},
    "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.45] * 3},
}


def faces(element, **uvs):
    """Einzelne Seiten eines Quaders mit anderem Texturbereich versehen."""
    for face, uv in uvs.items():
        element["faces"][face]["uv"] = uv
    return element


def shift(elements, dx, dy, dz):
    moved = []
    for element in elements:
        copy = json.loads(json.dumps(element))
        copy["from"] = [round(a + b, 3) for a, b in zip(copy["from"], (dx, dy, dz))]
        copy["to"] = [round(a + b, 3) for a, b in zip(copy["to"], (dx, dy, dz))]
        moved.append(copy)
    return moved


def write_extra_model(model_id, texture_id, elements, display_settings=None):
    """Zusatzmodell (z. B. Flügel), das die Textur eines anderen Cosmetics mitbenutzt."""
    base = ROOT / "assets" / NS
    tex = f"{NS}:item/{texture_id}"
    model = {"textures": {"0": tex, "particle": tex}, "elements": elements}
    if display_settings:
        model["display"] = display_settings
    (base / "models" / "item" / f"{model_id}.json").write_text(json.dumps(model, indent=2))
    (base / "items" / f"{model_id}.json").write_text(json.dumps(
        {"model": {"type": "minecraft:model", "model": f"{NS}:item/{model_id}"}}, indent=2))


def mini_dragon():
    red, red_dark, red_light = (196, 48, 38, 255), (140, 28, 24, 255), (226, 84, 60, 255)
    belly, belly_dark = (244, 204, 96, 255), (214, 168, 64, 255)
    horn, horn_dark = (238, 228, 204, 255), (196, 182, 150, 255)
    membrane, vein = (242, 128, 62, 255), (204, 86, 40, 255)
    bone = (122, 28, 26, 255)
    eye, pupil, white = (255, 214, 60, 255), (20, 16, 16, 255), (255, 255, 255, 255)

    img = canvas(red, 32, 32)
    # Schuppen (uv 0,0 - 8,8)
    for y in range(16):
        for x in range(16):
            if (x + (y // 2) * 2) % 4 == 0 and y % 2 == 0:
                img[y][x] = red_dark
            elif (x + y) % 7 == 0:
                img[y][x] = red_light
    # Bauch (uv 8,0 - 16,8)
    fill(img, 16, 0, 32, 16, belly)
    for y in range(1, 16, 3):
        fill(img, 16, y, 32, y + 1, belly_dark)
    # Gesicht vorne (uv 0,8 - 8,12): Augen oben links/rechts
    fill(img, 0, 16, 16, 24, red)
    fill(img, 1, 17, 5, 20, eye)
    fill(img, 11, 17, 15, 20, eye)
    fill(img, 2, 17, 4, 20, pupil)
    fill(img, 12, 17, 14, 20, pupil)
    img[17][1] = img[17][11] = white
    # Schnauze vorne (uv 0,12 - 8,16): Nasenlöcher
    fill(img, 0, 24, 16, 32, red_light)
    fill(img, 4, 26, 6, 28, red_dark)
    fill(img, 10, 26, 12, 28, red_dark)
    fill(img, 3, 30, 13, 31, red_dark)
    # Hörner (uv 8,8 - 12,12)
    fill(img, 16, 16, 24, 24, horn)
    fill(img, 16, 22, 24, 24, horn_dark)
    # Flughaut (uv 12,8 - 16,12)
    fill(img, 24, 16, 32, 24, membrane)
    for i in range(8):
        img[16 + i][24 + i] = vein
    # Flügelknochen (uv 8,12 - 12,16)
    fill(img, 16, 24, 24, 32, bone)

    scales, belly_uv, face_uv, snout_uv = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 12], [0, 12, 8, 16]
    horn_uv, membrane_uv, bone_uv = [8, 8, 12, 12], [12, 8, 16, 12], [8, 12, 12, 16]

    body = [
        faces(cube("Körper", [5, 5, 6], [11, 10, 12], scales), down=belly_uv),
        cube("Bauch", [5.5, 4.5, 6.5], [10.5, 5, 11.5], belly_uv),
        faces(cube("Kopf", [5.5, 8, 1.5], [10.5, 12, 6], scales), north=face_uv),
        faces(cube("Schnauze", [6.5, 8, 0], [9.5, 10, 1.5], scales), north=snout_uv),
        cube("Horn links", [6, 12, 4], [7, 14, 5], horn_uv),
        cube("Horn rechts", [9, 12, 4], [10, 14, 5], horn_uv),
        cube("Stachel 1", [7.5, 10, 7], [8.5, 11, 8], horn_uv),
        cube("Stachel 2", [7.5, 10, 9.5], [8.5, 11, 10.5], horn_uv),
        cube("Schwanz", [7, 6, 12], [9, 8, 15], scales),
        cube("Schwanzspitze", [7.5, 6.5, 15], [8.5, 7.5, 17], scales),
        cube("Stachel Schwanz", [7.75, 7.5, 15.5], [8.25, 8.5, 16.5], horn_uv),
        cube("Bein vorne links", [5.5, 3, 7], [7, 5, 8.5], scales),
        cube("Bein vorne rechts", [9, 3, 7], [10.5, 5, 8.5], scales),
        cube("Bein hinten links", [5.5, 3, 10], [7, 5, 11.5], scales),
        cube("Bein hinten rechts", [9, 3, 10], [10.5, 5, 11.5], scales),
    ]
    # Flügel: Gelenk liegt in der Modellmitte (8, 8, 8), damit sie sich darum drehen
    wing_a = [
        cube("Flügelknochen", [8, 8, 7.5], [16, 9, 8.5], bone_uv),
        cube("Flughaut", [8, 8.25, 8.5], [15, 8.75, 13], membrane_uv),
    ]
    wing_b = [
        cube("Flügelknochen", [0, 8, 7.5], [8, 9, 8.5], bone_uv),
        cube("Flughaut", [1, 8.25, 8.5], [8, 8.75, 13], membrane_uv),
    ]

    # Menü-Symbol: Körper mit angelegten Flügeln an den Schultern
    icon = body + shift(wing_a, 3, 2, 0.5) + shift(wing_b, -3, 2, 0.5)
    write_cosmetic("mini_dragon", icon, img, display_settings=PET_DISPLAY)
    write_extra_model("mini_dragon_body", "mini_dragon", body)
    write_extra_model("mini_dragon_wing_a", "mini_dragon", wing_a)
    write_extra_model("mini_dragon_wing_b", "mini_dragon", wing_b)


def ghost():
    body, shade, skirt = (244, 247, 255, 255), (222, 230, 252, 255), (210, 222, 250, 255)
    eye, blush, mouth = (30, 30, 46, 255), (255, 170, 190, 255), (70, 50, 70, 255)

    frames = []
    for blink in (False, True):
        img = canvas(body, 32, 32)
        # Gesicht (uv 0,0 - 8,8)
        if blink:
            fill(img, 4, 8, 6, 9, eye)
            fill(img, 10, 8, 12, 9, eye)
        else:
            fill(img, 4, 6, 6, 9, eye)
            fill(img, 10, 6, 12, 9, eye)
            img[6][4] = img[6][10] = (120, 120, 150, 255)
        fill(img, 2, 10, 4, 11, blush)
        fill(img, 12, 10, 14, 11, blush)
        fill(img, 7, 11, 9, 13, mouth)
        # Körper ohne Gesicht (uv 8,0 - 16,8), oben heller
        fill(img, 16, 0, 32, 16, shade)
        fill(img, 16, 0, 32, 4, body)
        # Unterer Saum (uv 0,8 - 8,16)
        fill(img, 0, 16, 16, 32, skirt)
        frames.extend(img)

    face_uv, plain, skirt_uv = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16]
    elements = [
        faces(cube("Körper", [4, 6, 4], [12, 14, 12], plain), north=face_uv),
        cube("Saum", [4, 4, 4], [12, 6, 12], skirt_uv),
        cube("Arm links", [2.5, 8, 7], [4, 10, 9], plain),
        cube("Arm rechts", [12, 8, 7], [13.5, 10, 9], plain),
    ]
    # Gezackter Rand unten
    for x in (4, 7, 10):
        for z in (4, 7, 10):
            if (x, z) != (7, 7):
                elements.append(cube("Zacke", [x, 3, z], [x + 2, 4, z + 2], skirt_uv))

    # Blinzeln: 3 Sekunden offen, kurz zu
    animation = {"frametime": 1, "frames": [{"index": 0, "time": 60}, {"index": 1, "time": 4}]}
    write_cosmetic("ghost", elements, frames, display_settings=PET_DISPLAY, animation=animation)



# ---------------------------------------------------------- Süße Haustiere
# Gemeinsame Zutaten für den Süß-Faktor: große glänzende Augen und rosa Bäckchen.
BLUSH = (255, 150, 170, 255)
SPARKLE = (255, 255, 255, 255)


def cute_eyes(img, x0, y0, left_x, right_x, eye_w=3, eye_h=4, color=(28, 26, 40, 255)):
    """Zwei große Augen mit Glanzpunkt oben links."""
    for ex in (left_x, right_x):
        fill(img, x0 + ex, y0, x0 + ex + eye_w, y0 + eye_h, color)
        img[y0][x0 + ex] = SPARKLE
        img[y0 + 1][x0 + ex] = SPARKLE if eye_h > 3 else color


def penguin():
    black, black_light = (38, 42, 58, 255), (58, 64, 86, 255)
    white, white_shade = (248, 248, 244, 255), (226, 230, 236, 255)
    orange, orange_dark = (255, 166, 46, 255), (224, 128, 30, 255)

    img = canvas(black, 32, 32)
    # Rücken/Seiten (uv 0,0 - 8,8) mit leichtem Glanz
    fill(img, 2, 0, 5, 16, black_light)
    # Bauch vorne (uv 8,0 - 16,8): weißer Bauch mit schwarzem Rand
    fill(img, 16, 0, 32, 16, black)
    fill(img, 18, 0, 30, 16, white)
    fill(img, 18, 12, 30, 16, white_shade)
    # Kopf vorne (uv 0,8 - 8,12): weißes Gesicht, Augen, Bäckchen
    fill(img, 0, 16, 16, 24, black)
    fill(img, 1, 18, 15, 24, white)
    fill(img, 3, 17, 7, 18, white)
    fill(img, 9, 17, 13, 18, white)
    cute_eyes(img, 0, 18, 3, 10, eye_w=3, eye_h=4)
    fill(img, 1, 22, 4, 23, BLUSH)
    fill(img, 12, 22, 15, 23, BLUSH)
    # Orange: Schnabel und Füße (uv 8,8 - 12,12)
    fill(img, 16, 16, 24, 24, orange)
    fill(img, 16, 22, 24, 24, orange_dark)
    # Flossen (uv 12,8 - 16,12)
    fill(img, 24, 16, 32, 24, black_light)
    fill(img, 24, 22, 32, 24, black)

    back, belly, face, beak, flipper = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 12], [8, 8, 12, 12], [12, 8, 16, 12]
    body = [
        faces(cube("Körper", [4.5, 2, 5.5], [11.5, 8, 11.5], back), north=belly),
        faces(cube("Kopf", [4.5, 8, 5.5], [11.5, 13.5, 11.5], back), north=face),
        cube("Haarbüschel", [7.5, 13.5, 7.5], [8.5, 14.5, 9], back),
        cube("Schnabel", [7.25, 9, 4.5], [8.75, 10, 5.5], beak),
        cube("Fuß links", [5, 1, 4.5], [7.25, 2, 7.5], beak),
        cube("Fuß rechts", [8.75, 1, 4.5], [11, 2, 7.5], beak),
        cube("Schwänzchen", [7, 2.5, 11.5], [9, 3.5, 12.5], back),
    ]
    flipper_a = [cube("Flosse", [8, 3.5, 7], [9, 8, 10], flipper)]
    flipper_b = [cube("Flosse", [7, 3.5, 7], [8, 8, 10], flipper)]

    write_cosmetic("penguin", body + shift(flipper_a, 3.5, 0, 0.5) + shift(flipper_b, -3.5, 0, 0.5), img,
                   display_settings=PET_DISPLAY)
    write_extra_model("penguin_body", "penguin", body)
    write_extra_model("penguin_flipper_a", "penguin", flipper_a)
    write_extra_model("penguin_flipper_b", "penguin", flipper_b)


def kitten():
    fur, stripe, fur_light = (250, 160, 70, 255), (214, 116, 40, 255), (255, 190, 110, 255)
    cream, pink, nose = (255, 244, 228, 255), (255, 170, 185, 255), (240, 110, 130, 255)
    eye = (50, 150, 90, 255)

    img = canvas(fur, 32, 32)
    # Fell mit Tigerstreifen (uv 0,0 - 8,8)
    for x in range(1, 16, 5):
        fill(img, x, 0, x + 1, 16, (228, 134, 52, 255))
    fill(img, 0, 0, 16, 1, fur_light)
    # Gesicht wach (uv 8,0 - 16,8)
    fill(img, 16, 0, 32, 16, fur)
    fill(img, 17, 0, 19, 3, stripe)
    fill(img, 23, 0, 25, 3, stripe)
    fill(img, 29, 0, 31, 3, stripe)
    fill(img, 20, 9, 28, 16, cream)                  # helle Schnauze
    for ex in (18, 26):                               # große grüne Augen
        fill(img, ex, 5, ex + 4, 10, eye)
        fill(img, ex + 1, 6, ex + 3, 10, (20, 30, 25, 255))
        img[5][ex] = img[6][ex] = SPARKLE
    fill(img, 23, 10, 25, 11, nose)
    img[12][22] = img[12][25] = (120, 70, 60, 255)    # Mündchen
    img[13][23] = img[13][24] = (120, 70, 60, 255)
    fill(img, 16, 11, 19, 12, BLUSH)
    fill(img, 29, 11, 32, 12, BLUSH)
    # Gesicht schlafend (uv 0,8 - 8,16): geschlossene Äuglein ^^
    fill(img, 0, 16, 16, 32, fur)
    fill(img, 4, 25, 12, 32, cream)
    for ex in (2, 10):
        img[22][ex] = img[21][ex + 1] = img[21][ex + 2] = img[22][ex + 3] = (60, 40, 40, 255)
    fill(img, 7, 26, 9, 27, nose)
    fill(img, 0, 24, 3, 25, BLUSH)
    fill(img, 13, 24, 16, 25, BLUSH)
    # Creme (uv 8,8 - 12,12): Pfoten, Brust
    fill(img, 16, 16, 24, 24, cream)
    # Ohren (uv 12,8 - 16,12): außen orange, innen rosa
    fill(img, 24, 16, 32, 24, fur)
    fill(img, 26, 18, 30, 24, pink)
    # Schwanz geringelt (uv 8,12 - 12,16)
    for y in range(24, 32):
        fill(img, 16, y, 24, y + 1, stripe if y % 3 == 0 else fur)
    fill(img, 16, 30, 24, 32, cream)

    fur_uv, face_uv, sleep_uv, cream_uv, ear_uv, tail_uv = (
        [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12], [12, 8, 16, 12], [8, 12, 12, 16])

    body = [
        faces(cube("Körper", [5.5, 4, 7], [10.5, 8, 11], fur_uv), north=cream_uv),
        faces(cube("Kopf", [4.5, 8, 5], [11.5, 13.5, 10.5], fur_uv), north=face_uv),
        faces(cube("Ohr links", [5, 13.5, 6.5], [7.25, 15.5, 7.5], ear_uv), south=fur_uv),
        faces(cube("Ohr rechts", [8.75, 13.5, 6.5], [11, 15.5, 7.5], ear_uv), south=fur_uv),
        cube("Pfote links", [6, 4, 6], [7.5, 5, 7], cream_uv),
        cube("Pfote rechts", [8.5, 4, 6], [10, 5, 7], cream_uv),
    ]
    # Schwanz: Gelenk in der Modellmitte, zeigt nach hinten (+z) und biegt sich nach oben
    tail = [
        cube("Schwanz", [7.25, 8, 8], [8.75, 9.5, 13], tail_uv),
        cube("Schwanzspitze", [7.25, 9.5, 11.5], [8.75, 13, 13], tail_uv),
    ]
    # Eingerollt schlafend: flacher Laib, Kopf auf den Pfoten, Schwanz vorne herum
    sleep = [
        cube("Körper", [4.5, 4, 6.5], [11.5, 8, 11.5], fur_uv),
        faces(cube("Kopf", [5, 4, 2.5], [11, 8.5, 7], fur_uv), north=sleep_uv),
        faces(cube("Ohr links", [5.5, 8.5, 4], [7.5, 10, 5], ear_uv), south=fur_uv),
        faces(cube("Ohr rechts", [8.5, 8.5, 4], [10.5, 10, 5], ear_uv), south=fur_uv),
        cube("Schwanz", [3.5, 4, 2], [5, 5.5, 11.5], tail_uv),
        cube("Schwanz vorne", [3.5, 4, 1], [9, 5.5, 2.5], tail_uv),
    ]

    write_cosmetic("kitten", body + shift(tail, 0, -3, 3), img, display_settings=PET_DISPLAY)
    write_extra_model("kitten_body", "kitten", body)
    write_extra_model("kitten_tail", "kitten", tail)
    write_extra_model("kitten_sleep", "kitten", sleep)


def bee():
    yellow, yellow_light, black = (255, 206, 52, 255), (255, 228, 110, 255), (46, 36, 34, 255)
    wing, wing_edge = (214, 238, 255, 255), (250, 252, 255, 255)

    img = canvas(yellow, 32, 32)
    # Seiten mit Streifen quer zur Körperlänge (uv 0,0 - 8,8)
    for x in (5, 6, 10, 11):
        fill(img, x, 0, x + 1, 16, black)
    fill(img, 0, 0, 16, 2, yellow_light)
    # Rücken/Bauch mit Streifen (uv 8,0 - 16,8)
    fill(img, 16, 0, 32, 16, yellow)
    for y in (5, 6, 10, 11):
        fill(img, 16, y, 32, y + 1, black)
    # Gesicht (uv 0,8 - 8,16)
    fill(img, 0, 16, 16, 32, yellow)
    cute_eyes(img, 0, 20, 2, 10, eye_w=4, eye_h=5)
    fill(img, 1, 26, 3, 27, BLUSH)
    fill(img, 13, 26, 15, 27, BLUSH)
    img[27][7] = img[27][8] = (120, 60, 30, 255)
    # Schwarz: Stachel, Fühler (uv 8,8 - 12,12)
    fill(img, 16, 16, 24, 24, black)
    # Flügel (uv 12,8 - 16,12)
    fill(img, 24, 16, 32, 24, wing)
    fill(img, 24, 16, 32, 17, wing_edge)
    fill(img, 24, 16, 25, 24, wing_edge)

    side, top, face, dark, wing_uv = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12], [12, 8, 16, 12]
    body = [
        faces(cube("Körper", [5, 5, 5], [11, 10, 12], side), north=face, up=top, down=top, south=top),
        cube("Stachel", [7.5, 6.5, 12], [8.5, 7.5, 13.5], dark),
        cube("Fühler links", [6, 10, 5], [6.5, 12, 5.5], dark),
        cube("Fühler rechts", [9.5, 10, 5], [10, 12, 5.5], dark),
        cube("Fühler Kugel links", [5.75, 12, 4.75], [6.75, 13, 5.75], dark),
        cube("Fühler Kugel rechts", [9.25, 12, 4.75], [10.25, 13, 5.75], dark),
    ]
    wing_a = [cube("Flügel", [8, 8, 7], [13, 8.4, 11], wing_uv)]
    wing_b = [cube("Flügel", [3, 8, 7], [8, 8.4, 11], wing_uv)]

    write_cosmetic("bee", body + shift(wing_a, 2, 2, 0.5) + shift(wing_b, -2, 2, 0.5), img,
                   display_settings=PET_DISPLAY)
    write_extra_model("bee_body", "bee", body)
    write_extra_model("bee_wing_a", "bee", wing_a)
    write_extra_model("bee_wing_b", "bee", wing_b)


def mushroom():
    red, red_dark, dot = (232, 58, 58, 255), (190, 36, 40, 255), (255, 250, 240, 255)
    stem, stem_shade, gills = (252, 240, 214, 255), (232, 214, 184, 255), (236, 206, 170, 255)

    img = canvas(red, 32, 32)
    # Hut mit weißen Punkten (uv 0,0 - 8,8)
    fill(img, 0, 12, 16, 16, red_dark)
    for dx, dy in ((2, 2), (9, 1), (12, 7), (5, 8), (1, 11), (10, 12)):
        fill(img, dx, dy, dx + 3, dy + 2, dot)
    # Stiel (uv 8,0 - 16,8)
    fill(img, 16, 0, 32, 16, stem)
    fill(img, 16, 12, 32, 16, stem_shade)
    # Gesicht (uv 0,8 - 8,16)
    fill(img, 0, 16, 16, 32, stem)
    cute_eyes(img, 0, 19, 3, 10, eye_w=3, eye_h=4)
    fill(img, 1, 24, 4, 25, BLUSH)
    fill(img, 12, 24, 15, 25, BLUSH)
    img[25][6] = img[25][9] = (110, 60, 50, 255)      # Lächeln
    fill(img, 7, 26, 9, 27, (110, 60, 50, 255))
    # Lamellen unter dem Hut (uv 8,8 - 12,12)
    fill(img, 16, 16, 24, 24, gills)
    for x in range(16, 24, 2):
        fill(img, x, 16, x + 1, 24, stem_shade)
    # Füßchen (uv 12,8 - 16,12)
    fill(img, 24, 16, 32, 24, stem_shade)

    cap, stem_uv, face, gill_uv, feet = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12], [12, 8, 16, 12]
    elements = [
        faces(cube("Stiel", [5.5, 2, 5.5], [10.5, 8, 10.5], stem_uv), north=face),
        faces(cube("Hut", [3, 8, 3], [13, 12, 13], cap), down=gill_uv),
        cube("Hut oben", [4.5, 12, 4.5], [11.5, 13.5, 11.5], cap),
        cube("Fuß links", [6, 1, 5], [7.5, 2, 8], feet),
        cube("Fuß rechts", [8.5, 1, 5], [10, 2, 8], feet),
    ]
    write_cosmetic("mushroom", elements, img, display_settings=PET_DISPLAY)



# --------------------------------------------------------------- Zauberei
ROBE_SEGMENTS = [7, 7, 8]  # muss zu ROBE_SEGMENTS in FakeCape.java passen


def wizard_robe(color_id, lining, lining_dark):
    black, fold_dark, fold_light = (30, 30, 37, 255), (22, 22, 28, 255), (42, 42, 52, 255)

    img = canvas(black, 32, 32)
    # Außenseite: schwarzer Stoff mit Falten, farbiger Saum an den Kanten
    for x in range(20):
        shade = fold_dark if x % 5 == 1 else fold_light if x % 5 == 3 else black
        fill(img, x, 0, x + 1, 32, shade)
    fill(img, 0, 0, 1, 32, lining)
    fill(img, 19, 0, 20, 32, lining)
    fill(img, 0, 31, 20, 32, lining)
    fill(img, 0, 0, 20, 2, fold_dark)                # Kragen
    # Innenfutter und Kanten in der Hausfarbe
    fill(img, 20, 0, 30, 32, lining)
    for y in range(0, 32, 4):
        fill(img, 20, y, 30, y + 1, lining_dark)
    fill(img, 30, 0, 32, 32, lining)

    # Kapuze, die auf dem Rücken liegt (Innenseite zeigt nach oben)
    hood = cube("Kapuze", [4.5, 11, 8.5], [11.5, 16, 10], [0, 0, 10, 5])
    hood["faces"]["up"]["uv"] = [10, 0, 15, 2]
    hood["faces"]["north"]["uv"] = [10, 0, 15, 5]
    hood_rim = cube("Kapuzenrand", [4.5, 15, 10], [11.5, 16, 10.5], [15, 0, 16, 2])

    robe_id = f"wizard_robe_{color_id}"
    # Menü-Symbol: der ganze Umhang in einem Stück
    icon = [{
        "name": "Umhang",
        "from": [2, -3, 7.5], "to": [14, 19, 8.5],
        "faces": {
            "south": {"uv": [0, 0, 10, 16], "texture": "#0"},
            "north": {"uv": [10, 0, 15, 16], "texture": "#0"},
            "east": {"uv": [15, 0, 16, 16], "texture": "#0"},
            "west": {"uv": [15, 0, 16, 16], "texture": "#0"},
            "up": {"uv": [15, 0, 16, 1], "texture": "#0"},
            "down": {"uv": [15, 0, 16, 1], "texture": "#0"},
        },
    }] + shift([hood, hood_rim], 0, 3, 0)
    robe_display = dict(CAPE_DISPLAY)
    robe_display["gui"] = {"rotation": [10, 20, 0], "scale": [0.62] * 3}
    write_cosmetic(robe_id, icon, img, display_settings=robe_display)
    write_cape_segments(robe_id, ROBE_SEGMENTS, x0=2, x1=14, top_extras=[hood, hood_rim])


def talking_hat():
    felt, felt_light, felt_dark = (126, 88, 58, 255), (150, 108, 72, 255), (98, 68, 45, 255)
    patch_a, patch_b, stitch = (156, 120, 78, 255), (104, 80, 60, 255), (62, 44, 30, 255)
    crease, mouth = (58, 38, 26, 255), (34, 20, 16, 255)

    def face(img, x0, open_mouth):
        # Die Gesichtsfläche ist breit und flach: Merkmale werden senkrecht gestaucht, deshalb hoch malen
        fill(img, x0, 0, x0 + 16, 16, felt)
        fill(img, x0, 0, x0 + 16, 1, felt_dark)
        for ex in (1, 10):
            # müde, schräge Augenfalten
            fill(img, x0 + ex, 3, x0 + ex + 5, 6, crease)
            fill(img, x0 + ex + 1, 2, x0 + ex + 4, 3, crease)
            fill(img, x0 + ex, 6, x0 + ex + 5, 7, felt_light)
        if open_mouth:
            fill(img, x0 + 3, 8, x0 + 13, 15, mouth)
            fill(img, x0 + 2, 8, x0 + 14, 9, crease)
            fill(img, x0 + 5, 12, x0 + 11, 15, (130, 44, 44, 255))
        else:
            fill(img, x0 + 3, 10, x0 + 13, 12, crease)
            fill(img, x0 + 2, 9, x0 + 4, 10, crease)
            fill(img, x0 + 12, 9, x0 + 14, 10, crease)

    img = canvas(felt, 32, 32)
    # Filz mit Flicken und Nähten (uv 0,0 - 8,8)
    fill(img, 0, 0, 16, 16, felt)
    fill(img, 2, 2, 7, 6, patch_a)
    fill(img, 9, 9, 14, 14, patch_b)
    fill(img, 10, 1, 13, 4, patch_b)
    for x in range(1, 8, 2):
        img[1][x] = img[6][x] = stitch
    for x in range(8, 15, 2):
        img[8][x] = img[14][x] = stitch
    for y in range(3, 16, 3):
        fill(img, 0, y, 16, y + 1, felt_dark) if y % 6 == 0 else None
    face(img, 16, open_mouth=False)             # Gesicht geschlossen (uv 8,0 - 16,8)
    # Gesicht offen liegt unten links (uv 0,8 - 8,16)
    tmp = canvas(felt, 32, 32)
    face(tmp, 0, open_mouth=True)
    for y in range(16):
        for x in range(16):
            img[16 + y][x] = tmp[y][x]
    # Krempe (uv 8,8 - 16,16)
    fill(img, 16, 16, 32, 32, felt_dark)
    fill(img, 16, 16, 32, 17, felt_light)
    fill(img, 20, 20, 24, 23, patch_a)

    felt_uv, face_closed, face_open, brim_uv = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 16, 16]
    t = HEAD_TOP

    def hat(face_uv):
        return [
            cube("Krempe", [0, t, 0], [16, t + 1, 16], brim_uv),
            faces(cube("Hut unten", [2.5, t + 1, 2.5], [13.5, t + 5, 13.5], felt_uv), north=face_uv),
            cube("Hut Mitte", [3.5, t + 5, 3.5], [12.5, t + 8, 12.5], felt_uv),
            cube("Hut oben", [4.5, t + 8, 4.5], [11.5, t + 10.5, 11], felt_uv),
            cube("Spitze", [5.5, t + 10.5, 5], [10, t + 12.5, 9.5], felt_uv),
            cube("Spitze geknickt", [3.5, t + 12, 5.5], [6.5, t + 13.5, 8.5], felt_uv),
            cube("Spitzenende", [2, t + 11, 6], [3.5, t + 12.5, 8], felt_uv),
        ]

    write_cosmetic("talking_hat", hat(face_closed), img, center_y=t + 6.5, gui_scale=0.5)
    write_extra_model("talking_hat_talk", "talking_hat", hat(face_open), display(t + 6.5, 0.5))


def halo():
    gold, light = (255, 222, 110, 255), (255, 246, 200, 255)
    img = canvas(gold)
    fill(img, 0, 0, 16, 4, light)

    uv = [0, 0, 16, 16]
    half = 2.3
    north = cube("Nord", [8 - half, 7.5, 2], [8 + half, 8.5, 3], uv)
    south = cube("Süd", [8 - half, 7.5, 13], [8 + half, 8.5, 14], uv)
    elements = [
        north, south,
        cube("Ost", [13, 7.5, 8 - half], [14, 8.5, 8 + half], uv),
        cube("West", [2, 7.5, 8 - half], [3, 8.5, 8 + half], uv),
    ]
    # Die Diagonalen entstehen durch um 45° gedrehte Kopien der Nord- und Süd-Kante
    for source in (north, south):
        for angle in (45, -45):
            diagonal = json.loads(json.dumps(source))
            diagonal["name"] += f" {angle}°"
            diagonal["rotation"] = {"angle": angle, "axis": "y", "origin": [8, 8, 8]}
            elements.append(diagonal)

    halo_display = {
        "gui": {"rotation": [35, 0, 0], "scale": [0.9] * 3},
        "ground": {"translation": [0, 2, 0], "scale": [0.5] * 3},
        "fixed": {"rotation": [-90, 0, 0], "scale": [0.9] * 3},
    }
    write_cosmetic("halo", elements, img, display_settings=halo_display)


def owl():
    feather, feather_dark, feather_light = (142, 100, 64, 255), (108, 74, 46, 255), (170, 126, 84, 255)
    belly, belly_spot = (240, 224, 192, 255), (170, 128, 88, 255)
    disc, disc_edge = (228, 204, 164, 255), (150, 108, 70, 255)
    amber, pupil = (255, 178, 40, 255), (24, 18, 16, 255)
    beak = (240, 172, 52, 255)
    tuft = (92, 62, 40, 255)

    frames = []
    for blink in (False, True):
        img = canvas(feather, 32, 32)
        # Federn mit V-Muster (uv 0,0 - 8,8)
        for y in range(0, 16, 4):
            for x in range(0, 16, 4):
                img[y][x + 1] = img[y + 1][x + 2] = img[y][x + 3] = feather_dark
        fill(img, 0, 0, 16, 1, feather_light)
        # Bauch mit Tupfen (uv 8,0 - 16,8)
        fill(img, 16, 0, 32, 16, belly)
        for y in range(2, 16, 4):
            for x in range(18 + (y // 4) % 2 * 2, 32, 4):
                img[y][x] = belly_spot
        # Gesicht (uv 0,8 - 8,16): heller Gesichtsschleier, riesige Augen
        fill(img, 0, 16, 16, 32, feather)
        fill(img, 1, 17, 15, 31, disc)
        fill(img, 1, 17, 15, 18, disc_edge)
        fill(img, 7, 17, 9, 19, disc_edge)          # Herzform oben
        for ex in (2, 9):
            if blink:
                fill(img, ex, 23, ex + 5, 24, pupil)
                img[22][ex] = img[22][ex + 4] = pupil
            else:
                fill(img, ex, 20, ex + 5, 26, amber)
                fill(img, ex + 1, 21, ex + 4, 25, pupil)
                img[21][ex + 1] = img[22][ex + 1] = SPARKLE
        fill(img, 1, 27, 3, 28, BLUSH)
        fill(img, 13, 27, 15, 28, BLUSH)
        # Schnabel und Füße (uv 8,8 - 12,12)
        fill(img, 16, 16, 24, 24, beak)
        # Flügel (uv 12,8 - 16,12)
        fill(img, 24, 16, 32, 24, feather_dark)
        for y in range(17, 24, 2):
            fill(img, 24, y, 32, y + 1, feather)
        # Federohren (uv 8,12 - 12,16)
        fill(img, 16, 24, 24, 32, tuft)
        frames.extend(img)

    feather_uv, belly_uv, face_uv, beak_uv, wing_uv, tuft_uv = (
        [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12], [12, 8, 16, 12], [8, 12, 12, 16])

    body = [
        faces(cube("Körper", [5, 4.5, 6], [11, 10, 11], feather_uv), north=belly_uv),
        cube("Fuß links", [6, 3.5, 5], [7.5, 4.5, 7], beak_uv),
        cube("Fuß rechts", [8.5, 3.5, 5], [10, 4.5, 7], beak_uv),
        cube("Schwanzfedern", [6.5, 4.5, 11], [9.5, 6, 12.5], feather_uv),
    ]
    # Kopf: Drehpunkt (Hals) in der Modellmitte
    head = [
        faces(cube("Kopf", [4.5, 8, 5], [11.5, 13, 10.5], feather_uv), north=face_uv),
        cube("Schnabel", [7.5, 9, 4.5], [8.5, 10.5, 5], beak_uv),
        cube("Federohr links", [4.5, 13, 6.5], [6, 14.5, 7.5], tuft_uv),
        cube("Federohr rechts", [10, 13, 6.5], [11.5, 14.5, 7.5], tuft_uv),
    ]
    wing_a = [cube("Flügel", [8, 3, 6.5], [9, 8, 10.5], wing_uv)]
    wing_b = [cube("Flügel", [7, 3, 6.5], [8, 8, 10.5], wing_uv)]

    animation = {"frametime": 1, "frames": [{"index": 0, "time": 80}, {"index": 1, "time": 4},
                                            {"index": 0, "time": 5}, {"index": 1, "time": 4}]}
    icon = body + shift(head, 0, 2, 0.25) + shift(wing_a, 3, 1.5, 0.5) + shift(wing_b, -3, 1.5, 0.5)
    write_cosmetic("owl", icon, frames, display_settings=PET_DISPLAY, animation=animation)
    write_extra_model("owl_body", "owl", body)
    write_extra_model("owl_head", "owl", head)
    write_extra_model("owl_wing_a", "owl", wing_a)
    write_extra_model("owl_wing_b", "owl", wing_b)



# ----------------------------------------------------------------- Emojis
# Emojis werden als 16x16 Pixel-Art gezeichnet (ein Buchstabe = eine Farbe, "." = durchsichtig)
# und dann zu einem 3D-Modell "extrudiert": jede zusammenhängende Pixelreihe wird ein kleiner Quader.
EMOJI_COLORS = {
    "Y": (255, 204, 50, 255),    # Gesicht gelb
    "O": (214, 140, 24, 255),    # Umriss
    "K": (60, 38, 30, 255),      # Augen/Mund
    "W": (255, 255, 255, 255),
    "R": (232, 44, 64, 255),     # Rot
    "D": (176, 24, 44, 255),     # Dunkelrot
    "P": (255, 140, 160, 255),   # Bäckchen
    "B": (90, 170, 255, 255),    # Tränen
    "A": (246, 96, 52, 255),     # Wütend-Gesicht
    "M": (190, 50, 30, 255),     # Wütend-Umriss
    "L": (160, 210, 255, 255),   # Zzz hell
    "N": (80, 130, 220, 255),    # Zzz dunkel / Ärmel
    "G": (80, 200, 110, 255),
    "V": (200, 110, 240, 255),
}

EMOJI_FACE = [
    "....OOOOOOOO....",
    "..OOYYYYYYYYOO..",
    ".OYYYYYYYYYYYYO.",
    ".OYYYYYYYYYYYYO.",
    "OYYYYYYYYYYYYYYO",
    "OYYYYYYYYYYYYYYO",
    "OYYYYYYYYYYYYYYO",
    "OYYYYYYYYYYYYYYO",
    "OYYYYYYYYYYYYYYO",
    "OYYYYYYYYYYYYYYO",
    "OYYYYYYYYYYYYYYO",
    "OYYYYYYYYYYYYYYO",
    ".OYYYYYYYYYYYYO.",
    ".OYYYYYYYYYYYYO.",
    "..OOYYYYYYYYOO..",
    "....OOOOOOOO....",
]


def overlay(base, top):
    return ["".join(t if t != "." else b for b, t in zip(brow, trow)) for brow, trow in zip(base, top)]


def recolor(rows, mapping):
    return ["".join(mapping.get(c, c) for c in row) for row in rows]


EMOJIS = {
    "heart": [
        "................",
        "................",
        "..RRRR....RRRR..",
        ".RRWWRR..RRRRRR.",
        "RRWWRRRRRRRRRRDR",
        "RRWRRRRRRRRRRRDR",
        "RRRRRRRRRRRRRRDR",
        "RRRRRRRRRRRRRDDR",
        ".RRRRRRRRRRRRDR.",
        "..RRRRRRRRRRDR..",
        "...RRRRRRRRDR...",
        "....RRRRRRDR....",
        ".....RRRRDR.....",
        "......RRDR......",
        ".......RR.......",
        "................",
    ],
    "laugh": overlay(EMOJI_FACE, [
        "................",
        "................",
        "................",
        "................",
        "...KKK....KKK...",
        "..K...K..K...K..",
        "................",
        "BB............BB",
        "BB.KKKKKKKKKK.BB",
        ".B.KWWWWWWWWK.B.",
        "...KKKKKKKKKK...",
        "...KKKRRRRKKK...",
        "....KKRRRRKK....",
        ".....KKKKKK.....",
        "................",
        "................",
    ]),
    "angry": overlay(recolor(EMOJI_FACE, {"Y": "A", "O": "M"}), [
        "................",
        "................",
        "................",
        "..KK........KK..",
        "...KKK....KKK...",
        "....KK....KK....",
        "...KKK....KKK...",
        "...KKK....KKK...",
        "................",
        "................",
        ".....KKKKKK.....",
        "....K......K....",
        "...K........K...",
        "................",
        "................",
        "................",
    ]),
    "wow": overlay(EMOJI_FACE, [
        "................",
        "................",
        "...KK......KK...",
        "................",
        "...KKK....KKK...",
        "...KWK....KWK...",
        "...KKK....KKK...",
        "................",
        "................",
        "......KKKK......",
        ".....KK..KK.....",
        ".....KK..KK.....",
        "......KKKK......",
        "................",
        "................",
        "................",
    ]),
    "thumbs": [
        "................",
        ".......OO.......",
        "......OYYO......",
        "......OYYO......",
        ".....OYYYO......",
        "....OYYYYOOOOO..",
        "OOOOOYYYYYYYYYO.",
        "ONNOYYYYYYYYYYO.",
        "ONNOYYYYYYYYOOO.",
        "ONNOYYYYYYYYYYO.",
        "ONNOYYYYYYYYOOO.",
        "ONNOYYYYYYYYYYO.",
        "ONNOYYYYYYYYOOO.",
        "ONNOOYYYYYYYYO..",
        "OOOO.OOOOOOOO...",
        "................",
    ],
    "sleepy": [
        "................",
        ".......NNNNNNN..",
        ".......NLLLLLN..",
        ".........NLLN...",
        "........NLLN....",
        ".......NLLLLLN..",
        "..NNNNNNNNNNNN..",
        "..NLLLLN........",
        "....NLN.........",
        "...NLN..........",
        "..NLLLLN........",
        "..NNNNNN..NNNN..",
        "..........NLLN..",
        "...........NN...",
        "..........NLLN..",
        "..........NNNN..",
    ],
    "party": [
        "..........R..N..",
        "....G.........Y.",
        "..........Y.....",
        ".......N.....R..",
        "..Y.......OOO...",
        ".........OYYO..G",
        "........OYRYO...",
        ".......OYYYGO...",
        "......OYNYYO....",
        ".....OYYYRYO....",
        "....OYGYYYO.....",
        "...OYYYNYO......",
        "..OYYRYYO.......",
        ".OYYYYOO........",
        "OOOOOO..........",
        "................",
    ],
}


def extrude_sprite(rows, depth=2.0):
    """Macht aus Pixel-Art ein 3D-Modell: jede zusammenhängende Pixelreihe wird ein Quader."""
    z0, z1 = 8 - depth / 2, 8 + depth / 2
    elements = []
    for y, row in enumerate(rows):
        x = 0
        while x < 16:
            if row[x] == ".":
                x += 1
                continue
            start = x
            while x < 16 and row[x] != ".":
                x += 1
            end = x  # exklusiv
            top, bottom = 16 - y, 15 - y
            elements.append({
                "from": [start, bottom, z0], "to": [end, top, z1],
                "faces": {
                    # Vorder- und Rückseite zeigen das Bild (Rückseite gespiegelt, damit es richtig herum ist)
                    "south": {"uv": [start, y, end, y + 1], "texture": "#0"},
                    "north": {"uv": [end, y, start, y + 1], "texture": "#0"},
                    "up": {"uv": [start, y, end, y + 1], "texture": "#0"},
                    "down": {"uv": [start, y, end, y + 1], "texture": "#0"},
                    "west": {"uv": [start, y, start + 1, y + 1], "texture": "#0"},
                    "east": {"uv": [end - 1, y, end, y + 1], "texture": "#0"},
                },
            })
    return elements


def emojis():
    emoji_display = {
        "gui": {"rotation": [0, 0, 0], "scale": [1, 1, 1]},
        "ground": {"translation": [0, 2, 0], "scale": [0.5] * 3},
        "fixed": {"scale": [1, 1, 1]},
    }
    for name, rows in EMOJIS.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
        img = [[EMOJI_COLORS[c] if c != "." else (0, 0, 0, 0) for c in row] for row in rows]
        write_cosmetic(f"emoji_{name}", extrude_sprite(rows), img, display_settings=emoji_display)



# ------------------------------------------------------------------ Truhe
def crate():
    wood, wood_dark, wood_light = (104, 60, 160, 255), (78, 42, 124, 255), (128, 82, 186, 255)
    gold, gold_dark, gold_light = (236, 186, 56, 255), (186, 136, 30, 255), (255, 226, 120, 255)
    gem, gem_light = (70, 220, 255, 255), (200, 250, 255, 255)
    star = (255, 240, 170, 255)

    img = canvas(wood, 32, 32)
    # Seiten (uv 0,0 - 8,8): Bretter mit Goldrahmen
    for y in range(0, 16, 4):
        fill(img, 0, y, 16, y + 1, wood_dark)
    fill(img, 2, 1, 14, 2, wood_light)
    fill(img, 0, 0, 16, 1, gold)
    fill(img, 0, 15, 16, 16, gold_dark)
    fill(img, 0, 0, 1, 16, gold)
    fill(img, 15, 0, 16, 16, gold_dark)
    # Deckel oben (uv 8,0 - 16,8): Goldkreuz und kleine Sterne
    fill(img, 16, 0, 32, 16, wood)
    fill(img, 23, 0, 25, 16, gold)
    fill(img, 16, 7, 32, 9, gold)
    for sx, sy in ((18, 2), (29, 3), (19, 12), (28, 13)):
        img[sy][sx] = star
        img[sy - 1][sx] = img[sy + 1][sx] = img[sy][sx - 1] = img[sy][sx + 1] = gold_light
    fill(img, 16, 0, 32, 1, gold)
    fill(img, 16, 15, 32, 16, gold_dark)
    # Gold (uv 0,8 - 8,16)
    fill(img, 0, 16, 16, 32, gold)
    fill(img, 0, 16, 16, 18, gold_light)
    fill(img, 0, 29, 16, 32, gold_dark)
    # Edelstein (uv 8,8 - 12,12)
    fill(img, 16, 16, 24, 24, gem)
    fill(img, 17, 17, 20, 20, gem_light)

    side, top, metal, jewel = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12]

    body = [
        faces(cube("Truhe", [2, 0, 3], [14, 8, 13], side), up=top, down=top),
        cube("Ecke vorne links", [1.75, 0, 2.75], [3, 8, 4], metal),
        cube("Ecke vorne rechts", [13, 0, 2.75], [14.25, 8, 4], metal),
        cube("Ecke hinten links", [1.75, 0, 12], [3, 8, 13.25], metal),
        cube("Ecke hinten rechts", [13, 0, 12], [14.25, 8, 13.25], metal),
        cube("Schloss", [6.5, 3.5, 2.5], [9.5, 7, 3], metal),
        cube("Schloss-Stein", [7.25, 4.25, 2.25], [8.75, 5.75, 2.5], jewel),
    ]
    # Deckel: Scharnier liegt in der Modellmitte (8, 8, 8) = hinten oben an der Truhe
    lid = [
        faces(cube("Deckel", [2, 8, -2], [14, 11, 8], side), up=top, down=top),
        faces(cube("Deckel Wölbung", [3, 11, -1], [13, 12, 7], side), up=top),
        cube("Deckel-Lasche", [7, 7.5, -2.5], [9, 9.5, -2], metal),
        cube("Edelstein oben", [7, 12, 2], [9, 13.25, 4], jewel),
        cube("Band links", [4.5, 8, -2.25], [5.5, 11.25, 8.25], metal),
        cube("Band rechts", [10.5, 8, -2.25], [11.5, 11.25, 8.25], metal),
    ]
    crate_display = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -1.5, 0], "scale": [0.6] * 3},
        "ground": {"translation": [0, 3, 0], "scale": [0.4] * 3},
        "fixed": {"rotation": [0, 180, 0], "scale": [0.6] * 3},
    }
    write_cosmetic("crate", body + shift(lid, 0, 0, 5), img, display_settings=crate_display)
    write_extra_model("crate_body", "crate", body)
    write_extra_model("crate_lid", "crate", lid)



# ---------------------------------------------------------------- Flügel
# Die Flügelformen werden berechnet (Kurven für Ober- und Unterkante) statt von Hand gepixelt.
# Danach wird die 16x16-Pixelgrafik zu einem 3D-Modell extrudiert.
# Flügel A zeigt im Modell nach +x, Flügel B ist gespiegelt. Das Gelenk (Wurzel am Rücken)
# liegt in der Modellmitte (8, 8, 8), damit die Flügel sich darum drehen.
import math

WING_ROOT_ROW = 6.5


def extrude_pixels(img, depth=1.0, dx=0.0, dy=0.0, mirror=False):
    """Wie extrude_sprite, aber für fertige Pixel (durchsichtig = Alpha 0), optional gespiegelt."""
    z0, z1 = 8 - depth / 2, 8 + depth / 2
    elements = []
    for y in range(16):
        x = 0
        while x < 16:
            if img[y][x][3] == 0:
                x += 1
                continue
            start = x
            while x < 16 and img[y][x][3] != 0:
                x += 1
            end = x
            gx0, gx1 = (16 - end, 16 - start) if mirror else (start, end)
            forward = [start, y, end, y + 1]
            backward = [end, y, start, y + 1]
            elements.append({
                "from": [gx0 + dx, 15 - y + dy, z0], "to": [gx1 + dx, 16 - y + dy, z1],
                "faces": {
                    "south": {"uv": backward if mirror else forward, "texture": "#0"},
                    "north": {"uv": forward if mirror else backward, "texture": "#0"},
                    "up": {"uv": forward, "texture": "#0"},
                    "down": {"uv": forward, "texture": "#0"},
                    "west": {"uv": [start, y, start + 1, y + 1], "texture": "#0"},
                    "east": {"uv": [end - 1, y, end, y + 1], "texture": "#0"},
                },
            })
    return elements


def blend(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


def wing_sprite(style):
    clear = (0, 0, 0, 0)
    img = [[clear] * 16 for _ in range(16)]

    def inside(x, y):
        cx, cy = x + 0.5, y + 0.5
        if style == "angel":
            # Hochgewölbte Vorderkante, lange Federn hängen nach unten
            top = 5.0 - 4.6 * math.sin(math.pi * cx / 17)
            bottom = 15.6 - 0.42 * cx - 1.3 * abs(math.sin(cx * 0.9))
            return top <= cy <= bottom
        if style in ("demon", "dragon"):
            # Fledermaus-Flügel: gewölbter Knochen oben, Membran-Bögen zwischen den Fingern
            top = 5.0 - 4.2 * math.sin(math.pi * cx / 17)
            bottom = 15.2 - 0.38 * cx - 3.2 * math.sin(math.pi * ((cx % 5) / 5.0))
            return top <= cy <= bottom
        if style == "pixie":
            # Fee: zwei lange, schmale Libellenflügel
            upper = ((cx - 8.5) / 7.8) ** 2 + ((cy - 4.2) / 3.0) ** 2 <= 1
            lower = ((cx - 6.5) / 6.3) ** 2 + ((cy - 10.2) / 2.6) ** 2 <= 1
            return upper or lower or (cx < 2 and 3 <= cy <= 12)
        # Schmetterling: ein großer oberer und ein kleiner unterer Flügellappen
        upper = ((cx - 8.5) / 7.5) ** 2 + ((cy - 5.0) / 4.6) ** 2 <= 1
        lower = ((cx - 5.0) / 5.0) ** 2 + ((cy - 11.5) / 3.8) ** 2 <= 1
        root = cx < 2.5 and 4 <= cy <= 11
        return upper or lower or root

    mask = [[inside(x, y) for x in range(16)] for y in range(16)]

    def edge(x, y):
        return any(not (0 <= x + ox < 16 and 0 <= y + oy < 16 and mask[y + oy][x + ox])
                   for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)))

    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            if style == "angel":
                color = (246, 246, 252, 255)
                if (x + int(y * 0.7)) % 4 == 0:
                    color = (214, 222, 240, 255)            # Federlinien
                if not mask[min(15, y + 2)][x] or not mask[min(15, y + 1)][x]:
                    color = (255, 236, 170, 255)            # goldene Federspitzen
                if y + 0.5 < 5.0 - 4.6 * math.sin(math.pi * (x + 0.5) / 17) + 1.2:
                    color = (255, 255, 255, 255)            # heller Flügelbug
            elif style in ("demon", "dragon"):
                membrane_top = (150, 26, 40, 255) if style == "demon" else (70, 160, 80, 255)
                membrane_bottom = (86, 12, 24, 255) if style == "demon" else (36, 104, 52, 255)
                bone = (40, 10, 14, 255) if style == "demon" else (24, 66, 34, 255)
                color = blend(membrane_top, membrane_bottom, y / 14)
                top_edge = 5.0 - 4.2 * math.sin(math.pi * (x + 0.5) / 17)
                on_bone = abs(y + 0.5 - top_edge) < 1.0 or (x % 5 == 4) or x == 0
                if on_bone:
                    color = bone
                if style == "dragon" and not on_bone and (x + y) % 5 == 0:
                    color = (110, 190, 100, 255)            # Schuppen-Glanz
            elif style == "butterfly":
                color = blend((70, 160, 255, 255), (30, 70, 200, 255), x / 15)
                if y >= 9:
                    color = (255, 150, 40, 255)             # orangefarbener unterer Flügel
                if edge(x, y):
                    color = (24, 22, 40, 255)
            else:  # pixie (Fee): zarte Pastellfarben, halb durchsichtig
                color = blend((255, 170, 220, 170), (150, 230, 255, 170), x / 15)
                if edge(x, y):
                    color = (255, 255, 255, 220)
            img[y][x] = color

    # Kleine Details
    if style == "butterfly":
        for sx, sy in ((11, 3), (13, 5), (9, 6), (4, 12)):
            if mask[sy][sx] and not edge(sx, sy):
                img[sy][sx] = (255, 255, 255, 255)
    if style == "pixie":
        for sx, sy in ((10, 3), (6, 5), (13, 5), (4, 11), (7, 12)):
            if mask[sy][sx]:
                img[sy][sx] = (255, 255, 255, 255)
    return img


def wings(cosmetic_id, style):
    img = wing_sprite(style)
    dy = WING_ROOT_ROW - 7.5
    wing_a = extrude_pixels(img, dx=8, dy=dy)
    wing_b = extrude_pixels(img, dx=-8, dy=dy, mirror=True)
    wing_display = {
        "gui": {"rotation": [0, 180, 0], "scale": [0.45] * 3},
        "ground": {"translation": [0, 2, 0], "scale": [0.3] * 3},
        "fixed": {"scale": [0.45] * 3},
    }
    write_cosmetic(cosmetic_id, wing_a + wing_b, img, display_settings=wing_display)
    write_extra_model(f"{cosmetic_id}_wing_a", cosmetic_id, wing_a)
    write_extra_model(f"{cosmetic_id}_wing_b", cosmetic_id, wing_b)



# ------------------------------------------------------------ Neue Hüte
# Einfache Hüte nutzen eine Paletten-Textur: 16 Farbfelder à 4x4 Pixel.
# uv(i) gibt den Texturbereich von Feld i zurück. Felder können auch Muster enthalten.
def palette(cells):
    """cells: Liste von Farben (r, g, b, a) oder Funktionen f(img, x0, y0), die ein 4x4-Feld bemalen."""
    img = canvas((0, 0, 0, 0))
    for i, cell in enumerate(cells):
        x0, y0 = (i % 4) * 4, (i // 4) * 4
        if callable(cell):
            cell(img, x0, y0)
        else:
            fill(img, x0, y0, x0 + 4, y0 + 4, cell)
    return img


def uv(i):
    x0, y0 = (i % 4) * 4, (i // 4) * 4
    return [x0, y0, x0 + 4, y0 + 4]


def mirror_x(elements):
    """Spiegelt Teile an der Kopfmitte (x = 8), z. B. für das rechte Horn."""
    mirrored = []
    for element in elements:
        copy = json.loads(json.dumps(element))
        x0, x1 = copy["from"][0], copy["to"][0]
        copy["from"][0], copy["to"][0] = round(16 - x1, 3), round(16 - x0, 3)
        copy["faces"]["east"], copy["faces"]["west"] = copy["faces"]["west"], copy["faces"]["east"]
        mirrored.append(copy)
    return mirrored


def stripes(colors):
    def paint(img, x0, y0):
        for y in range(4):
            for x in range(4):
                img[y0 + y][x0 + x] = colors[(x + y) % len(colors)]
    return paint


def dotted(base, dot):
    def paint(img, x0, y0):
        fill(img, x0, y0, x0 + 4, y0 + 4, base)
        img[y0 + 1][x0 + 1] = img[y0 + 2][x0 + 2] = dot
    return paint


def new_hats():
    t = HEAD_TOP

    # Wikingerhelm
    metal, metal_dark, rivet, horn, horn_tip = (164, 170, 180, 255), (118, 124, 136, 255), (220, 224, 230, 255), (238, 228, 204, 255), (196, 184, 156, 255)
    img = palette([metal, metal_dark, dotted(metal_dark, rivet), horn, horn_tip])
    horn_left = [cube("Horn", [-1, t - 1, 7], [1.2, t + 1, 9], uv(3)),
                 cube("Horn Mitte", [-2, t + 1, 7.25], [-0.5, t + 4, 8.75], uv(3)),
                 cube("Hornspitze", [-1.8, t + 4, 7.5], [-1, t + 6, 8.5], uv(4))]
    elements = [
        cube("Helm", [1.2, t - 3, 1.2], [14.8, t + 1, 14.8], uv(0)),
        cube("Kuppel", [3, t + 1, 3], [13, t + 3, 13], uv(0)),
        cube("Rand", [1, t - 3.2, 1], [15, t - 2.2, 15], uv(2)),
        cube("Nasenschutz", [7.3, t - 7, 0.7], [8.7, t - 3, 1.2], uv(1)),
    ] + horn_left + mirror_x(horn_left)
    write_cosmetic("viking_helmet", elements, img, center_y=t + 1, gui_scale=0.6)

    # Cowboyhut
    brown, brown_dark, band = (150, 98, 56, 255), (116, 74, 40, 255), (62, 40, 24, 255)
    img = palette([brown, brown_dark, band])
    elements = [
        cube("Krempe", [-1, t, -1], [17, t + 0.8, 17], uv(0)),
        cube("Krempe links hoch", [-1.5, t + 0.8, -1], [0.5, t + 1.8, 17], uv(1)),
        cube("Krempe rechts hoch", [15.5, t + 0.8, -1], [17.5, t + 1.8, 17], uv(1)),
        cube("Hut", [3, t + 0.8, 3.5], [13, t + 5.5, 12.5], uv(0)),
        cube("Delle", [3.5, t + 5.5, 5], [12.5, t + 6.3, 11], uv(1)),
        cube("Hutband", [2.9, t + 0.8, 3.4], [13.1, t + 2, 12.6], uv(2)),
    ]
    write_cosmetic("cowboy_hat", elements, img, center_y=t + 3, gui_scale=0.55)

    # Hexenhut
    black, purple, gold = (40, 34, 52, 255), (124, 62, 178, 255), (240, 196, 70, 255)
    img = palette([black, purple, gold])
    elements = [
        cube("Krempe", [-0.5, t, -0.5], [16.5, t + 0.8, 16.5], uv(0)),
        cube("Hut 1", [3, t + 0.8, 3], [13, t + 4, 13], uv(0)),
        cube("Hut 2", [4.5, t + 4, 4.5], [11.5, t + 7, 11.5], uv(0)),
        cube("Hut 3", [6, t + 7, 6.5], [10, t + 10, 10.5], uv(0)),
        cube("Hut 4", [7, t + 10, 8], [9.5, t + 12, 10.5], uv(0)),
        cube("Spitze", [7.5, t + 11.5, 10.5], [9, t + 12.5, 13], uv(0)),
        cube("Band", [2.9, t + 0.8, 2.9], [13.1, t + 2, 13.1], uv(1)),
        cube("Schnalle", [7, t + 0.7, 2.6], [9, t + 2.1, 2.9], uv(2)),
    ]
    write_cosmetic("witch_hat", elements, img, center_y=t + 6, gui_scale=0.45)

    # Weihnachtsmütze
    red, red_dark, white = (212, 36, 44, 255), (168, 22, 30, 255), (248, 248, 244, 255)
    img = palette([red, red_dark, white])
    elements = [
        cube("Fellrand", [1, t - 1, 1], [15, t + 1, 15], uv(2)),
        cube("Mütze 1", [2.5, t + 1, 2.5], [13.5, t + 4, 13.5], uv(0)),
        cube("Mütze 2", [4, t + 4, 4.5], [12, t + 6.5, 12], uv(0)),
        cube("Mütze 3", [6, t + 6.5, 7], [11, t + 8, 12], uv(1)),
        cube("Zipfel", [9, t + 6, 11], [12.5, t + 7.5, 15], uv(1)),
        cube("Bommel", [11, t + 4.5, 14], [14, t + 7.5, 17], uv(2)),
    ]
    write_cosmetic("santa_hat", elements, img, center_y=t + 4, gui_scale=0.55)

    # Partyhut (leicht schräg)
    img = palette([stripes([(255, 90, 120, 255), (255, 214, 70, 255), (90, 190, 255, 255)]), (255, 214, 70, 255)])
    elements = [
        cube("Kegel 1", [5, t, 5], [11, t + 2, 11], uv(0)),
        cube("Kegel 2", [5.75, t + 2, 5.75], [10.25, t + 4, 10.25], uv(0)),
        cube("Kegel 3", [6.5, t + 4, 6.5], [9.5, t + 6, 9.5], uv(0)),
        cube("Kegel 4", [7.25, t + 6, 7.25], [8.75, t + 8, 8.75], uv(0)),
        cube("Bommel", [7, t + 8, 7], [9, t + 10, 9], uv(1)),
    ]
    for element in elements:
        element["rotation"] = {"angle": 22.5, "axis": "z", "origin": [8, t, 8]}
    write_cosmetic("party_hat", elements, img, center_y=t + 5, gui_scale=0.6)

    # Katzenohren
    fur, pink, band_dark = (70, 64, 78, 255), (255, 170, 190, 255), (40, 36, 46, 255)
    img = palette([fur, pink, band_dark])
    ear = [faces(cube("Ohr", [2.5, t + 0.5, 6.3], [6, t + 2.5, 8.2], uv(0)), north=uv(1)),
           faces(cube("Ohr Mitte", [3, t + 2.5, 6.3], [5.5, t + 4, 8.2], uv(0)), north=uv(1)),
           cube("Ohrspitze", [3.5, t + 4, 6.3], [5, t + 5, 8.2], uv(0))]
    elements = [cube("Haarreif", [1.4, t - 0.5, 6.5], [14.6, t + 0.5, 8], uv(2))] + ear + mirror_x(ear)
    write_cosmetic("cat_ears", elements, img, center_y=t + 2, gui_scale=0.7)

    # Hasenohren (eins steht, eins ist umgeknickt)
    white, pink = (250, 250, 250, 255), (255, 176, 196, 255)
    img = palette([white, pink])
    elements = [
        cube("Haarreif", [1.4, t - 0.5, 7], [14.6, t + 0.5, 8.5], uv(0)),
        faces(cube("Ohr links", [3.5, t + 0.5, 7], [6.5, t + 9, 8.5], uv(0)), north=uv(1)),
        faces(cube("Ohr rechts unten", [9.5, t + 0.5, 7], [12.5, t + 5, 8.5], uv(0)), north=uv(1)),
        faces(cube("Ohr rechts geknickt", [10.5, t + 4, 7], [15, t + 6.5, 8.5], uv(0)), up=uv(1)),
    ]
    write_cosmetic("bunny_ears", elements, img, center_y=t + 4, gui_scale=0.55)

    # Kopfhörer
    dark, pad, accent = (52, 52, 60, 255), (34, 34, 40, 255), (64, 214, 232, 255)
    img = palette([dark, pad, accent])
    side = [cube("Bügel", [0.2, t - 5, 7], [1.4, t, 9], uv(0)),
            cube("Muschel", [-0.8, t - 8.5, 5.5], [1.6, t - 4.5, 10.5], uv(2)),
            cube("Polster", [1.6, t - 8, 6], [1.8, t - 5, 10], uv(1))]
    elements = [cube("Band", [1, t - 0.5, 7], [15, t + 1, 9], uv(0)),
                cube("Polster oben", [4, t + 1, 7.25], [12, t + 1.6, 8.75], uv(1))] + side + mirror_x(side)
    write_cosmetic("headphones", elements, img, center_y=t - 3, gui_scale=0.6)

    # Blumenkranz
    leaf = (70, 170, 80, 255)
    def flower(petal):
        def paint(img, x0, y0):
            fill(img, x0, y0, x0 + 4, y0 + 4, petal)
            fill(img, x0 + 1, y0 + 1, x0 + 3, y0 + 3, (255, 214, 60, 255))
        return paint
    img = palette([leaf, flower((255, 150, 190, 255)), flower((255, 255, 255, 255)),
                   flower((130, 170, 255, 255)), flower((255, 200, 90, 255))])
    elements = [
        cube("Kranz vorne", [1.4, t - 1, 1.4], [14.6, t, 2.6], uv(0)),
        cube("Kranz hinten", [1.4, t - 1, 13.4], [14.6, t, 14.6], uv(0)),
        cube("Kranz links", [1.4, t - 1, 2.6], [2.6, t, 13.4], uv(0)),
        cube("Kranz rechts", [13.4, t - 1, 2.6], [14.6, t, 13.4], uv(0)),
    ]
    spots = [(3, 1.2), (7, 1.0), (11, 1.2), (13.2, 5), (13.2, 10), (11, 13.2), (6.5, 13.2), (1.2, 10.5), (1.2, 5.5)]
    for i, (x, z) in enumerate(spots):
        elements.append(cube("Blume", [x, t - 0.6, z], [x + 1.8, t + 0.8, z + 1.8], uv(1 + i % 4)))
    write_cosmetic("flower_crown", elements, img, center_y=t, gui_scale=0.7)

    # Teufelshörner
    red, red_dark = (200, 30, 40, 255), (140, 16, 24, 255)
    img = palette([red, red_dark])
    horn = [cube("Horn", [4, t, 6], [6, t + 1.5, 8], uv(0)),
            cube("Horn Mitte", [4.5, t + 1.5, 6.3], [5.8, t + 3, 7.7], uv(0)),
            cube("Hornspitze", [5, t + 3, 6.6], [5.8, t + 4, 7.4], uv(1))]
    write_cosmetic("devil_horns", horn + mirror_x(horn), img, center_y=t + 2, gui_scale=0.8)



# ------------------------------------------------------- Weitere Haustiere
# Diese Tiere nutzen die Animationen der vorhandenen Haustiere (gleiche Teile-Namen):
# Fuchs = Kätzchen, Axolotl = Geist, Panda = Pinguin, Schleim = Pilzchen, Baby-Phönix = Drache.
def face_region(img, x0, y0, base, eye=(28, 26, 40, 255), eye_w=3, eye_h=4, eye_y=4, gap=5, blush=True, mouth=None):
    """Malt ein süßes Gesicht in ein 16x16-Feld der Textur."""
    fill(img, x0, y0, x0 + 16, y0 + 16, base)
    left = x0 + 8 - gap // 2 - eye_w
    right = x0 + 8 + (gap + 1) // 2
    for ex in (left, right):
        fill(img, ex, y0 + eye_y, ex + eye_w, y0 + eye_y + eye_h, eye)
        img[y0 + eye_y][ex] = SPARKLE
    if blush:
        fill(img, left - 2, y0 + eye_y + eye_h + 1, left, y0 + eye_y + eye_h + 2, BLUSH)
        fill(img, right + eye_w, y0 + eye_y + eye_h + 1, right + eye_w + 2, y0 + eye_y + eye_h + 2, BLUSH)
    if mouth:
        mouth(img, x0, y0)


def fox():
    orange, orange_dark, cream, dark = (236, 124, 44, 255), (200, 96, 30, 255), (252, 244, 232, 255), (58, 40, 34, 255)
    img = canvas(orange, 32, 32)
    for x in range(0, 16, 5):
        fill(img, x, 0, x + 1, 16, orange_dark)
    # Gesicht wach (uv 8,0 - 16,8): weiße Wangen
    def muzzle(img, x0, y0):
        fill(img, x0 + 1, y0 + 9, x0 + 15, y0 + 16, cream)
    face_region(img, 16, 0, orange, eye_y=4, mouth=muzzle)
    # Gesicht schlafend (uv 0,8 - 8,16)
    fill(img, 0, 16, 16, 32, orange)
    fill(img, 1, 25, 15, 32, cream)
    for ex in (3, 10):
        img[22][ex] = img[21][ex + 1] = img[21][ex + 2] = img[22][ex + 3] = dark
    fill(img, 16, 16, 24, 24, cream)          # Creme (uv 8,8 - 12,12)
    fill(img, 24, 16, 32, 24, dark)           # Pfoten/Nase (uv 12,8 - 16,12)
    fill(img, 16, 24, 24, 32, orange)         # Schwanz (uv 8,12 - 12,16)
    fill(img, 16, 24, 24, 26, orange_dark)

    fur, face, sleep_face, cream_uv, dark_uv, tail_uv = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12], [12, 8, 16, 12], [8, 12, 12, 16]
    body = [
        faces(cube("Körper", [5.5, 4, 7], [10.5, 8, 11], fur), north=cream_uv),
        faces(cube("Kopf", [4.5, 8, 5], [11.5, 12.5, 10.5], fur), north=face),
        faces(cube("Schnauze", [6.5, 8, 3.5], [9.5, 10, 5], cream_uv), north=cream_uv),
        cube("Nase", [7.5, 9.25, 3.25], [8.5, 10, 3.5], dark_uv),
        faces(cube("Ohr links", [4.5, 12.5, 6.5], [6.75, 15, 7.5], fur), north=dark_uv),
        faces(cube("Ohr rechts", [9.25, 12.5, 6.5], [11.5, 15, 7.5], fur), north=dark_uv),
        cube("Pfote links", [6, 4, 6], [7.5, 5, 7], dark_uv),
        cube("Pfote rechts", [8.5, 4, 6], [10, 5, 7], dark_uv),
    ]
    tail = [
        cube("Schwanz", [6.5, 7, 8], [9.5, 10.5, 14], tail_uv),
        cube("Schwanzspitze", [6.75, 7.25, 14], [9.25, 10.25, 16.5], cream_uv),
    ]
    sleep = [
        cube("Körper", [4.5, 4, 6.5], [11.5, 8, 11.5], fur),
        faces(cube("Kopf", [5, 4, 2.5], [11, 8.5, 7], fur), north=sleep_face),
        faces(cube("Ohr links", [5, 8.5, 4], [7, 10.5, 5], fur), north=dark_uv),
        faces(cube("Ohr rechts", [9, 8.5, 4], [11, 10.5, 5], fur), north=dark_uv),
        cube("Schwanz", [2.5, 4, 1.5], [5, 7, 11.5], tail_uv),
        cube("Schwanz vorne", [2.5, 4, 0], [9.5, 6.5, 2.5], tail_uv),
        cube("Schwanzspitze", [9.5, 4, 0], [11.5, 6.5, 2.5], cream_uv),
    ]
    write_cosmetic("fox", body + shift(tail, 0, -3, 3), img, display_settings=PET_DISPLAY)
    write_extra_model("fox_body", "fox", body)
    write_extra_model("fox_tail", "fox", tail)
    write_extra_model("fox_sleep", "fox", sleep)


def axolotl():
    pink, pink_dark, gill, gill_dark = (255, 172, 200, 255), (236, 140, 172, 255), (236, 70, 130, 255), (190, 40, 100, 255)
    img = canvas(pink, 32, 32)
    fill(img, 0, 12, 16, 16, pink_dark)                               # Körper (uv 0,0 - 8,8)
    def smile(img, x0, y0):
        img[y0 + 11][x0 + 6] = img[y0 + 11][x0 + 9] = (120, 50, 70, 255)
        fill(img, x0 + 7, y0 + 12, x0 + 9, y0 + 13, (120, 50, 70, 255))
    face_region(img, 16, 0, pink, eye_w=2, eye_h=3, eye_y=5, gap=8, mouth=smile)   # Gesicht (uv 8,0 - 16,8)
    fill(img, 0, 16, 16, 32, gill)                                     # Kiemen (uv 0,8 - 8,16)
    fill(img, 0, 28, 16, 32, gill_dark)
    fill(img, 16, 16, 24, 24, pink_dark)                               # Beine/Flosse (uv 8,8 - 12,12)

    body_uv, face, gill_uv, fin = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12]
    elements = [
        faces(cube("Kopf", [3.5, 6, 2], [12.5, 11, 6.5], body_uv), north=face),
        cube("Körper", [4.5, 6, 6.5], [11.5, 9.5, 13], body_uv),
        cube("Schwanzflosse", [7.25, 6.5, 13], [8.75, 10, 17], fin),
        cube("Bein vorne links", [3.5, 5, 7], [4.5, 6.5, 8.5], fin),
        cube("Bein vorne rechts", [11.5, 5, 7], [12.5, 6.5, 8.5], fin),
        cube("Bein hinten links", [3.5, 5, 11], [4.5, 6.5, 12.5], fin),
        cube("Bein hinten rechts", [11.5, 5, 11], [12.5, 6.5, 12.5], fin),
    ]
    for y in (7, 8.75, 10.5):
        elements.append(cube("Kieme links", [1.5, y, 3.5], [3.5, y + 1, 4.5], gill_uv))
        elements.append(cube("Kieme rechts", [12.5, y, 3.5], [14.5, y + 1, 4.5], gill_uv))
    write_cosmetic("axolotl", elements, img, display_settings=PET_DISPLAY)


def panda():
    white, shade, black = (246, 246, 242, 255), (222, 222, 216, 255), (36, 34, 40, 255)
    img = canvas(white, 32, 32)
    fill(img, 0, 12, 16, 16, shade)                                    # Körper (uv 0,0 - 8,8)
    fill(img, 16, 0, 32, 16, white)                                    # Bauch (uv 8,0 - 16,8)
    # Gesicht (uv 0,8 - 8,16): schwarze Augenflecken mit glänzenden Augen
    fill(img, 0, 16, 16, 32, white)
    fill(img, 1, 19, 6, 25, black)
    fill(img, 10, 19, 15, 25, black)
    for ex in (3, 11):
        fill(img, ex, 20, ex + 2, 23, (250, 250, 250, 255))
        fill(img, ex, 21, ex + 2, 23, (20, 20, 26, 255))
    fill(img, 7, 25, 9, 26, black)
    fill(img, 1, 26, 3, 27, BLUSH)
    fill(img, 13, 26, 15, 27, BLUSH)
    fill(img, 16, 16, 24, 24, black)                                   # Schwarz (uv 8,8 - 12,12)

    fur, belly, face, dark = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12]
    body = [
        faces(cube("Körper", [4.5, 2, 5.5], [11.5, 8, 11.5], fur), north=belly),
        cube("Schulterband", [4.4, 6.5, 5.4], [11.6, 8, 11.6], dark),
        faces(cube("Kopf", [4.5, 8, 5.5], [11.5, 13.5, 11.5], fur), north=face),
        cube("Schnauze", [7, 8.5, 5], [9, 10, 5.5], fur),
        cube("Ohr links", [4.5, 13.5, 7], [6.5, 15, 8.5], dark),
        cube("Ohr rechts", [9.5, 13.5, 7], [11.5, 15, 8.5], dark),
        cube("Fuß links", [5, 1, 5], [7.25, 2, 8], dark),
        cube("Fuß rechts", [8.75, 1, 5], [11, 2, 8], dark),
    ]
    arm_a = [cube("Arm", [8, 3.5, 7], [9.2, 8, 10], dark)]
    arm_b = [cube("Arm", [6.8, 3.5, 7], [8, 8, 10], dark)]
    write_cosmetic("panda", body + shift(arm_a, 3.5, 0, 0.5) + shift(arm_b, -3.5, 0, 0.5), img, display_settings=PET_DISPLAY)
    write_extra_model("panda_body", "panda", body)
    write_extra_model("panda_flipper_a", "panda", arm_a)
    write_extra_model("panda_flipper_b", "panda", arm_b)


def slime():
    outer, outer_dark, core = (120, 220, 100, 200), (90, 190, 80, 200), (70, 170, 60, 255)
    img = canvas(outer, 32, 32)
    fill(img, 0, 13, 16, 16, outer_dark)                               # Außen (uv 0,0 - 8,8)
    def smile(img, x0, y0):
        fill(img, x0 + 6, y0 + 11, x0 + 10, y0 + 12, (40, 90, 40, 255))
    face_region(img, 16, 0, outer, eye=(30, 70, 30, 255), eye_w=3, eye_h=3, eye_y=5, gap=4, mouth=smile)
    fill(img, 0, 16, 16, 32, core)                                     # Kern (uv 0,8 - 8,16)

    elements = [
        faces(cube("Hülle", [3, 1, 3], [13, 11, 13], [0, 0, 8, 8]), north=[8, 0, 16, 8]),
        cube("Kern", [5.5, 3, 5.5], [10.5, 8, 10.5], [0, 8, 8, 16]),
    ]
    write_cosmetic("slime", elements, img, display_settings=PET_DISPLAY)


def baby_phoenix():
    red, orange, yellow, beak = (230, 60, 40, 255), (255, 140, 40, 255), (255, 214, 70, 255), (255, 190, 60, 255)
    img = canvas(red, 32, 32)
    for y in range(16):
        fill(img, 0, y, 16, y + 1, red if y < 6 else orange if y < 12 else yellow)    # Federn (uv 0,0 - 8,8)
    face_region(img, 16, 0, orange, eye_w=3, eye_h=3, eye_y=5, gap=6)                   # Gesicht (uv 8,0 - 16,8)
    for y in range(16, 32):                                                             # Flügel (uv 0,8 - 8,16)
        fill(img, 0, y, 16, y + 1, red if y < 22 else orange if y < 28 else yellow)
    fill(img, 16, 16, 24, 24, beak)                                                     # Schnabel (uv 8,8 - 12,12)
    fill(img, 24, 16, 32, 24, yellow)                                                   # Flamme (uv 12,8 - 16,12)
    fill(img, 24, 16, 32, 19, (255, 250, 200, 255))

    feathers, face, wing_uv, beak_uv, flame = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12], [12, 8, 16, 12]
    body = [
        cube("Körper", [5, 5, 6], [11, 10, 11], feathers),
        faces(cube("Kopf", [5.5, 9, 2.5], [10.5, 13.5, 7], feathers), north=face),
        cube("Schnabel", [7.25, 10, 1.5], [8.75, 11.5, 2.5], beak_uv),
        cube("Flammenkrone", [7, 13.5, 4], [9, 15.5, 6], flame),
        cube("Flammenkrone Spitze", [7.5, 15.5, 4.5], [8.5, 17, 5.5], flame),
        cube("Schwanzfeder links", [6, 6, 11], [7.5, 7.5, 16], feathers),
        cube("Schwanzfeder mitte", [7.25, 6.5, 11], [8.75, 8, 17], flame),
        cube("Schwanzfeder rechts", [8.5, 6, 11], [10, 7.5, 16], feathers),
        cube("Fuß links", [6, 4, 7], [7.5, 5, 8.5], beak_uv),
        cube("Fuß rechts", [8.5, 4, 7], [10, 5, 8.5], beak_uv),
    ]
    wing_a = [cube("Flügel", [8, 8, 7], [15, 8.75, 12], wing_uv),
              cube("Flügelspitze", [15, 8, 8], [17, 8.75, 11], flame)]
    wing_b = [cube("Flügel", [1, 8, 7], [8, 8.75, 12], wing_uv),
              cube("Flügelspitze", [-1, 8, 8], [1, 8.75, 11], flame)]
    write_cosmetic("baby_phoenix", body + shift(wing_a, 3, 2, 0.5) + shift(wing_b, -3, 2, 0.5), img, display_settings=PET_DISPLAY)
    write_extra_model("baby_phoenix_body", "baby_phoenix", body)
    write_extra_model("baby_phoenix_wing_a", "baby_phoenix", wing_a)
    write_extra_model("baby_phoenix_wing_b", "baby_phoenix", wing_b)


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
    mini_dragon()
    ghost()
    penguin()
    kitten()
    bee()
    mushroom()
    wizard_robe("red", (172, 32, 38, 255), (130, 20, 26, 255))
    wizard_robe("green", (38, 124, 62, 255), (26, 92, 44, 255))
    wizard_robe("blue", (44, 84, 176, 255), (30, 58, 130, 255))
    wizard_robe("yellow", (220, 174, 44, 255), (176, 132, 26, 255))
    talking_hat()
    halo()
    owl()
    emojis()
    crate()
    wings("angel_wings", "angel")
    wings("demon_wings", "demon")
    wings("butterfly_wings", "butterfly")
    wings("dragon_wings", "dragon")
    wings("fairy_wings", "pixie")
    new_hats()
    fox()
    axolotl()
    panda()
    slime()
    baby_phoenix()
    print("Assets erzeugt in", ROOT)
