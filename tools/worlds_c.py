"""
Welten 11-15: Galaxie, Blutmond, Königs, Kirschblüten, Magma.
Konventionen siehe worlds_a.py.
"""
import math

from worldkit import (densify, MAIN, SECOND, FACE, GLOW, DARK, SLEEP, LIGHT, X1, X2, X3, X4, X5,
                      box, centered, stack, chain, bezier, taper, ball, disc, ring, around, blade,
                      mirror, shift, rot, cape_box, CAPE_EDGE, CAPE_OUTER, hexc, gradient_name as nm)

T_TOP = 14.4
ITEMS = []


def item(theme, cid, slot, typ, rarity, de, en, build, **extra):
    ITEMS.append(dict(theme=theme, id=cid, slot=slot, type=typ, rarity=rarity,
                      de=nm(theme, de, rarity == "ULTRA"), en=nm(theme, en, rarity == "ULTRA"), build=build, **extra))


def wings_spec(els):
    return dict(kind="wings", parts={"wing_a": els, "wing_b": mirror(els)}, icon=shift(els, 2) + shift(mirror(els), -2))


def orbit(name, cx, cy, cz, radius, tilt, count, size, cell, phase=0.0, squash=1.0):
    """Punkte auf einer gekippten Umlaufbahn (tilt in Grad: 0 = liegend, 90 = stehend)."""
    out = []
    t = math.radians(tilt)
    for i in range(count):
        a = phase + math.tau * i / count
        x = cx + math.cos(a) * radius
        y = cy + math.sin(a) * radius * math.sin(t) * squash
        z = cz + math.sin(a) * radius * math.cos(t)
        out.append(centered(name, x, y, z, size, size, size, cell))
    return out


def band(name, y0, y1, half, cell, cx=8.0, cz=8.0, thick=1.0):
    """Hohler viereckiger Reif um den Kopf (statt einer massiven Platte)."""
    return [box(name, [cx - half, y0, cz - half], [cx + half, y1, cz - half + thick], cell),
            box(name, [cx - half, y0, cz + half - thick], [cx + half, y1, cz + half], cell),
            box(name, [cx - half, y0, cz - half + thick], [cx - half + thick, y1, cz + half - thick], cell),
            box(name, [cx + half - thick, y0, cz - half + thick], [cx + half, y1, cz + half - thick], cell)]


def sail(root, fingers, cell, z=8.0, thick=0.5, sag=0.26, step=0.5):
    """Geschlossene Flughaut: Randkurve läuft durch die Fingerspitzen und hängt dazwischen durch.
    Wird zeilenweise als durchgehende Quader gefüllt, damit die Fläche glatt bleibt."""
    angles = [math.radians(a) for a, _ in fingers]
    lens = [l for _, l in fingers]
    reach = max(lens) + 1.0

    def boundary(theta):
        for i in range(len(angles) - 1):
            a0, a1 = angles[i], angles[i + 1]
            if min(a0, a1) <= theta <= max(a0, a1):
                u = (theta - a0) / (a1 - a0) if a1 != a0 else 0.0
                return (lens[i] + (lens[i + 1] - lens[i]) * u) * (1 - sag * math.sin(math.pi * u))
        return None

    out = []
    cols = int(2 * reach / step)
    for iy in range(int(2 * reach / step)):
        y = root[1] - reach + iy * step
        my = y + step / 2 - root[1]
        run = None
        for ix in range(cols + 2):
            x = root[0] - reach + ix * step
            mx = x - root[0]
            d = math.hypot(mx, my)
            limit = boundary(math.atan2(my, mx)) if d > 1e-6 else None
            inside = limit is not None and d <= limit
            if inside and run is None:
                run = x
            elif not inside and run is not None:
                if x - run > step:
                    # Zeilen leicht überlappen lassen, sonst liegen Deck- und Bodenfläche exakt aufeinander
                    out.append(box("Flughaut", [run, y, z - thick / 2], [x, y + step * 1.35, z + thick / 2], cell))
                run = None
    return out


def bowl(name, cx, cy, cz, r, cell, step=1.0, squash=1.0):
    """Untere Kugelhälfte (Kessel, Schale)."""
    out = []
    y = -r
    while y < -1e-6:
        y2 = min(0.0, y + step)
        w = math.sqrt(max(0.0, r * r - ((y + y2) / 2) ** 2))
        if w > 0.25:
            out.append(box(name, [cx - w, cy + y * squash, cz - w * 0.72], [cx + w, cy + y2 * squash, cz + w * 0.72], cell))
            out.append(box(name, [cx - w * 0.72, cy + y * squash, cz - w], [cx + w * 0.72, cy + y2 * squash, cz + w], cell))
        y = y2
    return out


def blossom(cx, cy, cz, r, cell, core, plane="xy", petals=5, phase=0.0):
    """Fünfblättrige Blüte. plane: xy = steht (Vorderansicht), xz = liegt."""
    out = []
    for i in range(petals):
        a = phase + math.tau * i / petals
        dx, dy = math.cos(a) * r, math.sin(a) * r
        for f, s in ((1.0, 1.15), (0.55, 0.9)):
            if plane == "xy":
                out.append(centered("Blütenblatt", cx + dx * f, cy + dy * f, cz, r * s, r * s, 0.5, cell))
            else:
                out.append(centered("Blütenblatt", cx + dx * f, cy, cz + dy * f, r * s, 0.5, r * s, cell))
    if plane == "xy":
        out.append(centered("Blütenmitte", cx, cy, cz - 0.2, r * 0.7, r * 0.7, 0.7, core))
    else:
        out.append(centered("Blütenmitte", cx, cy + 0.2, cz, r * 0.7, 0.7, r * 0.7, core))
    return out


# =========================================================================== GALAXIE
def cosmos_ringed_planet():
    els = ball("Planet", 8, 8, 8, 3.6, MAIN, step=0.8)
    for y, w in ((6.6, 3.2), (8.4, 3.5), (9.8, 2.9)):
        els.append(centered("Wolkenband", 8, y, 8, w * 2, 0.7, w * 1.5, SECOND))
    els += ball("Sturmauge", 5.4, 7.2, 6.4, 1.0, GLOW, step=0.5)
    # gekippter Staubring, innen dicht, außen dünn
    els += orbit("Ring innen", 8, 8, 8, 5.4, 16, 26, 1.3, SECOND)
    els += orbit("Ring außen", 8, 8, 8, 6.8, 16, 30, 0.9, LIGHT, phase=0.1)
    els += orbit("Ringstaub", 8, 8, 8, 7.8, 16, 24, 0.5, GLOW, phase=0.25)
    # zwei Monde auf eigener Bahn
    els += ball("Mond", 8 + 8.6, 10.5, 8, 1.3, LIGHT, step=0.6)
    els += ball("Mond klein", 8 - 7.6, 5.6, 9.5, 0.9, LIGHT, step=0.5)
    return dict(kind="halo", elements=els)


def cosmos_crescent():
    t = T_TOP
    els = band("Stirnreif", t - 2.2, t - 0.9, 7.4, X1)
    els += band("Reifkante", t - 0.9, t - 0.4, 7.0, GLOW)
    cx, cy = 8.0, t + 4.6
    for i in range(17):
        a = math.radians(105 + 150 * i / 16)          # Bogen von links oben nach links unten
        w = 2.2 * math.sin(math.pi * i / 16) ** 0.6 + 0.7
        els.append(centered("Mondsichel", cx + math.cos(a) * 4.6, cy + math.sin(a) * 4.6, 8, w, w, 1.1, X1))
    els += ball("Sichelspitze", cx + math.cos(math.radians(105)) * 4.6, cy + math.sin(math.radians(105)) * 4.6, 8, 0.9, GLOW, step=0.5)
    els += ball("Sichelspitze", cx + math.cos(math.radians(255)) * 4.6, cy + math.sin(math.radians(255)) * 4.6, 8, 0.9, GLOW, step=0.5)
    els.append(centered("Halterung", 8, t + 0.4, 8, 1.0, 1.6, 1.0, X1))
    for x, y, s in ((12.5, t + 7.5, 1.1), (3.5, t + 8.5, 0.8), (13.5, t + 2.5, 0.7), (2.5, t + 3.5, 0.9)):
        els.append(centered("Sternchen", x, y, 8, s, s * 2.4, s, GLOW))
        els.append(centered("Sternchen", x, y, 8, s * 2.4, s, s, GLOW))
    return dict(kind="head", elements=els)


