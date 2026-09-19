"""
Themen-Maschine: erzeugt für 20 Element-Welten je ein komplettes Cosmetic-Set
(2 Kopf-Cosmetics, 2 Rückenteile, 2 Haustiere) mit animierten Texturen.

Wird von generate_assets.py aufgerufen. Schreibt zusätzlich tools/themed_cosmetics.json,
aus der die Standard-Listen (cosmetics_de.yml / cosmetics_en.yml) ergänzt werden.
"""
import colorsys
import json
import math
from pathlib import Path

# Diese Namen kommen aus generate_assets.py (siehe setup())
G = {}

FRAMES = 8
TAU = math.tau


def setup(namespace):
    G.update(namespace)


# ---------------------------------------------------------------- Hilfen
def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (a[3] if len(a) > 3 else 255,)


def rgba(hex_color, alpha=255):
    return ((hex_color >> 16) & 255, (hex_color >> 8) & 255, hex_color & 255, alpha)


def hash_noise(x, y, seed=0):
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def value_noise(u, v, scale, seed):
    x, y = u * scale, v * scale
    x0, y0 = int(math.floor(x)), int(math.floor(y))
    fx, fy = x - x0, y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = hash_noise(x0, y0, seed), hash_noise(x0 + 1, y0, seed)
    c, d = hash_noise(x0, y0 + 1, seed), hash_noise(x0 + 1, y0 + 1, seed)
    return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy


def luminance(color):
    return (0.3 * color[0] + 0.59 * color[1] + 0.11 * color[2]) / 255


