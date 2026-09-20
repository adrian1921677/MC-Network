"""
Modellier-Baukasten für die 20 Element-Welten.

Jede Welt (tools/worlds_*.py) beschreibt ihre 6 Cosmetics mit eigenen Formen. Dieser Baukasten
liefert die Bausteine (Kugeln, Scheiben, Ketten, Flammen, Federklingen ...), die animierten
Texturen (aus themes.py) und schreibt die Modelle.

Texturfelder (je 16x16 Pixel in einer 64x64-Textur):
    0 Hauptmuster   1 Nebenmuster   2 Gesicht   3 Leuchten (pulsiert)
    4 Dunkel        5 Gesicht schlafend          6 Hell
    7-15 Zusatzfarben der jeweiligen Welt (siehe "extras")
"""
import json
import math
from pathlib import Path

import themes as T

G = {}
ANIM = {"frametime": 3, "interpolate": True}

MAIN, SECOND, FACE, GLOW, DARK, SLEEP, LIGHT = range(7)
X1, X2, X3, X4, X5 = 7, 8, 9, 10, 11   # Zusatzfarben

# Bereiche in Cape-Texturen (für Zusatzteile an Umhängen)
CAPE_OUTER = [0, 0, 10, 16]
CAPE_INNER = [10, 0, 15, 16]
CAPE_EDGE = [15, 0, 16, 16]


def setup(namespace):
    G.update(namespace)
    T.setup(namespace)