def cosmos_comet_wings():
    els = ball("Kometenkern", 9.5, 8, 8, 2.0, GLOW, step=0.6)
    els += ball("Glut", 9.5, 8, 8, 2.8, LIGHT, step=1.2)
    for angle, length, w in ((38, 19, 4.4), (20, 24, 5.6), (2, 26, 6.2), (-16, 22, 4.8), (-34, 16, 3.4),
                             (56, 14, 3.0), (-50, 12, 2.6)):
        rad = math.radians(angle)
        n = max(6, int(length / 0.8))
        pts = [(10 + math.cos(rad) * length * k / (n - 1),
                8 + math.sin(rad) * length * k / (n - 1) + math.sin(k * 0.3) * 1.4,
                8 + math.sin(k * 0.22) * 0.6) for k in range(n)]
        els += chain("Schweif", pts, taper(w, 0.8, n), SECOND, tip=GLOW)
        els += chain("Schweifglut", [(x, y + 0.3, z - 0.45) for x, y, z in pts[:int(n * 0.6)]],
                     taper(w * 0.6, 0.5, int(n * 0.6)), LIGHT)
        els += chain("Schweifkern", [(x, y, z + 0.5) for x, y, z in pts[:int(n * 0.35)]],
                     taper(w * 0.35, 0.4, int(n * 0.35)), GLOW)
    for x, y, s in ((16, 13.5, 0.9), (21, 11, 0.7), (25, 8.5, 0.8), (18, 3.5, 0.7), (23, 5, 0.6)):
        els.append(centered("Sternenstaub", x, y, 8, s, s * 2.6, s, GLOW))
        els.append(centered("Sternenstaub", x, y, 8, s * 2.6, s, s, GLOW))
    return wings_spec(els)


def cosmos_orbit_pack():
    els = [box("Träger", [6.8, 5.5, 8], [9.2, 10.5, 10], X1),
           box("Sockel", [6.2, 4.5, 9.5], [9.8, 6.5, 11.5], X1)]
    els += ball("Sternkern", 8, 9, 13, 1.9, GLOW, step=0.6)
    els += orbit("Bahn 1", 8, 9, 13, 5.2, 0, 24, 0.7, MAIN)
    els += orbit("Bahn 2", 8, 9, 13, 5.2, 62, 24, 0.7, SECOND, phase=0.3)
    els += orbit("Bahn 3", 8, 9, 13, 5.2, 118, 24, 0.7, LIGHT, phase=0.6)
    for x, y, z, r, cell in ((12.4, 11.5, 13, 1.1, SECOND), (4.2, 7.2, 14.5, 0.9, LIGHT), (8, 9, 18.2, 1.0, MAIN)):
        els += ball("Trabant", x, y, z, r, cell, step=0.55)
    return dict(kind="back", elements=els)


def cosmos_star_whale():
    body = ball("Leib", 8, 8.5, 8.5, 4.0, MAIN, step=0.9, squash=0.85)
    body += ball("Kopf", 8, 8.2, 4.2, 3.4, MAIN, step=0.9, squash=0.8)
    body.append(box("Maul", [5.2, 5.8, 0.8], [10.8, 7.4, 5], SECOND, north=SECOND))
    body.append(box("Gesicht", [5.4, 7.6, 1.1], [10.6, 10, 1.4], X1, north=X1))
    body += ball("Bauch", 8, 6.2, 7.5, 3.0, LIGHT, step=1.0, squash=0.6)
    for z in (6.0, 7.6, 9.2, 10.8):
        body.append(centered("Kehlfurche", 8, 5.3, z, 5.2, 0.5, 0.6, DARK))
    body += chain("Schwanzstiel", [(8, 8.4, 12), (8, 8.6, 14), (8, 8.8, 15.8)], [(3.0, 2.6, 2.2), (2.2, 2.0, 2.2), (1.5, 1.4, 2.0)], MAIN)
    for side in (-1, 1):
        body += chain("Fluke", [(8 + side * 1.2, 9.2, 16.4), (8 + side * 3.4, 9.8, 17.6), (8 + side * 5.4, 10.4, 18.4)],
                      [(2.6, 1.0, 2.6), (2.4, 0.8, 2.2), (1.8, 0.6, 1.6)], SECOND, tip=GLOW)
    for x, y, z, s in ((5.2, 10.6, 6.5, 0.7), (10.8, 10.2, 7.8, 0.6), (6.4, 11.2, 10.5, 0.5), (9.6, 11.4, 5.4, 0.6)):
        body.append(centered("Sternmal", x, y, z, s, s * 2.4, s, GLOW))
        body.append(centered("Sternmal", x, y, z, s * 2.4, s, s, GLOW))
    wing = [box("Brustflosse", [8, 7.4, 6.5], [15.5, 8.2, 10.5], SECOND),
            box("Flossenspitze", [14, 7.4, 7.5], [16.8, 8.1, 9.8], GLOW),
            box("Flossenansatz", [8, 6.8, 7], [10.5, 8.6, 10], MAIN)]
    return dict(kind="pet", parts={"body": body, "wing_a": wing, "wing_b": mirror(wing)},
                icon=body + shift(wing, 2.5, 0, 0) + shift(mirror(wing), -2.5, 0, 0))


def cosmos_ufo():
    els = disc("Untertasse", 8, 7.6, 8, 6.2, 1.0, X1, axis="y", step=0.8)
    els += disc("Rumpfkante", 8, 6.9, 8, 4.6, 0.7, DARK, axis="y", step=0.8)
    els += ball("Kuppel", 8, 8.1, 8, 3.2, GLOW, step=0.7, squash=0.85, top_only=True)
    els.append(box("Pilotensicht", [5.6, 9, 5.2], [10.4, 10.6, 5.5], X2, north=X2))
    els += ring("Landelichter", 8, 7.0, 8, 5.4, 0.8, MAIN, count=10, size=1.0)
    els += ring("Kuppelfassung", 8, 8.6, 8, 3.4, 0.5, X1, count=16, size=0.7)
    for i, (r, s) in enumerate(((2.4, 1.4), (3.4, 1.0), (4.3, 0.7))):
        els += disc("Strahl", 8, 6.2 - i * 0.9, 8, r, s, SECOND, axis="y", step=1.0)
    els.append(centered("Antenne", 8, 11.4, 8, 0.5, 2.2, 0.5, X1))
    els.append(centered("Antennenlicht", 8, 12.6, 8, 1.1, 1.1, 1.1, MAIN))
    return dict(kind="pet", elements=els)


item("cosmos", "cosmos_ringed_planet", "HEAD", "HALO", "ULTRA", "Ringplanet", "Ringed Planet", cosmos_ringed_planet, scale=1.35)
item("cosmos", "cosmos_crescent", "HEAD", "NONE", "EPIC", "Mondsichel-Diadem", "Crescent Diadem", cosmos_crescent,
     extras=[(hexc(0xC8D0FF), "metal")])
item("cosmos", "cosmos_comet_wings", "BACK", "WINGS", "ULTRA", "Kometenschweif", "Comet Trails", cosmos_comet_wings, scale=1.35)
item("cosmos", "cosmos_orbit_pack", "BACK", "BACKPACK", "LEGENDARY", "Sternenbahn", "Orrery", cosmos_orbit_pack,
     extras=[(hexc(0xB0A0E0), "metal")])
item("cosmos", "cosmos_star_whale", "PET", "DRAGON", "ULTRA", "Sternenwal", "Star Whale", cosmos_star_whale, scale=1.3,
     extras=[(hexc(0x2AA8FF), "face")])
item("cosmos", "cosmos_ufo", "PET", "GHOST", "EPIC", "Kleines Ufo", "Little UFO", cosmos_ufo,
     extras=[(hexc(0x9098B8), "metal"), (hexc(0x101830), "face")])