# ---------------------------------------------------------------- Muster
# Jedes Muster liefert eine Farbe für (u, v) im Bereich 0..1 und die Animationszeit t (0..1).
def pattern_color(theme, u, v, t, layer=0):
    dark, mid, light, accent = theme["colors"]
    kind = theme["pattern"]
    s = layer * 17 + 3

    if kind == "flame":
        heat = 1 - v * 0.85 + 0.18 * math.sin(TAU * (t + u * 1.7 + v * 0.6)) + 0.12 * (value_noise(u, v - t, 6, s) - 0.5)
        return mix(dark, mid, heat * 1.6) if heat < 0.62 else mix(mid, light, (heat - 0.62) * 2.6)
    if kind == "ice":
        facet = (int(u * 5 + v * 3) + int(v * 4 - u * 2)) % 3
        base = [mid, light, mix(mid, dark, 0.4)][facet]
        shine = abs(((u + v) / 2 - t + 1) % 1 - 0.5) < 0.05
        return mix(base, (255, 255, 255, 255), 0.75) if shine else base
    if kind == "storm":
        base = mix(dark, mid, value_noise(u, v + t * 0.3, 4, s) * 0.8)
        bolt_x = 0.5 + 0.28 * math.sin(v * 9 + int(t * 4) * 1.7)
        if abs(u - bolt_x) < 0.035 + 0.02 * math.sin(v * 20):
            return light if (int(t * 8) % 3) != 1 else accent
        if t < 0.15:
            return mix(base, light, 0.35)  # Blitz-Aufleuchten
        return base
    if kind == "leaf":
        n = value_noise(u, v, 5, s)
        base = mix(dark, mid, n * 1.3)
        if abs((u * 6 + v * 3) % 1 - 0.5) < 0.06:
            base = mix(base, light, 0.5)  # Blattadern
        if hash_noise(int(u * 16), int(v * 16), s) > 0.93:
            return mix(accent, light, 0.5 + 0.5 * math.sin(TAU * t + u * 9))  # Blüten/Glühwürmchen
        return base
    if kind == "smoke":
        w = math.sin(u * 6 + math.sin(v * 5 + t * TAU) * 2 + t * TAU)
        base = mix(dark, mid, 0.35 + 0.35 * w)
        return mix(base, accent, 0.5) if value_noise(u + t, v, 7, s) > 0.78 else base
    if kind == "radiance":
        angle = math.atan2(v - 0.5, u - 0.5)
        ray = 0.5 + 0.5 * math.sin(angle * 8 + t * TAU)
        dist = math.hypot(u - 0.5, v - 0.5)
        return mix(mix(mid, light, ray), accent, max(0.0, 0.5 - dist))
    if kind == "wave":
        w = math.sin(v * 12 + math.sin(u * 5 + t * TAU) * 1.6 - t * TAU)
        base = mix(dark, mid, 0.5 + 0.5 * w)
        return mix(base, light, 0.8) if w > 0.9 else base
    if kind == "crystal":
        cx, cy = int(u * 5), int(v * 5)
        shade = hash_noise(cx, cy, s)
        base = mix(dark, light, shade * 0.9)
        sparkle = hash_noise(int(u * 16), int(v * 16), s + 5)
        if sparkle > 0.9 and math.sin(TAU * t + sparkle * 40) > 0.4:
            return (255, 255, 255, 255)
        return mix(base, mid, 0.3)
    if kind == "neon":
        pulse = 0.5 + 0.5 * math.sin(t * TAU)
        gu, gv = (u * 6) % 1, (v * 6 + t) % 1
        if gu < 0.12 or gv < 0.12:
            return mix(mid, light, pulse) if (int(u * 6) + int(v * 6)) % 2 else mix(accent, light, pulse)
        return mix(dark, mid, 0.15)
    if kind == "candy":
        stripe = int((u + v) * 6 + t * 2) % 3
        return [mid, light, accent][stripe]
    if kind == "galaxy":
        neb = value_noise(u, v, 3, s)
        base = mix(dark, mid, neb ** 1.5 * 1.4)
        base = mix(base, accent, max(0.0, value_noise(u, v, 5, s + 2) - 0.6) * 1.5)
        star = hash_noise(int(u * 20), int(v * 20), s + 9)
        if star > 0.9:
            return mix(base, (255, 255, 255, 255), 0.5 + 0.5 * math.sin(TAU * t + star * 50))
        return base
    if kind == "blood":
        base = mix(dark, mid, value_noise(u, v, 4, s))
        col = int(u * 8)
        drip = (hash_noise(col, 0, s) + t) % 1
        if abs(v - drip) < 0.12 and hash_noise(col, 1, s) > 0.4:
            return mix(mid, light, 1 - abs(v - drip) / 0.12)
        return base
    if kind == "gold":
        base = mix(mid, light, 0.5 + 0.4 * math.sin(v * 6))
        if abs(((u - v) * 0.5 - t + 1) % 1 - 0.5) < 0.06:
            base = mix(base, (255, 255, 240, 255), 0.7)  # Glanz
        if hash_noise(int(u * 8), int(v * 8), s) > 0.9:
            return accent
        return base
    if kind == "petal":
        base = mix(mid, light, value_noise(u, v, 3, s) * 0.8)
        for i in range(5):
            px = hash_noise(i, 3, s)
            py = (hash_noise(i, 4, s) + t) % 1
            if (u - px) ** 2 * 3 + (v - py) ** 2 < 0.004:
                return accent
        return base
    if kind == "magma":
        cell = value_noise(u, v, 5, s)
        crack = abs(cell - 0.5) < 0.06
        glow = 0.5 + 0.5 * math.sin(t * TAU + u * 3)
        return mix(mid, light, glow) if crack else mix(dark, mix(dark, mid, 0.3), value_noise(u, v, 9, s))
    if kind == "toxic":
        base = mix(dark, mid, 0.4 + 0.6 * value_noise(u, v - t * 0.5, 4, s))
        for i in range(4):
            bx = hash_noise(i, 7, s)
            by = 1 - (hash_noise(i, 8, s) + t) % 1
            if (u - bx) ** 2 + (v - by) ** 2 < 0.006:
                return light
        return base
    if kind == "rainbow":
        r, g, b = colorsys.hsv_to_rgb((u * 0.5 + v * 0.3 + t) % 1, 0.75, 1.0)
        return (int(r * 255), int(g * 255), int(b * 255), 255)
    if kind == "void":
        angle = math.atan2(v - 0.5, u - 0.5)
        dist = math.hypot(u - 0.5, v - 0.5)
        spiral = math.sin(angle * 3 + dist * 18 - t * TAU)
        base = mix(dark, mid, max(0.0, spiral) * 0.8)
        if hash_noise(int(u * 18), int(v * 18), s) > 0.94:
            return accent
        return base
    if kind == "brass":
        base = mix(dark, mid, 0.5 + 0.4 * math.sin(v * 8 + u * 2))
        gx, gy = (u * 3) % 1 - 0.5, (v * 3) % 1 - 0.5
        r = math.hypot(gx, gy)
        if 0.28 < r < 0.36 or (r < 0.36 and int(math.atan2(gy, gx) / TAU * 8 + t * 8) % 2 == 0 and r > 0.2):
            return mix(light, accent, 0.3)  # Zahnräder, die sich drehen
        return base
    if kind == "spooky":
        w = value_noise(u + math.sin(t * TAU) * 0.1, v - t * 0.4, 4, s)
        base = mix(dark, mid, w)
        if w > 0.72:
            return mix(accent, light, (w - 0.72) * 3)
        return base
    return mid


def shade_edges(img, x0, y0, w, h, amount=0.12):
    """Glanzkante oben/links und Schatten unten/rechts auf einem Feld."""
    for y in range(h):
        for x in range(w):
            r, g, b, a = img[y0 + y][x0 + x]
            if a == 0:
                continue
            f = 1.0
            if x == 0 or y == 0:
                f += amount
            if x == w - 1 or y == h - 1:
                f -= amount
            img[y0 + y][x0 + x] = (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), a)