# ---------------------------------------------------------------- Texturen
def frames_for(theme, extras=()):
    """Animierte 64x64-Textur: Welt-Muster in den Feldern 0-6, Zusatzfarben ab Feld 7."""
    frames = T.template_frames(theme, face=True)
    for f, img in enumerate(frames):
        t = f / T.FRAMES
        for k, spec in enumerate(extras):
            index = 7 + k
            x0, y0 = (index % 4) * 16, (index // 4) * 16
            color, mode = spec if isinstance(spec, tuple) and len(spec) == 2 and isinstance(spec[1], str) else (spec, "solid")
            for y in range(16):
                for x in range(16):
                    if mode in ("face", "sleep"):
                        col = T.mix(color, (0, 0, 0, 255), y / 15 * 0.12)
                    elif mode == "glow":
                        col = T.mix(color, (255, 255, 255, 255), 0.2 + 0.25 * math.sin(t * T.TAU + x * 0.35))
                    elif mode == "metal":
                        shine = abs(x - y * 0.6 - 3 - t * 10) < 1.6
                        col = T.mix(color, (255, 255, 255, 255), 0.45 if shine else 0.0)
                        col = T.mix(col, (0, 0, 0, 255), y / 15 * 0.18)
                    else:
                        col = T.mix(color, (0, 0, 0, 255), y / 15 * 0.2)
                    img[y0 + y][x0 + x] = col[:3] + ((color[3] if len(color) > 3 else 255),)
            T.shade_edges(img, x0, y0, 16, 16)
            eye = (28, 24, 40, 255) if T.luminance(color) > 0.35 else (240, 240, 255, 255)
            if mode == "face":
                G["big_eyes"](img, x0, y0, eye_y=5, left=3, right=9, w=4, h=5, color=eye)
            elif mode == "sleep":
                for ex in (3, 9):
                    for i in range(4):
                        img[y0 + 8 - (1 if i in (1, 2) else 0)][x0 + ex + i] = eye
    return frames


def gradient_name(theme_id, text, bold=False):
    """Name mit Farbverlauf in den Farben der Welt."""
    theme = next(t for t in T.THEMES if t["id"] == theme_id)
    colors = ["#%02X%02X%02X" % theme["colors"][i][:3] for i in (1, 2, 3)]
    inner = f"<b>{text}</b>" if bold else text
    return f"<gradient:{colors[0]}:{colors[1]}:{colors[2]}>{inner}</gradient>"


def texture(frames):
    return [row for frame in frames for row in frame]


def hexc(value, alpha=255):
    return T.rgba(value, alpha)


# ---------------------------------------------------------------- Bausteine
def c(i):
    return G["uv"](i)


def box(name, a, b, cell, **faces):
    """Ein Quader von a nach b. faces: einzelne Seiten mit anderem Feld, z. B. north=FACE."""
    lo = [round(min(a[k], b[k]), 3) for k in range(3)]
    hi = [round(max(a[k], b[k]), 3) for k in range(3)]
    for k in range(3):
        if hi[k] - lo[k] < 0.05:
            hi[k] = round(lo[k] + 0.05, 3)
        lo[k] = max(-16, lo[k])
        hi[k] = min(32, hi[k])
    element = G["cube"](name, lo, hi, c(cell))
    for face, other in faces.items():
        element["faces"][face]["uv"] = c(other)
    return element


def rot(element, angle, axis="y", origin=(8, 8, 8)):
    element["rotation"] = {"angle": angle, "axis": axis, "origin": list(origin)}
    return element


def centered(name, cx, cy, cz, sx, sy, sz, cell, **faces):
    return box(name, [cx - sx / 2, cy - sy / 2, cz - sz / 2], [cx + sx / 2, cy + sy / 2, cz + sz / 2], cell, **faces)


def stack(name, cx, cz, y, layers, cell, tip=None, lean=(0.0, 0.0)):
    """Gestapelte Quader (Flammen, Türme, Kegel). layers: (höhe, halbe breite[, halbe tiefe]).
    lean: pro Schicht seitlich versetzen (x, z), z. B. für schräge Flammenspitzen."""
    out = []
    for i, layer in enumerate(layers):
        height, hw = layer[0], layer[1]
        hd = layer[2] if len(layer) > 2 else hw
        ox, oz = cx + lean[0] * i, cz + lean[1] * i
        cell_here = tip if (tip is not None and i == len(layers) - 1) else cell
        out.append(box(name, [ox - hw, y, oz - hd], [ox + hw, y + height, oz + hd], cell_here))
        y += height
    return out


def chain(name, points, sizes, cell, tip=None):
    """Quader entlang von Punkten (Hörner, Tentakel, Geweihe). sizes: Zahl oder (x, y, z)."""
    out = []
    for i, (point, size) in enumerate(zip(points, sizes)):
        sx, sy, sz = size if isinstance(size, (tuple, list)) else (size, size, size)
        cell_here = tip if (tip is not None and i == len(points) - 1) else cell
        out.append(centered(name, point[0], point[1], point[2], sx, sy, sz, cell_here))
    return out


def densify(points, max_step):
    """Fügt Zwischenpunkte ein, damit eine Kette aus Quadern keine Lücken hat."""
    out = [points[0]]
    for a, b in zip(points, points[1:]):
        dist = math.dist(a, b)
        n = max(1, int(math.ceil(dist / max_step)))
        for i in range(1, n + 1):
            out.append(tuple(a[k] + (b[k] - a[k]) * i / n for k in range(3)))
    return out


def bezier(p0, p1, p2, n):
    """n Punkte auf einer weichen Kurve von p0 über (Richtung) p1 nach p2."""
    return [tuple((1 - t) ** 2 * p0[k] + 2 * (1 - t) * t * p1[k] + t * t * p2[k] for k in range(3))
            for t in [i / (n - 1) for i in range(n)]]


def taper(a, b, n):
    return [a + (b - a) * i / (n - 1) for i in range(n)]


def ball(name, cx, cy, cz, r, cell, step=1.0, squash=1.0, top_only=False, **faces):
    """Rundliche Kugel aus Schichten, jede Schicht als Achteck (zwei gekreuzte Quader).
    top_only: nur die obere Hälfte (Kuppel, Pilzhut)."""
    out = []
    y = 0.0 if top_only else -r
    while y < r - 1e-6:
        y2 = min(r, y + step)
        mid = (y + y2) / 2
        w = math.sqrt(max(0.0, r * r - mid * mid))
        if w > 0.25:
            out.append(box(name, [cx - w, cy + y * squash, cz - w * 0.72], [cx + w, cy + y2 * squash, cz + w * 0.72], cell, **faces))
            out.append(box(name, [cx - w * 0.72, cy + y * squash, cz - w], [cx + w * 0.72, cy + y2 * squash, cz + w], cell, **faces))
        y = y2
    return out


def disc(name, cx, cy, cz, r, thick, cell, axis="y", step=1.0):
    """Flache runde Scheibe. axis = Richtung, in die die Scheibe zeigt (y = liegend, z = stehend)."""
    out = []
    i = -r
    while i < r - 1e-6:
        j = min(r, i + step)
        half = math.sqrt(max(0.0, r * r - ((i + j) / 2) ** 2))
        if half > 0.2:
            if axis == "y":
                out.append(box(name, [cx - half, cy - thick / 2, cz + i], [cx + half, cy + thick / 2, cz + j], cell))
            elif axis == "z":
                out.append(box(name, [cx - half, cy + i, cz - thick / 2], [cx + half, cy + j, cz + thick / 2], cell))
            else:
                out.append(box(name, [cx - thick / 2, cy + i, cz - half], [cx + thick / 2, cy + j, cz + half], cell))
        i = j
    return out


def ring(name, cx, cy, cz, radius, thick, cell, count=12, size=None, axis="y"):
    """Ring aus kleinen Quadern auf einem Kreis."""
    size = size or thick
    out = []
    for i in range(count):
        a = math.tau * i / count
        if axis == "y":
            out.append(centered(name, cx + math.cos(a) * radius, cy, cz + math.sin(a) * radius, size, thick, size, cell))
        else:  # stehender Ring (in der xy-Ebene)
            out.append(centered(name, cx + math.cos(a) * radius, cy + math.sin(a) * radius, cz, size, size, thick, cell))
    return out


def around(count, radius, cx=8, cz=8, phase=0.0):
    """Positionen gleichmäßig auf einem Kreis: [(x, z, winkel_in_grad, index)]."""
    return [(cx + math.cos(phase + math.tau * i / count) * radius,
             cz + math.sin(phase + math.tau * i / count) * radius,
             math.degrees(phase + math.tau * i / count), i) for i in range(count)]


def blade(name, root, angle, length, width, cell, tip=None, thick=0.8, taper_to=0.45, z=8.0, step=None):
    """Federklinge in der Flügelebene (xy): von root in Richtung angle (0 = nach außen, 90 = nach oben)."""
    # Schritt so klein, dass sich auch die schmale Spitze noch lückenlos überlappt
    step = step or max(0.3, width * taper_to * 0.6)
    n = max(2, int(length / step) + 1)
    rad = math.radians(angle)
    out = []
    for i in range(n):
        d = length * i / (n - 1)
        w = width * (1 - (1 - taper_to) * i / (n - 1))
        cell_here = tip if (tip is not None and i == n - 1) else cell
        out.append(centered(name, root[0] + math.cos(rad) * d, root[1] + math.sin(rad) * d, z, w, w, thick, cell_here))
    return out


def mirror(elements):
    return G["mirror_x"](elements)


def shift(elements, dx=0.0, dy=0.0, dz=0.0):
    return G["shift"](elements, dx, dy, dz)


# ---------------------------------------------------------------- Menü-Darstellung automatisch
# Minecraft lädt ein Modell nur, wenn alle Element-Koordinaten in -16..32 liegen.
# Alles darüber wird als fehlende Textur (schwarz/magenta) angezeigt.
COORD_MIN, COORD_MAX = -15.5, 31.5


def fit_factor(groups):
    """Faktor, um den ein Modell um (8,8,8) geschrumpft werden muss, damit es in den
    erlaubten Koordinatenbereich passt. 1.0 = passt bereits."""
    lo, hi = 1e9, -1e9
    for elements in groups:
        for e in elements:
            for k in range(3):
                lo = min(lo, e["from"][k], e["to"][k])
                hi = max(hi, e["from"][k], e["to"][k])
    factor = 1.0
    if lo < COORD_MIN:
        factor = min(factor, (COORD_MIN - 8) / (lo - 8))
    if hi > COORD_MAX:
        factor = min(factor, (COORD_MAX - 8) / (hi - 8))
    return round(factor, 4)


def scale_about(elements, factor, origin=8.0):
    """Skaliert Elemente (samt Rotationsursprung) um den Modellmittelpunkt."""
    for e in elements:
        for key in ("from", "to"):
            e[key] = [round(origin + (v - origin) * factor, 4) for v in e[key]]
        if "rotation" in e:
            e["rotation"]["origin"] = [round(origin + (v - origin) * factor, 4) for v in e["rotation"]["origin"]]
    return elements


def auto_display(elements, kind):
    lo = [min(min(e["from"][k], e["to"][k]) for e in elements) for k in range(3)]
    hi = [max(max(e["from"][k], e["to"][k]) for e in elements) for k in range(3)]
    center = [(lo[k] + hi[k]) / 2 for k in range(3)]
    extent = max(hi[k] - lo[k] for k in range(3))
    scale = round(min(1.0, 15.0 / max(extent, 1.0)), 3)
    rotation = {"head": [30, 225, 0], "halo": [25, 20, 0], "back": [15, 200, 0], "wings": [10, 180, 0],
                "pet": [30, 225, 0]}[kind]
    def clamp(v):
        return round(max(-80.0, min(80.0, v)), 2)
    translation = [clamp(-(center[0] - 8) * scale * 0.7), clamp(-(center[1] - 8) * scale * 0.9), 0]
    display = {
        "gui": {"rotation": rotation, "translation": translation, "scale": [scale] * 3},
        "ground": {"translation": [0, 2, 0], "scale": [round(scale * 0.5, 3)] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": translation, "scale": [round(scale * 0.9, 3)] * 3},
        "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [round(scale * 0.4, 3)] * 3},
    }
    if kind == "head":
        display["head"] = {}
    return display


# ---------------------------------------------------------------- Schreiben
def write_single(cid, elements, frames, kind):
    G["write_cosmetic"](cid, elements, texture(frames), display_settings=auto_display(elements, kind), animation=ANIM)


def write_parts(cid, parts, icon, frames, kind):
    G["write_cosmetic"](cid, icon, texture(frames), display_settings=auto_display(icon, kind), animation=ANIM)
    for suffix, elements in parts.items():
        G["write_extra_model"](f"{cid}_{suffix}", cid, elements)


def write_cape(cid, theme, top=(), bottom=(), robe=False):
    frames = T.cape_frames(theme)
    segments = G["ROBE_SEGMENTS"] if robe else G["CAPE_SEGMENTS"]
    x0, x1 = (2, 14) if robe else (3, 13)
    icon = [G["cube"]("Umhang", [x0, 16 - sum(segments), 7.5], [x1, 16, 8.5], CAPE_OUTER)]
    icon[0]["faces"]["north"]["uv"] = CAPE_INNER
    G["write_cosmetic"](cid, icon + list(top), texture(frames), display_settings=G["CAPE_DISPLAY"], animation=ANIM)
    G["write_cape_segments"](cid, segments, x0=x0, x1=x1, top_extras=list(top), bottom_extras=list(bottom))


def cape_box(name, a, b, uv_rect):
    """Zusatzteil für Umhänge: nutzt einen Bereich der Cape-Textur statt eines Paletten-Feldes."""
    element = box(name, a, b, 0)
    for face in element["faces"].values():
        face["uv"] = list(uv_rect)
    return element


# ---------------------------------------------------------------- Alles erzeugen
def generate_all(out_json):
    import importlib
    modules = []
    for name in ("worlds_a", "worlds_b", "worlds_c", "worlds_d"):
        try:
            modules.append(importlib.import_module(name))
        except ModuleNotFoundError:
            print("  (noch nicht vorhanden:", name + ")")
    themes_by_id = {theme["id"]: theme for theme in T.THEMES}
    entries = []
    shrunk = []
    for module in modules:
        for item in module.ITEMS:
            theme = themes_by_id[item["theme"]]
            spec = item["build"]()
            kind = spec["kind"]
            scale = item.get("scale", 1.0)
            if kind == "cape":
                write_cape(item["id"], theme, spec.get("top", ()), spec.get("bottom", ()), spec.get("robe", False))
            else:
                frames = frames_for(theme, item.get("extras", ()))
                groups = (list(spec["parts"].values()) + [spec["icon"]]) if "parts" in spec else [spec["elements"]]
                factor = fit_factor(groups)
                if factor < 1.0:
                    # zu groß für Minecraft: Geometrie schrumpfen und über die Cosmetic-Größe ausgleichen
                    for group in groups:
                        scale_about(group, factor)
                    scale = round(scale / factor, 3)
                    shrunk.append((item["id"], factor))
                if "parts" in spec:
                    write_parts(item["id"], spec["parts"], spec["icon"], frames, kind)
                else:
                    write_single(item["id"], spec["elements"], frames, kind)
            entries.append(dict(id=item["id"], slot=item["slot"], type=item["type"], glowing=item.get("glowing", True),
                                rarity=item["rarity"], de=item["de"], en=item["en"], scale=scale,
                                particle=item.get("particle", theme["particle"])))
    Path(out_json).write_text(json.dumps(entries, indent=1, ensure_ascii=False), encoding="utf-8")
    for cid, factor in shrunk:
        print(f"  passend geschrumpft: {cid} (x{factor})")
    return entries