# =========================================================================== BLUTMOND
def bloodmoon_red_moon():
    els = ball("Blutmond", 8, 8, 8, 4.2, MAIN, step=0.8)
    for cx, cy, cz, r in ((5.6, 9.6, 5.8, 1.1), (10.4, 6.4, 6.2, 0.9), (8.6, 10.8, 10.2, 0.8), (5.2, 5.8, 9.4, 0.7)):
        els += ball("Krater", cx, cy, cz, r, DARK, step=0.5)
    els += ring("Mondglut", 8, 8, 8, 4.6, 0.5, GLOW, count=20, size=0.7, axis="xy")
    # Fledermäuse auf der Umlaufbahn, jede mit gespreizten Schwingen
    for x, z, deg, i in around(3, 10.4):
        y = 8 + (3.6, -3.0, 1.2)[i]
        rad = math.radians(deg)
        nx, nz = -math.sin(rad), math.cos(rad)     # Querachse: die Schwingen spannen sich seitlich auf
        els.append(centered("Fledermausleib", x, y, z, 2.0 + abs(nz), 2.4, 2.0 + abs(nx), DARK))
        for side in (-1, 1):
            els.append(centered("Ohr", x + nx * 0.8 * side, y + 2.0, z + nz * 0.8 * side, 0.9, 1.8, 0.9, DARK))
            for d, h, w in ((2.0, 0.6, 2.6), (3.6, 1.2, 3.0), (5.0, 0.9, 2.4), (6.0, 0.0, 1.4)):
                els.append(centered("Schwinge", x + nx * d * side, y + h, z + nz * d * side,
                                    w * abs(nz) + 0.6, 0.6, w * abs(nx) + 0.6, SECOND))
            els.append(centered("Schwingenknochen", x + nx * 3.0 * side, y + 1.6, z + nz * 3.0 * side,
                                2.4 * abs(nz) + 0.5, 0.5, 2.4 * abs(nx) + 0.5, DARK))
        els.append(centered("Augen", x, y + 0.6, z, 1.9 * abs(nz) + 0.5, 0.5, 1.9 * abs(nx) + 0.5, GLOW))
    return dict(kind="halo", elements=els)


def bloodmoon_bat_ears():
    t = T_TOP
    els = band("Stirnband", t - 2.4, t - 1.2, 7.6, DARK)
    els.append(box("Bandschnalle", [6.4, t - 2.7, 1.1], [9.6, t - 0.9, 1.6], SECOND))
    for side in (-1, 1):
        base = 8 + side * 3.4
        # Ohr: Membran zwischen zwei Streben, nach außen geneigt
        for k in range(9):
            f = k / 8
            h = t - 1 + f * 7.5
            x = base + side * f * 3.6
            w = 3.0 * (1 - f) ** 0.7 + 0.5
            els.append(centered("Ohrmembran", x, h, 8, w, 1.1, 1.0 + (1 - f) * 1.4, SECOND))
        els += chain("Ohrknochen", [(base, t - 1, 7.2), (base + side * 1.8, t + 2.5, 7.0), (base + side * 3.6, t + 6.5, 7.0)],
                     [(0.8, 0.8, 0.8), (0.7, 0.7, 0.7), (0.6, 0.6, 0.6)], DARK)
        els.append(centered("Ohrinnen", base + side * 1.2, t + 1.6, 8.4, 1.6, 3.2, 0.5, MAIN))
    els.append(centered("Rubin", 8, t - 1.8, 1.0, 1.4, 1.4, 0.7, GLOW))
    return dict(kind="head", elements=els)


def bloodmoon_vampire_wings():
    fingers = ((72, 11.5), (46, 15.5), (20, 17.5), (-6, 15), (-30, 10))
    els = sail((9, 8.4), fingers, MAIN, z=8.0, thick=0.5, sag=0.24, step=0.5)
    els.append(box("Schulterknochen", [8, 7.4, 7.5], [10.5, 9.2, 8.7], X1))
    for angle, length in fingers:
        rad = math.radians(angle)
        n = max(5, int(length / 1.1))
        pts = [(9 + math.cos(rad) * length * k / (n - 1), 8.4 + math.sin(rad) * length * k / (n - 1), 8.45)
               for k in range(n)]
        els += chain("Fingerknochen", pts, taper(1.2, 0.5, n), X1)
        els.append(centered("Kralle", 9 + math.cos(rad) * (length + 0.7), 8.4 + math.sin(rad) * (length + 0.7), 8.2,
                            1.0, 1.0, 0.7, X2))
    els += chain("Armknochen", [(9.6, 8.4, 8.45), (12.5, 11.5, 8.45), (15.5, 13.5, 8.45)],
                 [(1.6, 1.6, 1.0), (1.3, 1.3, 0.9), (1.1, 1.1, 0.8)], X1)
    return wings_spec(els)


def bloodmoon_vampire_cape():
    top = [cape_box("Hochkragen links", [2.0, 15.2, 7.4], [7.4, 21.5, 9.0], CAPE_EDGE),
           cape_box("Hochkragen rechts", [8.6, 15.2, 7.4], [14.0, 21.5, 9.0], CAPE_EDGE),
           cape_box("Kragenband", [2.0, 14.6, 7.2], [14.0, 15.8, 9.2], CAPE_OUTER),
           cape_box("Schließe", [7.0, 15.0, 6.8], [9.0, 17.0, 7.6], CAPE_EDGE)]
    bottom = []
    for i in range(6):
        x = 3 + i * 1.7
        depth = 3.6 if i % 2 == 0 else 2.0
        for k in range(5):
            f = k / 4
            bottom.append(cape_box("Zipfel", [x + f * 0.75, 10 - depth * f - 0.6, 7.6],
                                   [x + 1.7 - f * 0.75, 10 - depth * f, 8.4], CAPE_OUTER))
    return dict(kind="cape", top=top, bottom=bottom)


def bloodmoon_bat():
    body = [box("Leib", [5.8, 5.5, 6], [10.2, 10.5, 10.5], X1, north=X2),
            box("Brustfell", [6.4, 5.8, 5.2], [9.6, 9, 6.2], X3),
            box("Schnauze", [6.8, 6.6, 4], [9.2, 8.4, 5.4], X1),
            box("Zahn", [7.1, 6.2, 4.2], [7.7, 6.9, 4.8], LIGHT), box("Zahn", [8.3, 6.2, 4.2], [8.9, 6.9, 4.8], LIGHT),
            box("Fuß", [6.2, 4.6, 8.5], [7.4, 5.6, 10], X1), box("Fuß", [8.6, 4.6, 8.5], [9.8, 5.6, 10], X1)]
    for side in (-1, 1):
        body += chain("Ohr", [(8 + side * 1.9, 10.5, 7.2), (8 + side * 2.9, 13, 7.0), (8 + side * 3.6, 15.2, 7.0)],
                      [(1.7, 1.4, 1.2), (1.5, 1.6, 1.0), (0.9, 1.4, 0.8)], X1, tip=X3)
        body.append(centered("Ohrinnen", 8 + side * 2.6, 13, 6.5, 1.0, 3.0, 0.4, X3))
    fingers = ((66, 6.0), (34, 8.0), (2, 8.5), (-28, 6.5))
    wing = sail((11.5, 9), fingers, MAIN, z=8.0, thick=0.45, sag=0.2, step=0.45)
    wing.append(box("Armknochen", [8, 8.5, 7.6], [12, 9.1, 8.4], X1))
    for angle, length in fingers:
        rad = math.radians(angle)
        pts = [(11.5 + math.cos(rad) * length * k / 5, 9 + math.sin(rad) * length * k / 5, 8.4) for k in range(6)]
        wing += chain("Finger", pts, taper(0.9, 0.4, 6), X1)
    return dict(kind="pet", parts={"body": body, "wing_a": wing, "wing_b": mirror(wing)},
                icon=body + shift(wing, 1.5, 0, 0) + shift(mirror(wing), -1.5, 0, 0))


def bloodmoon_imp():
    body = ball("Bauch", 8, 5.2, 8, 3.6, X1, step=0.9, squash=0.9)
    body.append(box("Kopf", [5.2, 8, 4.6], [10.8, 13, 10.5], X1, north=X2))
    body.append(box("Nase", [7.2, 9.5, 3.4], [8.8, 11, 4.8], X1))
    body.append(box("Grinsen", [6, 8.6, 4.3], [10, 9.3, 4.6], SECOND))
    for side in (-1, 1):
        body += chain("Horn", bezier((8 + side * 2.2, 13, 7.5), (8 + side * 3.6, 15.8, 7.2), (8 + side * 2.4, 17.4, 6.2), 5),
                      taper(1.5, 0.5, 5), DARK, tip=LIGHT)
        body.append(centered("Ohr", 8 + side * 5.6, 11, 8, 2.4, 1.2, 2.0, X1))
        body.append(box("Fuß", [8 + side * 2.6 - 1.3, 1.0, 5.6], [8 + side * 2.6 + 1.3, 2.8, 9.2], DARK))
    body += chain("Schwanz", bezier((8, 4.5, 11), (8, 3.0, 14.5), (8, 7.0, 16.0), 6), taper(1.0, 0.6, 6), X1)
    body += [centered("Schwanzspitze", 8, 7.8, 16.4, 0.6, 2.4, 1.6, SECOND),
             centered("Schwanzspitze", 8, 8.4, 16.4, 2.2, 1.2, 1.4, SECOND)]
    flipper = [box("Arm", [10.2, 4.5, 7], [12.6, 8.5, 9.4], X1),
               box("Kralle", [11.8, 3.4, 7.2], [13.2, 4.8, 9.2], DARK)]
    return dict(kind="pet", parts={"body": body, "flipper_a": flipper, "flipper_b": mirror(flipper)},
                icon=body + flipper + mirror(flipper))