# ---------------------------------------------------------------- Texturen für Vorlagen
# Paletten-Felder (je 16x16 Pixel in einer 64x64-Textur):
#   0 Hauptmuster, 1 Nebenmuster (dunkler), 2 Gesicht, 3 Akzent (leuchtet, pulsiert),
#   4 Dunkel, 5 Gesicht mit geschlossenen Augen, 6 Hell
def template_frames(theme, face=True):
    dark, mid, light, accent = theme["colors"]
    frames = []
    eye_color = light if luminance(mid) < 0.35 else (28, 24, 40, 255)
    for f in range(FRAMES):
        t = f / FRAMES
        img = [[(0, 0, 0, 0)] * 64 for _ in range(64)]
        for index in range(16):
            x0, y0 = (index % 4) * 16, (index // 4) * 16
            for y in range(16):
                for x in range(16):
                    u, v = x / 15, y / 15
                    if index in (0, 2, 5):
                        color = pattern_color(theme, u, v, t, 0)
                    elif index == 1:
                        color = mix(pattern_color(theme, u, v, t, 1), dark, 0.35)
                    elif index == 3:
                        color = mix(accent, light, 0.5 + 0.5 * math.sin(t * TAU + u * 2))
                    elif index == 4:
                        color = mix(dark, (0, 0, 0, 255), 0.25)
                    elif index == 6:
                        color = light
                    else:
                        color = mid
                    img[y0 + y][x0 + x] = color
            if index not in (3,):
                shade_edges(img, x0, y0, 16, 16)
        if face:
            G["big_eyes"](img, 32, 0, eye_y=5, left=3, right=9, w=4, h=5, color=eye_color)
            for ex in (3, 9):  # geschlossene Äuglein ^^
                for i in range(4):
                    img[16 + 8 - (1 if i in (1, 2) else 0)][16 + ex + i] = eye_color
        frames.append(img)
    return frames


def texture(frames):
    return [row for frame in frames for row in frame]


ANIM = {"frametime": 3, "interpolate": True}


# ---------------------------------------------------------------- Kopf-Vorlagen
def floating_crown(cid, theme):
    uv, cube, json_ = G["uv"], G["cube"], json
    half = 2.4
    wall = cube("Wand", [8 - half, 6, 1.5], [8 + half, 9.5, 2.7], uv(0))
    rim = cube("Rand", [8 - half - 0.2, 9.5, 1.3], [8 + half + 0.2, 10.2, 2.9], uv(6))
    spike = cube("Zacke", [7.3, 10.2, 1.6], [8.7, 12.8, 2.6], uv(1))
    tip = cube("Spitze", [7.65, 12.8, 1.8], [8.35, 14, 2.4], uv(6))
    gem = cube("Juwel", [7.3, 14, 1.5], [8.7, 15.4, 2.7], uv(3))
    elements = []

    def quarter(element, turns):
        copy = json_.loads(json_.dumps(element))
        for _ in range(turns):
            (x0, y0, z0), (x1, y1, z1) = copy["from"], copy["to"]
            copy["from"], copy["to"] = [16 - z1, y0, x0], [16 - z0, y1, x1]
        return copy

    for turns in range(4):
        for part in (wall, rim, spike, tip, gem):
            elements.append(quarter(part, turns))
            diagonal = quarter(part, turns)
            diagonal["rotation"] = {"angle": 45, "axis": "y", "origin": [8, 8, 8]}
            elements.append(diagonal)
    elements.append(cube("Kern", [7, 9, 7], [9, 11, 9], uv(3)))
    display = {"gui": {"rotation": [25, 20, 0], "translation": [0, -1, 0], "scale": [0.85] * 3},
               "ground": {"translation": [0, 2, 0], "scale": [0.5] * 3}, "fixed": {"scale": [0.8] * 3}}
    G["write_cosmetic"](cid, elements, texture(template_frames(theme, face=False)), display_settings=display, animation=ANIM)


def orbit_ring(cid, theme):
    uv, cube = G["uv"], G["cube"]
    elements = []
    for i in range(8):
        angle = i * 45
        # Splitter liegen auf einem Kreis und sind abwechselnd höher/tiefer
        rad = math.radians(angle)
        cx, cz = 8 + math.cos(rad) * 6.5, 8 + math.sin(rad) * 6.5
        lift = 1.2 if i % 2 else 0
        shard = cube("Splitter", [cx - 0.7, 7 + lift, cz - 0.7], [cx + 0.7, 10 + lift, cz + 0.7], uv(0))
        shard_tip = cube("Splitterspitze", [cx - 0.35, 10 + lift, cz - 0.35], [cx + 0.35, 11.2 + lift, cz + 0.35], uv(3))
        shard_bottom = cube("Splitterende", [cx - 0.35, 6.2 + lift, cz - 0.35], [cx + 0.35, 7 + lift, cz + 0.35], uv(3))
        elements += [shard, shard_tip, shard_bottom]
    ring_half = 2.1
    for turns, (fx, fz, tx, tz) in enumerate([(8 - ring_half, 3.6, 8 + ring_half, 4.2), (11.8, 8 - ring_half, 12.4, 8 + ring_half),
                                              (8 - ring_half, 11.8, 8 + ring_half, 12.4), (3.6, 8 - ring_half, 4.2, 8 + ring_half)]):
        bar = cube("Ring", [fx, 8.2, fz], [tx, 8.8, tz], uv(6))
        elements.append(bar)
        diagonal = json.loads(json.dumps(bar))
        diagonal["rotation"] = {"angle": 45, "axis": "y", "origin": [8, 8, 8]}
        elements.append(diagonal)
    display = {"gui": {"rotation": [30, 0, 0], "scale": [0.8] * 3},
               "ground": {"translation": [0, 2, 0], "scale": [0.5] * 3}, "fixed": {"scale": [0.8] * 3}}
    G["write_cosmetic"](cid, elements, texture(template_frames(theme, face=False)), display_settings=display, animation=ANIM)


def horns(cid, theme):
    uv, cube, mirror_x, t = G["uv"], G["cube"], G["mirror_x"], G["HEAD_TOP"]
    path = [(3.2, t - 0.2, 6.2, 2.4), (2.4, t + 1.4, 6.6, 2.1), (1.6, t + 2.9, 7.3, 1.8),
            (1.2, t + 4.3, 8.3, 1.5), (1.4, t + 5.5, 9.5, 1.2), (2.0, t + 6.4, 10.6, 0.9)]
    left = []
    for i, (x, y, z, size) in enumerate(path):
        left.append(cube("Horn", [x - size / 2, y, z - size / 2], [x + size / 2, y + 1.6, z + size / 2],
                         uv(3) if i == len(path) - 1 else uv(0 if i % 2 == 0 else 1)))
    band = [G["cube"]("Haarreif", [1.4, t - 0.6, 5.6], [14.6, t + 0.4, 7], uv(4))]
    elements = band + left + mirror_x(left)
    G["write_cosmetic"](cid, elements, texture(template_frames(theme, face=False)), center_y=t + 3, gui_scale=0.6, animation=ANIM)


# ---------------------------------------------------------------- Rücken-Vorlagen
def themed_wings(cid, theme, shape):
    base = G["wing_sprite"](shape)
    res = len(base)
    dark = theme["colors"][0]
    frames = []
    for f in range(FRAMES):
        t = f / FRAMES
        img = [[(0, 0, 0, 0)] * res for _ in range(res)]
        for y in range(res):
            for x in range(res):
                a = base[y][x][3]
                if a == 0:
                    continue
                color = pattern_color(theme, x / res, y / res, t, 2)
                # Struktur der Vorlage (Federlinien, Knochen, Rand) als Helligkeit übernehmen
                l = luminance(base[y][x])
                color = mix(color, dark, max(0.0, 0.55 - l) * 0.9)
                img[y][x] = color[:3] + (a,)
        frames.append(img)
    G["wings"](cid, shape, frames=frames, animation=ANIM)


def themed_cape(cid, theme):
    dark, mid, light, accent = theme["colors"]
    frames = []
    for f in range(FRAMES):
        t = f / FRAMES
        img = [[dark] * 32 for _ in range(32)]
        for y in range(32):
            for x in range(20):
                img[y][x] = pattern_color(theme, x / 19, y / 31, t, 3)
        edge = mix(light, accent, 0.5 + 0.5 * math.sin(t * TAU))
        for y in range(32):
            img[y][0] = img[y][19] = edge
            for x in range(20, 30):
                img[y][x] = mix(pattern_color(theme, (x - 20) / 9, y / 31, t, 4), dark, 0.6)
            img[y][30] = img[y][31] = edge
        for x in range(20):
            img[31][x] = edge
        frames.append(img)
    G["write_cosmetic"](cid, G["cape_element"](), texture(frames), display_settings=G["CAPE_DISPLAY"], animation=ANIM)
    G["write_cape_segments"](cid)


# ---------------------------------------------------------------- Haustier-Vorlagen
# Jede Vorlage nutzt die Teile-Namen einer vorhandenen Animation.
def pet_drake(cid, theme):  # Animation DRAGON
    uv, cube, faces, shift = G["uv"], G["cube"], G["faces"], G["shift"]
    body = [
        faces(cube("Körper", [5, 5, 6], [11, 10, 12], uv(0)), down=uv(1)),
        cube("Bauch", [5.5, 4.5, 6.5], [10.5, 5, 11.5], uv(1)),
        faces(cube("Kopf", [5.5, 8, 1.5], [10.5, 12, 6], uv(0)), north=uv(2)),
        cube("Schnauze", [6.5, 8, 0], [9.5, 10, 1.5], uv(1)),
        cube("Horn links", [6, 12, 4], [7, 14.5, 5], uv(6)),
        cube("Horn rechts", [9, 12, 4], [10, 14.5, 5], uv(6)),
        cube("Stachel 1", [7.5, 10, 7], [8.5, 11.2, 8], uv(3)),
        cube("Stachel 2", [7.5, 10, 9.5], [8.5, 11.2, 10.5], uv(3)),
        cube("Schwanz", [7, 6, 12], [9, 8, 15], uv(0)),
        cube("Schwanzspitze", [7.25, 6.25, 15], [8.75, 7.75, 17.5], uv(3)),
    ]
    for x, z in ((5.5, 7), (9, 7), (5.5, 10), (9, 10)):
        body.append(cube("Bein", [x, 3, z], [x + 1.5, 5, z + 1.5], uv(4)))
    wing_a = [cube("Knochen", [8, 8, 7.5], [16, 9, 8.5], uv(4)), cube("Flughaut", [8, 8.25, 8.5], [15, 8.75, 13], uv(1))]
    wing_b = [cube("Knochen", [0, 8, 7.5], [8, 9, 8.5], uv(4)), cube("Flughaut", [1, 8.25, 8.5], [8, 8.75, 13], uv(1))]
    tex = texture(template_frames(theme))
    G["write_cosmetic"](cid, body + shift(wing_a, 3, 2, 0.5) + shift(wing_b, -3, 2, 0.5), tex,
                        display_settings=G["PET_DISPLAY"], animation=ANIM)
    for suffix, parts in (("body", body), ("wing_a", wing_a), ("wing_b", wing_b)):
        G["write_extra_model"](f"{cid}_{suffix}", cid, parts)


def pet_spirit(cid, theme):  # Animation GHOST
    uv, cube, faces = G["uv"], G["cube"], G["faces"]
    elements = [
        faces(cube("Körper", [4, 6, 4], [12, 14, 12], uv(0)), north=uv(2)),
        cube("Saum", [4, 4, 4], [12, 6, 12], uv(1)),
        cube("Arm links", [2.5, 8, 7], [4, 10, 9], uv(0)),
        cube("Arm rechts", [12, 8, 7], [13.5, 10, 9], uv(0)),
        cube("Flamme", [7, 14, 7], [9, 15.8, 9], uv(3)),
        cube("Flammenspitze", [7.5, 15.8, 7.5], [8.5, 17, 8.5], uv(6)),
    ]
    for x in (4, 7, 10):
        for z in (4, 7, 10):
            if (x, z) != (7, 7):
                elements.append(cube("Zacke", [x, 3, z], [x + 2, 4, z + 2], uv(1)))
    G["write_cosmetic"](cid, elements, texture(template_frames(theme)), display_settings=G["PET_DISPLAY"], animation=ANIM)


def pet_critter(cid, theme):  # Animation KITTEN (Schulter)
    uv, cube, faces, shift = G["uv"], G["cube"], G["faces"], G["shift"]
    body = [
        faces(cube("Körper", [5.5, 4, 7], [10.5, 8, 11], uv(0)), north=uv(6)),
        faces(cube("Kopf", [4.5, 8, 5], [11.5, 12.5, 10.5], uv(0)), north=uv(2)),
        cube("Schnauze", [6.5, 8, 3.8], [9.5, 9.8, 5], uv(6)),
        faces(cube("Ohr links", [4.5, 12.5, 6.5], [6.75, 15, 7.5], uv(0)), north=uv(3)),
        faces(cube("Ohr rechts", [9.25, 12.5, 6.5], [11.5, 15, 7.5], uv(0)), north=uv(3)),
        cube("Pfote links", [6, 4, 6], [7.5, 5, 7], uv(4)),
        cube("Pfote rechts", [8.5, 4, 6], [10, 5, 7], uv(4)),
    ]
    tail = [cube("Schwanz", [6.5, 7, 8], [9.5, 10.5, 14], uv(1)),
            cube("Schwanzspitze", [6.75, 7.25, 14], [9.25, 10.25, 16.5], uv(3))]
    sleep = [
        cube("Körper", [4.5, 4, 6.5], [11.5, 8, 11.5], uv(0)),
        faces(cube("Kopf", [5, 4, 2.5], [11, 8.5, 7], uv(0)), north=uv(5)),
        faces(cube("Ohr links", [5, 8.5, 4], [7, 10.5, 5], uv(0)), north=uv(3)),
        faces(cube("Ohr rechts", [9, 8.5, 4], [11, 10.5, 5], uv(0)), north=uv(3)),
        cube("Schwanz", [2.5, 4, 1.5], [5, 7, 11.5], uv(1)),
        cube("Schwanz vorne", [2.5, 4, 0], [9.5, 6.5, 2.5], uv(1)),
        cube("Schwanzspitze", [9.5, 4, 0], [11.5, 6.5, 2.5], uv(3)),
    ]
    G["write_cosmetic"](cid, body + shift(tail, 0, -3, 3), texture(template_frames(theme)),
                        display_settings=G["PET_DISPLAY"], animation=ANIM)
    for suffix, parts in (("body", body), ("tail", tail), ("sleep", sleep)):
        G["write_extra_model"](f"{cid}_{suffix}", cid, parts)


def pet_blob(cid, theme):  # Animation MUSHROOM (Hüpfen)
    uv, cube, faces = G["uv"], G["cube"], G["faces"]
    elements = [
        faces(cube("Hülle", [3, 1, 3], [13, 11, 13], uv(0)), north=uv(2)),
        cube("Krönchen", [6.5, 11, 6.5], [9.5, 12.5, 9.5], uv(3)),
        cube("Krönchen Spitze", [7.25, 12.5, 7.25], [8.75, 13.8, 8.75], uv(6)),
    ]
    G["write_cosmetic"](cid, elements, texture(template_frames(theme)), display_settings=G["PET_DISPLAY"], animation=ANIM)


def pet_wisp(cid, theme):  # Animation BEE (schwirrend)
    uv, cube, faces, shift = G["uv"], G["cube"], G["faces"], G["shift"]
    body = [
        faces(cube("Kugel", [5, 5, 5], [11, 11, 11], uv(0)), north=uv(2)),
        cube("Kern oben", [6.5, 11, 6.5], [9.5, 12, 9.5], uv(3)),
        cube("Schweif", [7, 6.5, 11], [9, 8.5, 14], uv(1)),
        cube("Schweifspitze", [7.5, 7, 14], [8.5, 8, 16], uv(3)),
    ]
    wing_a = [cube("Flügel", [8, 8, 7], [13.5, 8.4, 11], uv(3))]
    wing_b = [cube("Flügel", [2.5, 8, 7], [8, 8.4, 11], uv(3))]
    G["write_cosmetic"](cid, body + shift(wing_a, 2, 2, 0.5) + shift(wing_b, -2, 2, 0.5), texture(template_frames(theme)),
                        display_settings=G["PET_DISPLAY"], animation=ANIM)
    for suffix, parts in (("body", body), ("wing_a", wing_a), ("wing_b", wing_b)):
        G["write_extra_model"](f"{cid}_{suffix}", cid, parts)


def pet_owlet(cid, theme):  # Animation OWL (Schulter)
    uv, cube, faces, shift = G["uv"], G["cube"], G["faces"], G["shift"]
    body = [
        faces(cube("Körper", [5, 4.5, 6], [11, 10, 11], uv(0)), north=uv(1)),
        cube("Fuß links", [6, 3.5, 5], [7.5, 4.5, 7], uv(6)),
        cube("Fuß rechts", [8.5, 3.5, 5], [10, 4.5, 7], uv(6)),
        cube("Schwanzfedern", [6.5, 4.5, 11], [9.5, 6, 12.5], uv(3)),
    ]
    head = [
        faces(cube("Kopf", [4.5, 8, 5], [11.5, 13, 10.5], uv(0)), north=uv(2)),
        cube("Schnabel", [7.5, 9, 4.5], [8.5, 10.5, 5], uv(6)),
        cube("Federohr links", [4.5, 13, 6.5], [6, 14.8, 7.5], uv(3)),
        cube("Federohr rechts", [10, 13, 6.5], [11.5, 14.8, 7.5], uv(3)),
    ]
    wing_a = [cube("Flügel", [8, 3, 6.5], [9, 8, 10.5], uv(1))]
    wing_b = [cube("Flügel", [7, 3, 6.5], [8, 8, 10.5], uv(1))]
    icon = body + shift(head, 0, 2, 0.25) + shift(wing_a, 3, 1.5, 0.5) + shift(wing_b, -3, 1.5, 0.5)
    G["write_cosmetic"](cid, icon, texture(template_frames(theme)), display_settings=G["PET_DISPLAY"], animation=ANIM)
    for suffix, parts in (("body", body), ("head", head), ("wing_a", wing_a), ("wing_b", wing_b)):
        G["write_extra_model"](f"{cid}_{suffix}", cid, parts)


# Vorlage -> (Funktion, Animation im Plugin, deutscher Name, englischer Name)
PETS = {
    "drake": (pet_drake, "DRAGON", "Drache", "Drake"),
    "spirit": (pet_spirit, "GHOST", "Geist", "Spirit"),
    "critter": (pet_critter, "KITTEN", "Fuchs", "Fox"),
    "blob": (pet_blob, "MUSHROOM", "Schleim", "Slime"),
    "wisp": (pet_wisp, "BEE", "Irrlicht", "Wisp"),
    "owlet": (pet_owlet, "OWL", "Eule", "Owl"),
}
PET_ORDER = list(PETS)

# ---------------------------------------------------------------- Die 20 Welten
# colors: dunkel, mittel, hell, Akzent
THEMES = [
    dict(id="inferno", de="Inferno", en="Inferno", pattern="flame", particle="FLAME", wings="angel",
         colors=(rgba(0x8C1A0A), rgba(0xFF6A10), rgba(0xFFE27A), rgba(0xFFFFFF))),
    dict(id="frost", de="Frost", en="Frost", pattern="ice", particle="SNOWFLAKE", wings="pixie",
         colors=(rgba(0x2A5A9A), rgba(0x8FD8FF), rgba(0xE8FAFF), rgba(0x5AB4FF))),
    dict(id="storm", de="Sturm", en="Storm", pattern="storm", particle="ELECTRIC_SPARK", wings="demon",
         colors=(rgba(0x10142E), rgba(0x2E3A78), rgba(0xE8F4FF), rgba(0x6AD8FF))),
    dict(id="forest", de="Wald", en="Forest", pattern="leaf", particle="FIREFLY", wings="butterfly",
         colors=(rgba(0x1E4A1E), rgba(0x4CA84C), rgba(0xB6F07A), rgba(0xFFE066))),
    dict(id="shadow", de="Schatten", en="Shadow", pattern="smoke", particle="SMOKE", wings="demon",
         colors=(rgba(0x08060E), rgba(0x3A2458), rgba(0x9A70D8), rgba(0xC090FF))),
    dict(id="heaven", de="Himmels", en="Heaven", pattern="radiance", particle="END_ROD", wings="angel",
         colors=(rgba(0xC8A850), rgba(0xFFF4D0), rgba(0xFFFFFF), rgba(0xFFD35A))),
    dict(id="ocean", de="Ozean", en="Ocean", pattern="wave", particle="DRIPPING_WATER", wings="pixie",
         colors=(rgba(0x0A2A5A), rgba(0x1E78C8), rgba(0x9AE8FF), rgba(0xFFFFFF))),
    dict(id="crystal", de="Kristall", en="Crystal", pattern="crystal", particle="WAX_OFF", wings="pixie",
         colors=(rgba(0x3A1A6A), rgba(0x9A5AE8), rgba(0xF0D8FF), rgba(0xFFFFFF))),
    dict(id="neon", de="Neon", en="Neon", pattern="neon", particle="GLOW", wings="butterfly",
         colors=(rgba(0x0A0A14), rgba(0xFF3CCB), rgba(0xFFFFFF), rgba(0x2CF0FF))),
    dict(id="candy", de="Zucker", en="Candy", pattern="candy", particle="HEART", wings="butterfly",
         colors=(rgba(0xC85A8A), rgba(0xFF9AC8), rgba(0xFFFFFF), rgba(0x8AF0C8))),
    dict(id="cosmos", de="Galaxie", en="Galaxy", pattern="galaxy", particle="END_ROD", wings="angel", ultra=True,
         colors=(rgba(0x0A0620), rgba(0x5A2AA0), rgba(0xFFFFFF), rgba(0x2AA8FF))),
    dict(id="bloodmoon", de="Blutmond", en="Blood Moon", pattern="blood", particle="DAMAGE_INDICATOR", wings="demon",
         colors=(rgba(0x1A0206), rgba(0x8A0A1A), rgba(0xFF3A4A), rgba(0x1A0206))),
    dict(id="regal", de="Königs", en="Royal", pattern="gold", particle="WAX_ON", wings="angel",
         colors=(rgba(0x8A5A0A), rgba(0xE8B43A), rgba(0xFFF0A0), rgba(0xE02A4A))),
    dict(id="sakura", de="Kirschblüten", en="Sakura", pattern="petal", particle="CHERRY_LEAVES", wings="butterfly",
         colors=(rgba(0xA84A78), rgba(0xFFB0D2), rgba(0xFFF0F6), rgba(0xFF6AA8))),
    dict(id="magma", de="Magma", en="Magma", pattern="magma", particle="LAVA", wings="demon",
         colors=(rgba(0x1A0E0A), rgba(0xFF5A0A), rgba(0xFFD24A), rgba(0xFF8A2A))),
    dict(id="toxic", de="Gift", en="Toxic", pattern="toxic", particle="NOXIOUS_GAS", wings="demon",
         colors=(rgba(0x0E2A0A), rgba(0x5AD21E), rgba(0xE6FF7A), rgba(0x2AFF6A))),
    dict(id="rainbow", de="Regenbogen", en="Rainbow", pattern="rainbow", particle="END_ROD", wings="butterfly", ultra=True,
         colors=(rgba(0x5A2A8A), rgba(0xFF6AA8), rgba(0xFFFFFF), rgba(0x6AE8FF))),
    dict(id="void", de="Leere", en="Void", pattern="void", particle="REVERSE_PORTAL", wings="demon", ultra=True,
         colors=(rgba(0x000004), rgba(0x4A0A8A), rgba(0xD070FF), rgba(0xFF3CF0))),
    dict(id="steampunk", de="Messing", en="Brass", pattern="brass", particle="WHITE_SMOKE", wings="demon",
         colors=(rgba(0x4A2A12), rgba(0xB8823A), rgba(0xF0C878), rgba(0x6AD8C8))),
    dict(id="spooky", de="Spuk", en="Spooky", pattern="spooky", particle="SCULK_SOUL", wings="demon",
         colors=(rgba(0x140A1E), rgba(0x4A2A6A), rgba(0x9AFFB8), rgba(0xFF8A2A))),
]


def generate_all(out_json):
    entries = []

    def name(theme, noun_de, noun_en, bold=False):
        light, accent = theme["colors"][2], theme["colors"][3]
        a = "#%02X%02X%02X" % light[:3]
        b = "#%02X%02X%02X" % accent[:3]
        mid = "#%02X%02X%02X" % theme["colors"][1][:3]
        fmt = lambda text: f"<gradient:{mid}:{a}:{b}>{'<b>' if bold else ''}{text}{'</b>' if bold else ''}</gradient>"
        return fmt(f"{theme['de']}-{noun_de}" if not theme['de'].endswith("s") else f"{theme['de']}{noun_de.lower()}"), fmt(f"{theme['en']} {noun_en}")

    for i, theme in enumerate(THEMES):
        tid = theme["id"]
        ultra = theme.get("ultra", False)
        part = theme["particle"]

        # Kopf 1: schwebende Krone
        floating_crown(f"{tid}_crown", theme)
        de, en = name(theme, "Krone", "Crown", ultra)
        entries.append(dict(id=f"{tid}_crown", slot="HEAD", type="HALO", glowing=True,
                            rarity="ULTRA" if ultra else "LEGENDARY", de=de, en=en, scale=1.3 if ultra else 1.1, particle=part))
        # Kopf 2: Hörner oder Splitter-Ring
        if i % 2 == 0:
            horns(f"{tid}_horns", theme)
            de, en = name(theme, "Hörner", "Horns")
            entries.append(dict(id=f"{tid}_horns", slot="HEAD", type="NONE", glowing=False, rarity="EPIC",
                                de=de, en=en, particle=part))
        else:
            orbit_ring(f"{tid}_orbit", theme)
            de, en = name(theme, "Splitterkranz", "Shard Halo")
            entries.append(dict(id=f"{tid}_orbit", slot="HEAD", type="HALO", glowing=True, rarity="EPIC",
                                de=de, en=en, scale=1.2, particle=part))
        # Rücken: Flügel + Umhang
        themed_wings(f"{tid}_wings", theme, theme["wings"])
        de, en = name(theme, "Schwingen", "Wings", ultra)
        entries.append(dict(id=f"{tid}_wings", slot="BACK", type="WINGS", glowing=True,
                            rarity="ULTRA" if ultra else "LEGENDARY", de=de, en=en, scale=1.45 if ultra else 1.15, particle=part))
        themed_cape(f"{tid}_cape", theme)
        de, en = name(theme, "Umhang", "Cape")
        entries.append(dict(id=f"{tid}_cape", slot="BACK", type="NONE", glowing=True, rarity="EPIC", de=de, en=en,
                            scale=1.05, particle=part))
        # Haustiere: zwei verschiedene Kreaturen pro Welt
        for n, template in enumerate((PET_ORDER[i % 6], PET_ORDER[(i + 3) % 6])):
            function, animation, noun_de, noun_en = PETS[template]
            cid = f"{tid}_{template}"
            function(cid, theme)
            big = ultra and n == 0
            de, en = name(theme, noun_de, noun_en, big)
            entries.append(dict(id=cid, slot="PET", type=animation, glowing=True,
                                rarity="ULTRA" if big else ("LEGENDARY" if n == 0 else "EPIC"), de=de, en=en,
                                scale=1.6 if big else 1.0, particle=part))

    Path(out_json).write_text(json.dumps(entries, indent=1, ensure_ascii=False), encoding="utf-8")
    return entries