item("bloodmoon", "bloodmoon_red_moon", "HEAD", "HALO", "LEGENDARY", "Blutmond", "Blood Moon", bloodmoon_red_moon, scale=1.2)
item("bloodmoon", "bloodmoon_bat_ears", "HEAD", "NONE", "RARE", "Fledermausohren", "Bat Ears", bloodmoon_bat_ears)
item("bloodmoon", "bloodmoon_vampire_wings", "BACK", "WINGS", "LEGENDARY", "Vampirschwingen", "Vampire Wings", bloodmoon_vampire_wings, scale=1.2)
item("bloodmoon", "bloodmoon_vampire_cape", "BACK", "NONE", "EPIC", "Vampirumhang", "Vampire Cloak", bloodmoon_vampire_cape, scale=1.05)
item("bloodmoon", "bloodmoon_bat", "PET", "DRAGON", "EPIC", "Fledermaus", "Little Bat", bloodmoon_bat,
     extras=[hexc(0x3A2028), (hexc(0x3A2028), "face"), hexc(0x6A3A44)])
item("bloodmoon", "bloodmoon_imp", "PET", "PENGUIN", "LEGENDARY", "Kobold", "Imp", bloodmoon_imp,
     extras=[hexc(0xC83A46), (hexc(0xC83A46), "face")])


# =========================================================================== KÖNIGS
def regal_imperial_crown():
    t = T_TOP
    els = band("Stirnreif", t - 0.4, t + 2.2, 7.4, X1, thick=1.2)
    els += band("Reifkante oben", t + 2.2, t + 3.0, 7.7, X2, thick=1.3)
    els += band("Reifkante unten", t - 1.0, t - 0.4, 7.7, X2, thick=1.3)
    els += band("Pelzkante", t - 2.4, t - 1.0, 7.6, LIGHT, thick=1.4)
    for x, z, deg, i in around(8, 6.6):
        h = 3.2 if i % 2 == 0 else 2.0
        els += stack("Zacke", x, z, t + 3.0, [(h * 0.55, 1.1), (h * 0.45, 0.6)], X1, tip=X2)
        els.append(centered("Zackenperle", x, t + 3.0 + h + 0.5, z, 1.0, 1.0, 1.0, LIGHT))
        els.append(centered("Reifjuwel", x * 0.97 + 0.24, t + 0.9, z * 0.97 + 0.24, 1.5, 1.5, 1.5, X3 if i % 2 else GLOW))
    # vier Bügel zum Scheitel
    for deg in (45, 135, 225, 315):
        rad = math.radians(deg)
        arc = bezier((8 + math.cos(rad) * 6.9, t + 3.0, 8 + math.sin(rad) * 6.9),
                     (8 + math.cos(rad) * 6.2, t + 9.8, 8 + math.sin(rad) * 6.2),
                     (8, t + 10.6, 8), 8)
        pts = densify(arc, 0.7)
        els += chain("Bügel", pts, [1.0] * len(pts), X1)
        els += chain("Bügelperlen", pts[1::3], [1.4] * len(pts), LIGHT)
    # Samtkappe: deutlich flacher, damit die Bügel frei stehen
    els += ball("Samtkappe", 8, t + 2.4, 8, 6.4, MAIN, step=1.2, squash=0.5, top_only=True)
    els += ball("Reichsapfel", 8, t + 12.2, 8, 1.6, X2, step=0.6)
    els.append(centered("Apfelband", 8, t + 12.2, 8, 3.4, 0.5, 3.4, X1))
    els.append(centered("Kreuz senkrecht", 8, t + 15.0, 8, 0.7, 3.0, 0.7, X1))
    els.append(centered("Kreuz waagrecht", 8, t + 15.4, 8, 2.2, 0.7, 0.7, X1))
    return dict(kind="head", elements=els)


def regal_jewel_ring():
    els = []
    for x, z, deg, i in around(6, 6.2):
        y = 8 + (1.8, -1.2, 2.4, -0.6, 1.2, -2.0)[i]
        size = (1.5, 1.1, 1.3, 1.0, 1.4, 1.2)[i]
        cell = (MAIN, X3, GLOW, X3, MAIN, GLOW)[i]
        # geschliffenes Juwel: Oktaeder
        for k, f in enumerate((0.35, 0.75, 1.0, 0.75, 0.35)):
            els.append(centered("Juwel", x, y - size * 1.2 + k * size * 0.6, z, size * f * 2, size * 0.62, size * f * 2, cell))
        els.append(centered("Fassung", x, y - size * 1.4, z, size * 0.9, 0.5, size * 0.9, X1))
        els += ball("Glanz", x + 0.4, y + size * 0.4, z - 0.5, 0.35, LIGHT, step=0.3)
    els += ring("Goldreif", 8, 8, 8, 6.2, 0.45, X1, count=28, size=0.6)
    els += ring("Funken", 8, 8, 8, 7.4, 0.3, X2, count=14, size=0.4)
    return dict(kind="halo", elements=els)


def regal_griffin_wings():
    root = (9.0, 8.4)
    # geschlossene Schwungfederfläche, gezackter Hinterrand durch die Federspitzen
    quills = ((62, 4.5), (50, 6.5), (38, 9.0), (26, 12.0), (14, 15.5), (2, 18.5),
              (-10, 19.0), (-22, 16.0), (-34, 12.0), (-48, 7.0))
    # geschlossener Flügelkörper …
    els = sail(root, tuple((a, l * 0.6) for a, l in quills), MAIN, z=8.0, thick=0.7, sag=0.06, step=0.45)
    els += sail(root, tuple((a, l * 0.36) for a, l in quills), SECOND, z=8.6, thick=0.55, sag=0.1, step=0.45)
    els += sail(root, tuple((a, l * 0.18) for a, l in quills), LIGHT, z=9.1, thick=0.5, sag=0.12, step=0.45)
    # … und davor die einzeln stehenden Schwungfedern
    for angle, length in quills:
        rad = math.radians(angle)
        inner = length * 0.52
        els += blade("Schwungfeder", (root[0] + math.cos(rad) * inner, root[1] + math.sin(rad) * inner),
                     angle, length - inner, 2.3, MAIN, tip=X2, z=8.0, thick=0.7, taper_to=0.42)
        n = max(5, int(length / 1.1))
        pts = [(root[0] + math.cos(rad) * length * k / (n - 1), root[1] + math.sin(rad) * length * k / (n - 1), 8.45)
               for k in range(n)]
        els += chain("Federkiel", pts, taper(0.8, 0.3, n), X1)
    els.append(box("Vorderkante", [8.2, 8.0, 7.5], [15.0, 8.9, 8.7], X1))
    els += ball("Schulterbausch", 9.6, 8.4, 9.4, 2.2, LIGHT, step=0.8)
    return wings_spec(els)


def regal_mantle():
    top = [cape_box("Hermelinkragen", [1.2, 14.0, 6.6], [14.8, 17.6, 9.6], CAPE_EDGE),
           cape_box("Kragenrolle", [0.8, 16.6, 6.8], [15.2, 18.4, 9.4], CAPE_EDGE),
           cape_box("Schulterstück", [1.6, 12.6, 7.0], [14.4, 14.4, 9.2], CAPE_EDGE)]
    for x in (3.0, 6.0, 9.0, 12.0):
        top.append(cape_box("Schwanzspitze", [x, 15.0, 6.3], [x + 1.0, 17.0, 6.7], CAPE_OUTER))
    bottom = [cape_box("Hermelinsaum", [1.6, 7.2, 7.3], [14.4, 10.2, 8.7], CAPE_EDGE),
              cape_box("Saumrolle", [1.2, 6.2, 7.4], [14.8, 7.6, 8.6], CAPE_EDGE)]
    for x in (2.6, 5.4, 8.2, 11.0, 13.0):
        bottom.append(cape_box("Schwanzspitze", [x, 7.8, 7.0], [x + 1.0, 9.6, 7.3], CAPE_OUTER))
    return dict(kind="cape", top=top, bottom=bottom, robe=True)


def regal_lion_cub():
    body = [box("Körper", [5.6, 3.8, 6.5], [10.4, 8.4, 11.5], X1, north=X1),
            box("Kopf", [5.4, 7.8, 3.2], [10.6, 12.4, 8.0], X1, north=X4),
            box("Schnauze", [6.6, 8.2, 2.0], [9.4, 10.0, 3.4], X3),
            box("Nase", [7.4, 9.4, 1.7], [8.6, 10.1, 2.2], DARK),
            box("Pfote", [5.6, 3.6, 4.6], [7.2, 5.0, 7.0], X3), box("Pfote", [8.8, 3.6, 4.6], [10.4, 5.0, 7.0], X3)]
    for side in (-1, 1):
        body.append(centered("Ohr", 8 + side * 2.2, 12.6, 5.6, 1.6, 1.4, 1.2, X1))
        body.append(centered("Ohrinnen", 8 + side * 2.2, 12.7, 5.2, 0.9, 0.8, 0.5, X3))
    # Mähne aus Büscheln rund um den Kopf
    for x, z, deg, i in around(10, 4.2, cx=8, cz=6.0):
        y = 10.0 + math.sin(math.radians(deg)) * 1.2
        size = 2.2 if i % 2 == 0 else 1.6
        body.append(centered("Mähne", x, y, z + 1.4, size, size * 1.4, size, X2 if i % 2 else X5))
    body += chain("Schwanz", bezier((8, 8.0, 11.5), (8, 10.5, 14.0), (8, 13.0, 12.8), 5), taper(1.0, 0.8, 5), X1)
    body.append(centered("Schwanzquaste", 8, 13.6, 12.6, 1.8, 2.2, 1.8, X2))
    tail = chain("Schwanz", bezier((8, 8.5, 10.5), (8, 11.5, 13.0), (8, 14.0, 11.5), 5), taper(1.1, 0.9, 5), X1)
    tail.append(centered("Quaste", 8, 14.6, 11.3, 1.9, 2.3, 1.9, X2))
    sleep = [box("Körper", [4.8, 3.8, 6.0], [11.2, 7.4, 12.0], X1),
             box("Kopf", [5.6, 3.8, 3.0], [10.4, 8.0, 6.6], X1, north=X5),
             box("Schnauze", [6.8, 4.4, 2.0], [9.2, 5.8, 3.2], X3)]
    for side in (-1, 1):
        sleep.append(centered("Ohr", 8 + side * 2.0, 8.2, 5.0, 1.5, 1.3, 1.1, X1))
    for x, z, deg, i in around(8, 3.6, cx=8, cz=5.0):
        sleep.append(centered("Mähne", x, 6.0 + math.sin(math.radians(deg)) * 0.8, z + 1.0, 1.8, 1.8, 1.8, X2 if i % 2 else X5))
    sleep += chain("Schwanzdecke", [(4.4, 5.0, 10.5), (4.2, 5.2, 7.0), (6.0, 5.4, 4.6)], [1.1, 1.0, 1.0], X1)
    sleep.append(centered("Quaste", 6.6, 5.6, 3.8, 1.7, 1.7, 1.7, X2))
    return dict(kind="pet", parts={"body": body, "tail": tail, "sleep": sleep}, icon=body)


def regal_griffin():
    body = [box("Löwenleib", [5.8, 5.2, 7.5], [10.2, 10.0, 12.5], X2, north=X2),
            box("Brustgefieder", [5.6, 5.6, 4.8], [10.4, 10.4, 7.8], LIGHT),
            box("Kopf", [5.8, 8.6, 1.8], [10.2, 12.6, 5.2], LIGHT, north=X4),
            box("Schnabel oben", [6.8, 10.0, -0.4], [9.2, 11.4, 2.0], X1),
            box("Schnabelspitze", [7.2, 9.2, -0.6], [8.8, 10.2, 1.2], X1),
            box("Schnabel unten", [7.0, 9.2, 0.4], [9.0, 10.0, 2.0], X3),
            box("Hinterbein", [5.6, 3.0, 9.6], [7.4, 6.4, 12.2], X2), box("Hinterbein", [8.6, 3.0, 9.6], [10.4, 6.4, 12.2], X2),
            box("Vorderkralle", [5.8, 3.2, 5.6], [7.4, 5.8, 8.2], X1), box("Vorderkralle", [8.6, 3.2, 5.6], [10.2, 5.8, 8.2], X1)]
    for side in (-1, 1):
        body.append(centered("Federohr", 8 + side * 1.8, 13.2, 3.4, 1.0, 2.0, 0.9, LIGHT))
    body += chain("Löwenschwanz", bezier((8, 9.4, 12.5), (8, 12.0, 15.0), (8, 14.4, 13.4), 5), taper(1.1, 0.8, 5), X2)
    body.append(centered("Schwanzquaste", 8, 15.0, 13.2, 1.8, 2.2, 1.8, X1))
    wing = [box("Flügelansatz", [8, 9.0, 7.0], [10.5, 10.2, 10.0], LIGHT)]
    for angle, length in ((58, 7.5), (34, 10.0), (10, 11.0), (-14, 9.0)):
        wing += blade("Schwungfeder", (9.5, 9.6), angle, length, 2.0, LIGHT, tip=X1, z=8.6, thick=0.6, taper_to=0.5)
    for angle, length in ((46, 4.5), (22, 5.5), (0, 5.0)):
        wing += blade("Deckfeder", (9.5, 9.4), angle, length, 1.5, X2, tip=LIGHT, z=9.1, thick=0.5)
    return dict(kind="pet", parts={"body": body, "wing_a": wing, "wing_b": mirror(wing)},
                icon=body + shift(wing, 2, 0, 0) + shift(mirror(wing), -2, 0, 0))


item("regal", "regal_imperial_crown", "HEAD", "NONE", "LEGENDARY", "Kaiserkrone", "Imperial Crown", regal_imperial_crown, scale=1.15,
     extras=[(hexc(0xE8B43A), "metal"), (hexc(0xFFF0A0), "metal"), hexc(0x2AA0E8), hexc(0xF0F0F0), hexc(0xE8E0D0)])
item("regal", "regal_jewel_ring", "HEAD", "HALO", "EPIC", "Juwelenreigen", "Jewel Ring", regal_jewel_ring,
     extras=[(hexc(0xE8B43A), "metal"), (hexc(0xFFF0A0), "metal"), hexc(0x2AA0E8)])
item("regal", "regal_griffin_wings", "BACK", "WINGS", "LEGENDARY", "Greifenschwingen", "Griffin Wings", regal_griffin_wings, scale=1.2,
     extras=[(hexc(0xE8B43A), "metal"), (hexc(0xFFF0A0), "metal")])
item("regal", "regal_mantle", "BACK", "NONE", "EPIC", "Krönungsmantel", "Coronation Mantle", regal_mantle, scale=1.05)
item("regal", "regal_lion_cub", "PET", "KITTEN", "LEGENDARY", "Löwenjunges", "Lion Cub", regal_lion_cub,
     extras=[hexc(0xE8C06A), hexc(0xB07030), hexc(0xFFE8C0), (hexc(0xE8C06A), "face"), (hexc(0xE8C06A), "sleep")])
item("regal", "regal_griffin", "PET", "DRAGON", "LEGENDARY", "Greifenküken", "Griffin Chick", regal_griffin,
     extras=[(hexc(0xE8B43A), "metal"), hexc(0xC89040), hexc(0xFFD070), (hexc(0xF0EADC), "face")])


# =========================================================================== KIRSCHBLÜTEN
def sakura_kanzashi():
    t = T_TOP
    els = ball("Haarknoten", 8, t + 1.0, 11.5, 4.0, X1, step=0.9, squash=0.9)
    els.append(box("Haarband", [4.4, t - 0.4, 8.2], [11.6, t + 0.8, 12.0], X2))
    els.append(box("Haarschopf", [6.8, t + 4.2, 10.0], [9.2, t + 6.2, 12.4], X1))
    # zwei gekreuzte Haarnadeln
    for side in (-1, 1):
        pts = densify([(8 + side * 1.0, t + 2.0, 10.0), (8 + side * 7.5, t + 4.5, 5.5)], 0.9)
        els += chain("Haarnadel", pts, [0.5] * len(pts), X3)
        els += blossom(8 + side * 8.0, t + 5.0, 5.0, 1.7, MAIN, GLOW, plane="xz")
        # Perlengehänge
        for k in range(4):
            els.append(centered("Kette", 8 + side * 7.6, t + 4.2 - k * 1.3, 5.2, 0.4, 0.9, 0.4, X3))
        els.append(centered("Anhänger", 8 + side * 7.6, t - 1.4, 5.2, 1.1, 1.4, 1.1, SECOND))
    els += blossom(8, t + 3.0, 7.6, 2.0, SECOND, GLOW, plane="xy")
    for x, y, z, r in ((4.5, t + 1.6, 6.5, 1.2), (11.5, t + 2.4, 6.2, 1.0), (6.0, t + 4.6, 13.5, 1.1)):
        els += blossom(x, y, z, r, MAIN, X3, plane="xz")
    return dict(kind="head", elements=els)


def sakura_blossom_ring():
    els = []
    # Blüten auf einer Spirale, dazwischen einzelne wirbelnde Blätter
    for i in range(7):
        a = math.tau * i / 7
        r = 5.4 + math.sin(a * 2) * 0.9
        x, z = 8 + math.cos(a) * r, 8 + math.sin(a) * r
        y = 8 + math.sin(a * 1.5) * 2.4
        els += blossom(x, y, z, 1.5 + (i % 3) * 0.25, MAIN if i % 2 else SECOND, GLOW, plane="xz")
    for i in range(12):
        a = math.tau * i / 12 + 0.26
        r = 7.6 - (i % 3) * 0.9
        x, z = 8 + math.cos(a) * r, 8 + math.sin(a) * r
        y = 8 + math.cos(a * 2.2) * 3.0
        els.append(centered("Blütenblatt", x, y, z, 1.4, 0.4, 1.0, SECOND if i % 2 else LIGHT))
        els.append(centered("Blütenblatt", x + 0.5, y + 0.3, z + 0.3, 0.9, 0.4, 0.8, LIGHT))
    els += ring("Duftschleier", 8, 8, 8, 6.4, 0.3, GLOW, count=18, size=0.4)
    return dict(kind="halo", elements=els)


def sakura_petal_wings():
    els = []
    # jede Schwinge besteht aus fünf großen Blütenblättern (Linsenform)
    for angle, length, width, cell, z in ((70, 10.0, 2.8, SECOND, 8.6), (42, 14.5, 3.6, MAIN, 8.0),
                                          (12, 16.5, 4.0, MAIN, 8.0), (-18, 13.5, 3.4, SECOND, 8.6),
                                          (-46, 9.5, 2.6, LIGHT, 9.1)):
        rad = math.radians(angle)
        nx, ny = -math.sin(rad), math.cos(rad)      # quer zur Blattachse
        n = max(8, int(length / 0.75))
        for k in range(n):
            f = (k + 0.5) / n
            w = width * math.sin(math.pi * f ** 0.8)
            d = length * f
            bx, by = 8.6 + math.cos(rad) * d, 8 + math.sin(rad) * d
            steps = max(1, int(w / 0.8))
            for s in range(-steps, steps + 1):
                g = s / max(1, steps)
                tint = LIGHT if abs(g) > 0.75 or f > 0.86 else cell
                els.append(centered("Blütenblatt", bx + nx * g * w, by + ny * g * w, z, 1.0, 1.0, 0.5, tint))
        # gekerbte Spitze und Mittelader
        tipx, tipy = 8.6 + math.cos(rad) * (length - 0.4), 8 + math.sin(rad) * (length - 0.4)
        els.append(centered("Kerbe", tipx, tipy, z, 0.9, 0.9, 0.7, X1))
        for k in range(int(length)):
            f = (k + 0.5) / int(length)
            els.append(centered("Ader", 8.6 + math.cos(rad) * length * f, 8 + math.sin(rad) * length * f, z + 0.3,
                                0.8, 0.8, 0.3, LIGHT))
    els += blossom(9.5, 8, 8.4, 1.8, LIGHT, GLOW, plane="xy")
    for x, y in ((17, 14.5), (21, 10), (15, 2.5), (19, 5)):
        els.append(centered("Loses Blatt", x, y, 8, 1.4, 0.5, 1.0, SECOND))
    return wings_spec(els)


def sakura_wagasa():
    els = []
    # Schirmkuppel aus Ringen mit abnehmendem Radius, leicht geneigt
    for k in range(7):
        f = k / 6
        r = 6.4 * math.cos(f * 1.15)
        y = 10.5 + f * 4.6
        z = 13.0 - f * 1.4
        els += ring("Schirmbahn", 8, y, z, max(0.6, r), 1.0, MAIN if k % 2 else SECOND, count=max(6, int(r * 4)), size=1.5)
    els += ring("Schirmrand", 8, 10.2, 13.0, 6.4, 0.9, X1, count=26, size=1.2)
    for x, z, deg, i in around(8, 6.0, cx=8, cz=13.0):
        pts = densify([(x, 10.4, z), (8, 15.0, 11.6)], 1.1)
        els += chain("Speiche", pts, [0.4] * len(pts), X1)
    els += chain("Schirmstock", densify([(8, 15.6, 11.4), (8, 3.0, 13.8)], 1.0), [0.6] * 40, X1)
    els.append(centered("Knauf", 8, 16.2, 11.3, 1.0, 1.4, 1.0, X1))
    els.append(centered("Griff", 8, 3.2, 13.9, 1.1, 2.4, 1.1, X2))
    els.append(centered("Quaste", 8, 1.6, 13.9, 0.9, 1.6, 0.9, GLOW))
    els.append(box("Halterung", [7.0, 7.5, 8.5], [9.0, 10.5, 12.0], X2))
    return dict(kind="back", elements=els)


def sakura_mochi_bunny():
    els = ball("Mochi", 8, 5.0, 8, 4.4, X1, step=0.9, squash=0.78)
    els.append(box("Gesicht", [5.2, 5.0, 3.6], [10.8, 8.2, 3.9], X2, north=X2))
    for side in (-1, 1):
        els += chain("Ohr", bezier((8 + side * 1.8, 8.2, 8.2), (8 + side * 3.2, 11.5, 7.6), (8 + side * 2.2, 14.6, 7.4), 6),
                     [(1.9, 1.6, 1.5), (1.8, 1.6, 1.4), (1.7, 1.6, 1.3), (1.6, 1.6, 1.3), (1.4, 1.5, 1.2), (1.1, 1.2, 1.1)], X1)
        els.append(centered("Ohrinnen", 8 + side * 2.6, 11.5, 6.9, 0.8, 4.4, 0.4, X3))
        els.append(centered("Wange", 8 + side * 3.0, 5.6, 4.4, 1.6, 1.0, 0.5, X3))
        els.append(centered("Pfötchen", 8 + side * 2.6, 1.6, 5.8, 1.8, 1.2, 2.4, X1))
    els.append(centered("Puschelschwanz", 8, 4.2, 11.8, 2.0, 2.0, 1.6, LIGHT))
    for x, y, z, s in ((5.0, 7.6, 6.0, 0.5), (11.0, 7.2, 6.6, 0.4), (8.0, 8.4, 11.0, 0.45)):
        els.append(centered("Zuckerstaub", x, y, z, s, s, s, GLOW))
    return dict(kind="pet", elements=shift(els, 0, 1.0, 0))


def sakura_lucky_cat():
    body = [box("Körper", [5.2, 3.6, 6.0], [10.8, 9.0, 11.0], LIGHT, north=LIGHT),
            box("Kopf", [5.0, 8.4, 3.6], [11.0, 13.4, 9.0], LIGHT, north=X4),
            box("Schnauze", [6.6, 9.0, 2.6], [9.4, 10.6, 3.8], LIGHT),
            box("Nase", [7.5, 10.0, 2.3], [8.5, 10.6, 2.8], X2),
            box("Latz", [6.0, 4.0, 4.8], [10.0, 8.0, 6.2], X1),
            box("Halsband", [5.2, 8.0, 4.6], [10.8, 9.0, 9.4], X2),
            box("Glöckchen", [7.2, 7.0, 4.0], [8.8, 8.4, 5.4], X3),
            box("Glöckchenschlitz", [7.4, 7.3, 3.8], [8.6, 7.7, 4.1], DARK),
            box("Sitzpfote", [5.4, 3.4, 4.6], [7.2, 5.0, 7.0], LIGHT),
            box("Goldmünze", [8.4, 5.0, 3.8], [11.4, 8.0, 5.0], X3),
            box("Münzloch", [9.5, 6.1, 3.6], [10.3, 6.9, 4.0], X1)]
    for side in (-1, 1):
        body.append(centered("Ohr", 8 + side * 2.4, 13.6, 6.2, 1.8, 1.6, 1.3, LIGHT))
        body.append(centered("Ohrinnen", 8 + side * 2.4, 13.7, 5.8, 1.0, 0.9, 0.5, X2))
        body.append(centered("Fleck", 8 + side * 2.2, 12.4, 8.6, 2.2, 1.6, 1.8, X5))
    # erhobene Winkepfote
    body += chain("Winkearm", [(10.6, 8.6, 6.4), (11.6, 10.2, 6.0), (11.9, 12.0, 5.8)], [(1.7, 1.7, 1.7), (1.6, 1.6, 1.6), (1.5, 1.5, 1.5)], LIGHT)
    body.append(centered("Winkepfote", 12.0, 13.2, 5.8, 2.0, 2.0, 2.0, LIGHT))
    tail = chain("Schwanz", bezier((8, 8.6, 10.6), (8, 12.0, 13.6), (8, 14.6, 11.6), 5), taper(1.4, 1.0, 5), LIGHT)
    tail.append(centered("Schwanzspitze", 8, 15.0, 11.4, 1.5, 1.5, 1.5, X5))
    sleep = [box("Körper", [4.6, 3.6, 6.0], [11.4, 7.4, 12.2], LIGHT),
             box("Kopf", [5.4, 3.6, 2.8], [10.6, 8.0, 6.6], LIGHT, north=X5),
             box("Halsband", [5.4, 4.6, 6.0], [10.6, 5.6, 6.6], X2),
             box("Glöckchen", [7.4, 4.2, 2.6], [8.6, 5.4, 3.4], X3)]
    for side in (-1, 1):
        sleep.append(centered("Ohr", 8 + side * 2.2, 8.2, 4.8, 1.7, 1.5, 1.2, LIGHT))
    sleep += chain("Schwanzdecke", [(4.0, 5.2, 10.8), (3.8, 5.4, 7.0), (5.8, 5.6, 4.4)], [1.4, 1.3, 1.2], LIGHT)
    sleep.append(centered("Spitze", 6.4, 5.8, 3.6, 1.4, 1.4, 1.4, X5))
    return dict(kind="pet", parts={"body": body, "tail": tail, "sleep": sleep}, icon=body + shift(tail, 0, -2, 2))


item("sakura", "sakura_kanzashi", "HEAD", "NONE", "EPIC", "Kanzashi-Haarschmuck", "Kanzashi Pins", sakura_kanzashi,
     extras=[hexc(0x3A2630), hexc(0xC8305A), (hexc(0xE8B43A), "metal")])
item("sakura", "sakura_blossom_ring", "HEAD", "HALO", "LEGENDARY", "Blütenwirbel", "Blossom Swirl", sakura_blossom_ring, scale=1.15)
item("sakura", "sakura_petal_wings", "BACK", "WINGS", "LEGENDARY", "Blütenschwingen", "Petal Wings", sakura_petal_wings, scale=1.15,
     extras=[hexc(0xC8305A)])
item("sakura", "sakura_wagasa", "BACK", "BACKPACK", "EPIC", "Papierschirm", "Paper Parasol", sakura_wagasa,
     extras=[hexc(0x6A3A28), hexc(0x3A2630)])
item("sakura", "sakura_mochi_bunny", "PET", "MUSHROOM", "EPIC", "Mochi-Häschen", "Mochi Bunny", sakura_mochi_bunny,
     extras=[hexc(0xFFF4F8), (hexc(0xFFF4F8), "face"), hexc(0xFF9AC0)])
item("sakura", "sakura_lucky_cat", "PET", "KITTEN", "LEGENDARY", "Glückskatze", "Lucky Cat", sakura_lucky_cat,
     extras=[hexc(0xE84A6A), hexc(0xC02A48), (hexc(0xE8B43A), "metal"), (hexc(0xFFF6F8), "face"), (hexc(0xFFF6F8), "sleep")])


# =========================================================================== MAGMA
def magma_volcano_hat():
    t = T_TOP
    els = []
    layers = 14
    base_r, top_r, height = 7.6, 3.0, 9.5

    def radius(f):
        return base_r - (base_r - top_r) * f ** 0.75

    for k in range(layers):
        f = k / (layers - 1)
        r = radius(f)
        y = t - 1.2 + f * height
        els += disc("Vulkanhang", 8, y + height / layers / 2, 8, r, height / layers + 0.25,
                    X1 if k % 3 else DARK, axis="y", step=1.4)
    crater_y = t - 1.2 + height
    els += ring("Kraterrand", 8, crater_y, 8, top_r, 1.3, DARK, count=16, size=1.7)
    els += disc("Lavasee", 8, crater_y + 0.1, 8, top_r - 0.5, 0.9, GLOW, axis="y", step=0.7)
    els += ball("Lavablase", 8.6, crater_y + 1.0, 7.2, 1.0, MAIN, step=0.5)
    # Lavaströme laufen außen an der Flanke herunter
    for x, z, deg, i in around(5, 1.0, phase=0.4):
        ang = math.radians(deg)
        pts = []
        for k in range(9):
            f = 1 - k / 8
            wobble = math.sin(k * 1.1 + i) * 0.22
            r = radius(f) + 0.55
            pts.append((8 + math.cos(ang + wobble) * r, t - 1.2 + f * height, 8 + math.sin(ang + wobble) * r))
        els += chain("Lavastrom", pts, taper(0.9, 1.3, 9), MAIN, tip=MAIN)
        els += chain("Glutader", [(px, py + 0.3, pz) for px, py, pz in pts], taper(0.5, 0.8, 9), GLOW)
    for x, y, z, r in ((5.0, crater_y + 4.5, 6.5, 2.0), (10.5, crater_y + 6.0, 9.0, 1.6), (7.5, crater_y + 8.0, 7.0, 1.4)):
        els += ball("Rauchwolke", x, y, z, r, DARK, step=1.0)
    for x, y, z in ((11.5, crater_y + 3.0, 6.0), (4.5, crater_y + 5.5, 10.0), (10.0, crater_y + 9.0, 9.5)):
        els.append(centered("Funke", x, y, z, 0.6, 0.6, 0.6, GLOW))
    return dict(kind="head", elements=els)


def magma_lava_orbit():
    els = []
    for x, z, deg, i in around(5, 6.2):
        y = 8 + (2.4, -2.0, 1.0, -1.4, 2.8)[i]
        r = (2.4, 1.8, 2.1, 1.6, 2.0)[i]
        # kantiger Brocken statt Kugel
        els.append(centered("Brocken", x, y, z, r * 1.8, r * 1.5, r * 1.6, DARK))
        els.append(centered("Brocken", x + 0.4, y + r * 0.6, z - 0.3, r * 1.3, r * 1.0, r * 1.2, DARK))
        els.append(centered("Brocken", x - 0.5, y - r * 0.5, z + 0.4, r * 1.1, r * 0.9, r * 1.4, DARK))
        # glühende Risse
        els.append(centered("Riss", x, y + r * 0.2, z - r * 0.85, r * 1.5, 0.35, 0.3, GLOW))
        els.append(centered("Riss", x - r * 0.5, y, z, 0.3, r * 1.2, r * 1.5, MAIN))
        els.append(centered("Riss", x, y - r * 0.55, z, r * 1.2, 0.3, r * 0.9, MAIN))
        els.append(centered("Schmelztropfen", x, y - r * 1.2, z, 0.5, 1.1, 0.5, GLOW))
    els += ring("Aschespur", 8, 8, 8, 7.4, 0.3, SECOND, count=16, size=0.5)
    return dict(kind="halo", elements=els)


def magma_obsidian_wings():
    els = [box("Plattenträger", [8, 7.2, 7.4], [11, 9.6, 8.6], DARK)]
    plates = ((70, 9, 3.4), (48, 13, 4.2), (24, 16, 4.8), (0, 15, 4.2), (-24, 11, 3.4), (-46, 7.5, 2.6))
    for angle, length, width in plates:
        rad = math.radians(angle)
        n = max(4, int(length / 1.1))
        for k in range(n):
            f = (k + 0.5) / n
            w = width * (1 - f * 0.82) + 0.5
            els.append(centered("Obsidianplatte", 9 + math.cos(rad) * length * f, 8.4 + math.sin(rad) * length * f, 8,
                                w, w, 0.9, DARK))
        # scharfe Spitze
        els.append(centered("Splitterspitze", 9 + math.cos(rad) * (length + 0.8), 8.4 + math.sin(rad) * (length + 0.8), 8,
                            0.7, 0.7, 0.6, SECOND))
        # Glutnaht entlang der Plattenkante
        for k in range(n):
            f = (k + 0.5) / n
            off = math.radians(angle + 12)
            els.append(centered("Glutnaht", 9 + math.cos(off) * length * f, 8.4 + math.sin(off) * length * f, 8.45,
                                0.7, 0.7, 0.4, GLOW if k % 2 else MAIN))
    els += ball("Kernglut", 9.4, 8.4, 8, 1.6, GLOW, step=0.6)
    return wings_spec(els)


def magma_cauldron():
    els = bowl("Kessel", 8, 12.0, 12.5, 4.6, DARK, step=0.9, squash=0.85)
    els += ring("Kesselrand", 8, 11.9, 12.5, 4.6, 1.1, X1, count=18, size=1.3)
    els += disc("Lavaoberfläche", 8, 11.6, 12.5, 4.0, 0.8, GLOW, axis="y", step=0.7)
    for x, y, z, r in ((6.4, 12.2, 11.5, 0.9), (9.6, 12.6, 13.4, 0.7), (8.2, 13.4, 11.0, 0.6)):
        els += ball("Blase", x, y, z, r, MAIN, step=0.5)
    for x, y, z, s in ((6.0, 15.0, 11.0, 0.6), (10.5, 16.5, 13.0, 0.5), (8.0, 18.0, 12.0, 0.45)):
        els.append(centered("Funke", x, y, z, s, s * 1.8, s, GLOW))
    for side in (-1, 1):
        els += chain("Henkel", bezier((8 + side * 4.4, 11.4, 12.5), (8 + side * 6.4, 13.6, 12.5), (8 + side * 4.4, 15.2, 12.5), 5),
                     [0.7] * 5, X1)
    els += [box("Fuß", [6.0, 6.6, 11.0], [7.2, 8.4, 12.2], X1), box("Fuß", [8.8, 6.6, 11.0], [10.0, 8.4, 12.2], X1),
            box("Fuß", [7.4, 6.6, 13.6], [8.6, 8.4, 14.8], X1),
            box("Traggestell", [6.6, 7.0, 8.4], [9.4, 13.0, 9.6], X1),
            box("Gurt", [4.4, 9.5, 8.0], [11.6, 10.7, 9.0], X2)]
    return dict(kind="back", elements=els)


def magma_golem():
    body = [box("Rumpf", [4.6, 2.2, 5.0], [11.4, 9.6, 11.0], DARK, north=DARK),
            box("Kopf", [5.4, 9.6, 5.4], [10.6, 13.4, 10.2], DARK, north=X1),
            box("Brustplatte", [5.4, 4.6, 4.2], [10.6, 8.4, 5.2], DARK),
            box("Fuß", [4.8, 1.0, 5.6], [7.4, 2.6, 9.6], DARK), box("Fuß", [8.6, 1.0, 5.6], [11.2, 2.6, 9.6], DARK)]
    # glühende Risse quer über den Körper
    for a, b in (([5.0, 6.4, 4.1], [11.0, 7.0, 4.4]), ([6.2, 3.2, 4.1], [9.0, 3.8, 4.4]),
                 ([4.4, 5.0, 6.0], [4.8, 5.6, 10.0]), ([11.2, 4.2, 6.0], [11.6, 4.8, 9.4]),
                 ([5.6, 9.4, 6.0], [10.4, 9.9, 9.6])):
        body.append(box("Riss", a, b, GLOW))
    for x, z, deg, i in around(6, 3.4, cx=8, cz=8.2):
        h = (3.0, 2.2, 2.6, 1.8, 2.8, 2.0)[i]
        body += stack("Gratstein", x, z, 9.6, [(h * 0.6, 1.2), (h * 0.4, 0.7)], DARK, tip=MAIN)
    body += ball("Herzglut", 8, 6.0, 8.0, 1.4, GLOW, step=0.6)
    body.append(centered("Kernschein", 8, 6.0, 4.4, 2.0, 2.0, 0.4, MAIN))
    flipper = [box("Schulter", [10.8, 6.6, 6.4], [13.8, 9.8, 10.0], DARK),
               box("Arm", [11.2, 2.6, 6.8], [13.4, 7.0, 9.6], DARK),
               box("Faust", [10.8, 1.2, 6.4], [13.8, 3.2, 10.0], DARK),
               box("Armriss", [11.0, 4.6, 6.6], [13.6, 5.1, 6.9], GLOW),
               box("Faustglut", [11.0, 1.6, 6.2], [13.6, 2.4, 6.5], MAIN)]
    return dict(kind="pet", parts={"body": body, "flipper_a": flipper, "flipper_b": mirror(flipper)},
                icon=body + flipper + mirror(flipper))


def magma_worm():
    head = [box("Panzerkopf", [4.8, 4.8, 2.6], [11.2, 11.2, 10.5], DARK, north=DARK),
            box("Stirnplatte", [5.4, 10.2, 3.2], [10.6, 12.2, 8.5], DARK),
            box("Kieferplatte", [5.4, 3.8, 3.2], [10.6, 5.4, 8.0], DARK)]
    # Schlund mit Zahnkranz
    head += disc("Schlund", 8, 8.0, 2.4, 2.8, 0.9, GLOW, axis="z", step=0.6)
    for x, z, deg, i in around(10, 3.4, cx=8, cz=8.0):
        rad = math.radians(deg)
        head.append(centered("Zahn", 8 + math.cos(rad) * 3.2, 8 + math.sin(rad) * 3.2, 1.9, 0.9, 0.9, 1.6, SECOND))
    for side in (-1, 1):
        head += chain("Kieferzange", bezier((8 + side * 4.6, 8.0, 3.0), (8 + side * 6.4, 8.2, 0.5), (8 + side * 3.4, 8.4, -1.6), 5),
                      taper(1.6, 0.7, 5), DARK, tip=SECOND)
        head.append(centered("Augenglut", 8 + side * 3.0, 10.4, 3.4, 1.2, 1.2, 0.5, GLOW))
    body = [box("Segment", [5.2, 5.2, 4.4], [10.8, 10.8, 11.2], DARK),
            box("Panzerring", [4.6, 4.6, 5.6], [11.4, 11.4, 7.4], DARK),
            box("Glutfuge", [5.0, 5.0, 7.5], [11.0, 11.0, 8.3], GLOW),
            box("Bauchglut", [6.0, 4.9, 5.0], [10.0, 5.4, 10.6], MAIN)]
    for x, z, deg, i in around(6, 3.0, cx=8, cz=8.0):
        rad = math.radians(deg)
        body.append(centered("Grat", 8 + math.cos(rad) * 3.2, 8 + math.sin(rad) * 3.2, 9.6, 1.0, 1.0, 1.8, SECOND))
    tail = [box("Schwanz", [5.8, 5.8, 3.5], [10.2, 10.2, 9.5], DARK),
            box("Endglut", [6.6, 6.6, 9.5], [9.4, 9.4, 10.6], GLOW)]
    for x, z, deg, i in around(5, 2.6, cx=8, cz=8.0):
        rad = math.radians(deg)
        tail.append(centered("Enddorn", 8 + math.cos(rad) * 3.0, 8 + math.sin(rad) * 3.0, 11.2, 1.0, 1.0, 2.4, SECOND))
    icon = head + shift(body, 0, 0, 5.2) + shift(body, 0, 0, 10.4) + shift(tail, 0, 0, 15.6)
    return dict(kind="pet", parts={"head": head, "body": body, "tail": tail}, icon=icon)


item("magma", "magma_volcano_hat", "HEAD", "NONE", "EPIC", "Vulkanhut", "Volcano Hat", magma_volcano_hat, scale=1.1,
     extras=[hexc(0x4A2A1A)])
item("magma", "magma_lava_orbit", "HEAD", "HALO", "LEGENDARY", "Glutbrocken", "Ember Chunks", magma_lava_orbit, scale=1.15)
item("magma", "magma_obsidian_wings", "BACK", "WINGS", "LEGENDARY", "Obsidianschwingen", "Obsidian Wings", magma_obsidian_wings, scale=1.2)
item("magma", "magma_cauldron", "BACK", "BACKPACK", "EPIC", "Lavakessel", "Lava Cauldron", magma_cauldron,
     extras=[(hexc(0x5A5A66), "metal"), hexc(0x3A2A20)])
item("magma", "magma_golem", "PET", "PENGUIN", "LEGENDARY", "Magmagolem", "Magma Golem", magma_golem,
     extras=[(hexc(0x2A1A16), "face")])
item("magma", "magma_worm", "PET", "SERPENT", "LEGENDARY", "Lavawurm", "Lava Worm", magma_worm)
