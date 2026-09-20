"""
Welten 16-20: Gift, Regenbogen, Leere, Messing, Spuk.
Konventionen siehe worlds_a.py, Hilfsformen aus worlds_c.py.
"""
import math

from worldkit import (densify, MAIN, SECOND, FACE, GLOW, DARK, SLEEP, LIGHT, X1, X2, X3, X4, X5,
                      box, centered, stack, chain, bezier, taper, ball, disc, ring, around, blade,
                      mirror, shift, rot, cape_box, CAPE_EDGE, CAPE_OUTER, hexc, gradient_name as nm)
from worlds_c import band, sail, bowl, blossom, orbit

T_TOP = 14.4
ITEMS = []


def item(theme, cid, slot, typ, rarity, de, en, build, **extra):
    ITEMS.append(dict(theme=theme, id=cid, slot=slot, type=typ, rarity=rarity,
                      de=nm(theme, de, rarity == "ULTRA"), en=nm(theme, en, rarity == "ULTRA"), build=build, **extra))


def wings_spec(els):
    return dict(kind="wings", parts={"wing_a": els, "wing_b": mirror(els)}, icon=shift(els, 2) + shift(mirror(els), -2))


def gear(name, cx, cy, cz, r, teeth, thick, cell, hub, axis="z", phase=0.0):
    """Zahnrad mit Nabe, Speichen und Zähnen."""
    out = disc(name, cx, cy, cz, r, thick, cell, axis=axis, step=0.8)
    for i in range(teeth):
        a = phase + math.tau * i / teeth
        dx, dy = math.cos(a) * (r + 0.7), math.sin(a) * (r + 0.7)
        if axis == "z":
            out.append(centered("Zahn", cx + dx, cy + dy, cz, 1.3, 1.3, thick, cell))
        else:
            out.append(centered("Zahn", cx + dx, cy, cz + dy, 1.3, thick, 1.3, cell))
    if axis == "z":
        out += disc("Nabe", cx, cy, cz, r * 0.32, thick * 1.6, hub, axis="z", step=0.6)
    else:
        out += disc("Nabe", cx, cy, cz, r * 0.32, thick * 1.6, hub, axis="y", step=0.6)
    for i in range(4):
        a = phase + math.tau * i / 4 + 0.4
        for k in range(1, 4):
            d = r * (0.35 + k * 0.18)
            if axis == "z":
                out.append(centered("Speiche", cx + math.cos(a) * d, cy + math.sin(a) * d, cz, 0.9, 0.9, thick * 1.1, hub))
            else:
                out.append(centered("Speiche", cx + math.cos(a) * d, cy, cz + math.sin(a) * d, 0.9, thick * 1.1, 0.9, hub))
    return out


# =========================================================================== GIFT
def toxic_gas_mask():
    t = T_TOP
    els = [box("Maskenkörper", [3.2, 2.0, -0.4], [12.8, 9.5, 3.0], X1),
           box("Maskenrand", [2.6, 1.6, 0.2], [13.4, 10.0, 2.4], X2),
           box("Stirnriemen", [2.8, 9.5, 0.4], [13.2, 11.2, 2.2], X2)]
    els += band("Kopfriemen", 7.0, 8.6, 7.8, X2, thick=1.2)
    els += band("Kopfriemen oben", t - 3.0, t - 1.6, 7.6, X2, thick=1.2)
    for side in (-1, 1):
        # Sichtglas
        els += disc("Sichtglas", 8 + side * 3.2, 6.6, -0.5, 2.5, 0.9, GLOW, axis="z", step=0.6)
        els += ring("Glasfassung", 8 + side * 3.2, 6.6, -0.2, 2.9, 0.9, X1, count=14, size=1.1, axis="xy")
    # Filterpatrone vorne unten
    els += disc("Filter", 8, 3.2, -2.4, 2.4, 2.6, X1, axis="z", step=0.7)
    els += ring("Filterrippe", 8, 3.2, -3.4, 2.4, 0.6, X2, count=12, size=0.8, axis="xy")
    els.append(centered("Filtergitter", 8, 3.2, -3.9, 2.4, 2.4, 0.4, DARK))
    for k in range(3):
        els.append(centered("Schlauch", 8 - 3.0 - k * 0.3, 3.4 - k * 0.5, -1.0 + k * 1.4, 1.0, 1.0, 1.4, DARK))
    els.append(centered("Ventil", 8, 1.6, 0.0, 2.0, 1.2, 1.6, X2))
    for x, y, z, s in ((4.0, 8.0, -1.5, 0.6), (12.0, 7.0, -1.2, 0.5), (8.0, 1.0, -2.0, 0.5)):
        els.append(centered("Giftschwaden", x, y, z, s, s, s, GLOW))
    return dict(kind="head", elements=els)


def toxic_flask_halo():
    els = []
    for x, z, deg, i in around(5, 5.6):
        y = 8 + (2.2, -1.6, 0.8, -2.4, 1.4)[i]
        h = (3.4, 2.6, 3.0, 2.4, 2.8)[i]
        cell = (MAIN, SECOND, GLOW, SECOND, MAIN)[i]
        # Rundkolben mit Hals und Korken
        els += ball("Kolben", x, y, z, h * 0.5, cell, step=0.55)
        els.append(centered("Flaschenhals", x, y + h * 0.5 + 0.8, z, 0.9, 1.6, 0.9, LIGHT))
        els.append(centered("Korken", x, y + h * 0.5 + 1.9, z, 1.2, 0.9, 1.2, X1))
        els.append(centered("Füllstand", x, y - h * 0.15, z, h * 0.85, 0.5, h * 0.85, GLOW))
        els.append(centered("Etikett", x, y, z - h * 0.5, 1.2, 1.4, 0.3, LIGHT))
        for k in range(3):
            els.append(centered("Blase", x + math.sin(k * 2.1) * 0.6, y + 0.4 + k * 0.6, z + math.cos(k * 2.1) * 0.5,
                                0.4, 0.4, 0.4, LIGHT))
    els += ring("Dunstring", 8, 8, 8, 6.8, 0.3, GLOW, count=16, size=0.5)
    return dict(kind="halo", elements=els)


def toxic_dragonfly_wings():
    els = []
    # vier schmale, lange Libellenflügel mit Adernetz
    for angle, length, width, cell in ((34, 20, 2.6, MAIN), (10, 23, 3.0, MAIN),
                                       (-14, 19, 2.4, SECOND), (-38, 14, 2.0, SECOND)):
        rad = math.radians(angle)
        nx, ny = -math.sin(rad), math.cos(rad)
        n = int(length / 0.8)
        for k in range(n):
            f = (k + 0.5) / n
            w = width * math.sin(math.pi * min(1.0, f * 1.15)) ** 0.5
            bx, by = 9 + math.cos(rad) * length * f, 8 + math.sin(rad) * length * f
            steps = max(1, int(w / 0.75))
            for s in range(-steps, steps + 1):
                g = s / max(1, steps)
                cell_here = GLOW if abs(g) > 0.8 else cell
                els.append(centered("Flügelhaut", bx + nx * g * w, by + ny * g * w, 8.0, 0.85, 0.85, 0.4, cell_here))
            if k % 3 == 0:
                els.append(centered("Querader", bx, by, 8.35, w * 0.9, w * 0.9, 0.25, LIGHT))
        els += chain("Längsader", [(9 + math.cos(rad) * length * k / 9, 8 + math.sin(rad) * length * k / 9, 8.4)
                                   for k in range(10)], taper(0.8, 0.4, 10), LIGHT)
    els += ball("Flügelgelenk", 9.4, 8, 8.5, 1.6, X1, step=0.6)
    return wings_spec(els)


def toxic_barrels():
    els = []
    for cx, cy, cz, r, h, cell in ((5.6, 6.5, 11.5, 2.6, 6.5, X1), (10.6, 7.5, 12.0, 2.4, 6.0, X1),
                                   (8.0, 12.5, 12.5, 2.2, 5.0, X2)):
        els += disc("Fass", cx, cy, cz, r, h, cell, axis="y", step=0.9)
        for k in (-0.34, 0.0, 0.34):
            els += ring("Fassband", cx, cy + h * k, cz, r + 0.15, 0.7, X2 if cell == X1 else X1, count=12, size=1.0)
        els += disc("Deckel", cx, cy + h / 2, cz, r * 0.85, 0.6, X2, axis="y", step=0.8)
        els.append(centered("Gefahrzeichen", cx, cy, cz - r, r * 0.9, r * 0.9, 0.3, GLOW))
        els.append(centered("Verschluss", cx + r * 0.4, cy + h / 2 + 0.5, cz, 1.0, 0.8, 1.0, DARK))
    # auslaufende Brühe
    for x, y, z, s in ((4.0, 9.6, 10.0, 0.9), (12.4, 10.8, 11.0, 0.7), (8.0, 15.6, 12.0, 0.8), (6.0, 16.6, 11.0, 0.6)):
        els += ball("Giftblase", x, y, z, s, GLOW, step=0.5)
    els += [box("Traggestell", [6.6, 5.0, 8.4], [9.4, 14.0, 9.8], X2),
            box("Gurt", [4.2, 9.0, 8.0], [11.8, 10.4, 9.0], DARK)]
    return dict(kind="back", elements=els)


def toxic_frog():
    els = ball("Froschleib", 8, 5.0, 8, 4.2, MAIN, step=0.85, squash=0.8)
    els.append(box("Maul", [4.4, 4.2, 3.6], [11.6, 5.0, 4.2], DARK))
    els.append(box("Gesicht", [4.8, 5.2, 3.7], [11.2, 8.0, 4.0], X1, north=X1))
    for side in (-1, 1):
        els += ball("Augenhügel", 8 + side * 2.4, 8.4, 5.4, 1.9, MAIN, step=0.6)
        els += ball("Auge", 8 + side * 2.4, 8.8, 4.4, 1.2, GLOW, step=0.5)
        els.append(centered("Pupille", 8 + side * 2.4, 8.8, 3.5, 0.7, 1.1, 0.4, DARK))
        # Hinterbein angewinkelt
        els.append(centered("Oberschenkel", 8 + side * 4.0, 3.4, 9.6, 2.6, 2.6, 3.4, MAIN))
        els.append(centered("Unterschenkel", 8 + side * 4.6, 2.0, 8.0, 1.6, 1.6, 3.0, MAIN))
        els.append(centered("Fuß", 8 + side * 4.6, 1.2, 5.6, 2.6, 1.0, 2.6, SECOND))
        els.append(centered("Vorderpfote", 8 + side * 2.2, 1.2, 4.6, 1.8, 1.0, 2.0, SECOND))
    for x, y, z, r in ((5.4, 7.0, 9.6, 1.1), (10.8, 6.4, 10.4, 0.9), (8.2, 8.0, 11.4, 1.0), (6.2, 5.0, 11.0, 0.8)):
        els.append(centered("Giftfleck", x, y, z, r * 2, r * 1.2, r * 2, SECOND))
    els.append(centered("Kehlsack", 8, 3.6, 4.6, 3.4, 2.2, 1.4, SECOND))
    return dict(kind="pet", elements=shift(els, 0, 1.0, 0))


def toxic_fly():
    body = [box("Hinterleib", [6.0, 6.0, 8.5], [10.0, 9.5, 13.5], MAIN),
            box("Brust", [5.6, 6.2, 5.0], [10.4, 10.0, 8.5], DARK),
            box("Kopf", [5.8, 6.6, 2.2], [10.2, 10.4, 5.0], DARK, north=X1)]
    for k, z in enumerate((9.2, 10.6, 12.0)):
        body.append(centered("Ringel", 8, 7.8, z, 4.4, 1.0, 0.9, SECOND))
    for side in (-1, 1):
        body += ball("Facettenauge", 8 + side * 2.0, 8.8, 3.0, 1.8, GLOW, step=0.6)
        body += chain("Bein", [(8 + side * 2.4, 6.2, 6.5), (8 + side * 4.0, 4.6, 7.4), (8 + side * 4.6, 3.0, 8.8)],
                      [0.7, 0.6, 0.5], DARK)
        body.append(centered("Fühler", 8 + side * 1.0, 10.8, 2.6, 0.5, 1.6, 0.5, SECOND))
    body.append(centered("Stachel", 8, 7.6, 14.2, 0.8, 0.8, 2.0, GLOW))
    wing = [box("Flügelansatz", [8, 9.4, 6.0], [10, 10.0, 8.0], DARK)]
    for angle, length in ((24, 9.0), (-4, 7.5)):
        rad = math.radians(angle)
        n = 10
        for k in range(n):
            f = (k + 0.5) / n
            w = 2.0 * math.sin(math.pi * min(1.0, f * 1.2)) ** 0.6
            bx, by = 9.5 + math.cos(rad) * length * f, 9.6 + math.sin(rad) * length * f
            wing.append(centered("Flügelhaut", bx, by, 8.0, w * 1.6, w * 0.8, 0.4, LIGHT))
    return dict(kind="pet", parts={"body": body, "wing_a": wing, "wing_b": mirror(wing)},
                icon=body + shift(wing, 1.5, 0, 0) + shift(mirror(wing), -1.5, 0, 0))


item("toxic", "toxic_gas_mask", "HEAD", "NONE", "EPIC", "Gasmaske", "Gas Mask", toxic_gas_mask,
     extras=[(hexc(0x3A4A2A), "metal"), (hexc(0x1E2A16), "metal")])
item("toxic", "toxic_flask_halo", "HEAD", "HALO", "LEGENDARY", "Giftfläschchen", "Toxic Flasks", toxic_flask_halo, scale=1.15,
     extras=[hexc(0x8A6A3A)])
item("toxic", "toxic_dragonfly_wings", "BACK", "WINGS", "LEGENDARY", "Libellenflügel", "Dragonfly Wings", toxic_dragonfly_wings, scale=1.2,
     extras=[(hexc(0x3A4A2A), "metal")])
item("toxic", "toxic_barrels", "BACK", "BACKPACK", "EPIC", "Giftfässer", "Toxic Barrels", toxic_barrels,
     extras=[(hexc(0x4A5A2A), "metal"), (hexc(0x8A8A3A), "metal")])
item("toxic", "toxic_frog", "PET", "MUSHROOM", "EPIC", "Giftfrosch", "Poison Frog", toxic_frog,
     extras=[(hexc(0x5AD21E), "face")])
item("toxic", "toxic_fly", "PET", "BEE", "LEGENDARY", "Mutantenfliege", "Mutant Fly", toxic_fly,
     extras=[(hexc(0x1A2A12), "face")])


# =========================================================================== REGENBOGEN
BOW = (MAIN, SECOND, X1, X2, X3, GLOW, LIGHT)


def rainbow_arc():
    els = []
    # Regenbogen als echter Halbbogen mit sieben Streifen
    for k, cell in enumerate(BOW):
        r = 7.6 - k * 0.95
        n = 26
        for i in range(n + 1):
            a = math.pi * i / n
            els.append(centered("Bogen", 8 + math.cos(a) * r, 4.2 + math.sin(a) * r, 8, 1.2, 1.2, 1.5, cell))
    for cx, cy, cz in ((8 - 7.2, 3.4, 8), (8 + 7.2, 3.4, 8)):
        for dx, dy, dz, r in ((0, 0, 0, 2.6), (-2.0, -0.4, 0.4, 1.8), (2.0, -0.4, -0.4, 1.8), (0, 1.4, 0.6, 1.9)):
            els += ball("Wolke", cx + dx, cy + dy, cz + dz, r, LIGHT, step=1.0)
    for x, y, s in ((3.0, 12.0, 0.7), (13.0, 11.0, 0.6), (8.0, 14.5, 0.8)):
        els.append(centered("Funkeln", x, y, 8, s, s * 2.6, s, LIGHT))
        els.append(centered("Funkeln", x, y, 8, s * 2.6, s, s, LIGHT))
    return dict(kind="halo", elements=els)


def rainbow_afro():
    t = T_TOP
    els = band("Stirnband", t - 3.2, t - 1.8, 7.7, X4, thick=1.3)
    # große Lockenkugel: viele kleine Büschel auf konzentrischen Kugelschalen
    radius, layers = 8.0, 9
    for k in range(layers):
        phi = math.pi * (k + 0.5) / layers          # 0 = unten, pi = oben
        ry = -math.cos(phi) * radius * 0.92
        rr = math.sin(phi) * radius
        count = max(4, int(rr * 1.9))
        for x, z, deg, i in around(count, rr, cx=8, cz=8, phase=k * 0.55):
            cell = BOW[(i + k * 3) % len(BOW)]
            els.append(centered("Locke", x, t - 1.0 + ry + radius * 0.35, z, 2.4, 2.4, 2.4, cell))
            if i % 2 == 0:
                els.append(centered("Locke", x * 0.92 + 0.64, t - 1.0 + ry + radius * 0.35 + 1.0, z * 0.92 + 0.64,
                                    1.8, 1.8, 1.8, BOW[(i + k) % len(BOW)]))
    els.append(centered("Haarspange", 8, t - 2.6, 1.2, 2.6, 1.4, 0.8, GLOW))
    return dict(kind="head", elements=els)


def rainbow_wings():
    els = []
    # sieben gestaffelte Farbschichten, jede etwas kürzer und weiter vorne
    for k, cell in enumerate(BOW):
        scale = 1.0 - k * 0.11
        quills = tuple((a, l * scale) for a, l in
                       ((68, 11), (52, 15), (36, 19), (20, 22), (4, 23), (-14, 21), (-32, 17), (-50, 12)))
        els += sail((9, 8), quills, cell, z=8.0 + k * 0.42, thick=0.42, sag=0.14, step=0.5)
    for angle, length in ((68, 11), (52, 15), (36, 19), (20, 22), (4, 23), (-14, 21), (-32, 17), (-50, 12)):
        rad = math.radians(angle)
        n = max(6, int(length / 1.2))
        els += chain("Lichtkiel", [(9 + math.cos(rad) * length * 0.92 * k / (n - 1),
                                    8 + math.sin(rad) * length * 0.92 * k / (n - 1), 7.7)
                                   for k in range(n)], taper(0.9, 0.35, n), LIGHT)
    els += ball("Prismenkern", 9.6, 8, 9.4, 2.2, LIGHT, step=0.7)
    return wings_spec(els)


def rainbow_cape():
    top = [cape_box("Wolkenkragen", [1.8, 14.4, 6.8], [14.2, 18.0, 9.4], CAPE_EDGE),
           cape_box("Wolkenbausch", [3.0, 17.0, 6.6], [13.0, 19.6, 9.6], CAPE_EDGE),
           cape_box("Wolkenbausch", [1.2, 16.2, 7.0], [5.2, 18.6, 9.2], CAPE_EDGE),
           cape_box("Wolkenbausch", [10.8, 16.2, 7.0], [14.8, 18.6, 9.2], CAPE_EDGE)]
    bottom = []
    for i in range(7):
        x = 3 + i * 1.45
        drop = 1.2 + (i % 3) * 1.1
        bottom.append(cape_box("Zacke", [x, 10 - drop, 7.6], [x + 1.45, 10, 8.4], CAPE_OUTER))
        bottom.append(cape_box("Stern", [x + 0.3, 10 - drop - 1.4, 7.7], [x + 1.1, 10 - drop - 0.4, 8.3], CAPE_EDGE))
    return dict(kind="cape", top=top, bottom=bottom)


def rainbow_pegasus():
    body = ball("Leib", 8, 7.0, 9.5, 3.8, LIGHT, step=0.9, squash=0.85)
    body.append(box("Hals", [6.6, 8.0, 4.6], [9.4, 12.6, 7.6], LIGHT))
    body.append(box("Kopf", [6.2, 11.0, 1.8], [9.8, 14.4, 5.6], LIGHT, north=X4))
    body.append(box("Schnauze", [6.6, 10.6, 0.8], [9.4, 12.6, 2.2], LIGHT))
    body.append(box("Nüstern", [6.9, 11.0, 0.5], [9.1, 11.6, 0.9], X5))
    for side in (-1, 1):
        body.append(centered("Ohr", 8 + side * 1.2, 15.0, 3.6, 0.9, 1.6, 0.9, LIGHT))
        body.append(centered("Vorderbein", 8 + side * 2.0, 3.2, 7.0, 1.9, 6.0, 1.9, LIGHT))
        body.append(centered("Hinterbein", 8 + side * 2.2, 3.2, 11.8, 2.1, 6.0, 2.1, LIGHT))
        body.append(centered("Huf", 8 + side * 2.0, 0.8, 7.0, 2.1, 1.2, 2.1, X2))
        body.append(centered("Huf", 8 + side * 2.2, 0.8, 11.8, 2.3, 1.2, 2.3, X2))
    # Mähne und Schweif in Regenbogenfarben
    for k, cell in enumerate(BOW):
        f = k / (len(BOW) - 1)
        body.append(centered("Mähne", 8, 13.8 - f * 4.4, 6.4 + f * 1.4, 1.6, 1.9, 1.9, cell))
        body.append(centered("Schweif", 8, 8.4 - f * 2.4, 13.4 + f * 1.1, 1.8, 2.0, 1.8, cell))
    body += chain("Horn", [(8, 14.6, 3.2), (8, 16.0, 2.9), (8, 17.2, 2.7)], [(1.0, 1.2, 1.0), (0.8, 1.2, 0.8), (0.5, 1.2, 0.5)],
                  X2, tip=LIGHT)
    wing = []
    for k, cell in enumerate(BOW[:5]):
        scale = 1.0 - k * 0.14
        quills = tuple((a, l * scale) for a, l in ((58, 6), (38, 8.5), (18, 10.5), (-4, 10), (-26, 7.5)))
        wing += sail((9, 9), quills, cell, z=8.0 + k * 0.4, thick=0.4, sag=0.12, step=0.45)
    return dict(kind="pet", parts={"body": body, "wing_a": wing, "wing_b": mirror(wing)},
                icon=body + shift(wing, 2, 1, 0) + shift(mirror(wing), -2, 1, 0))


def rainbow_parrot():
    body = [box("Körper", [6.0, 4.0, 6.5], [10.0, 9.5, 11.0], MAIN),
            box("Bauch", [6.4, 4.2, 5.6], [9.6, 8.0, 6.8], X2),
            box("Fuß", [6.4, 3.0, 6.0], [7.6, 4.2, 7.6], X4), box("Fuß", [8.4, 3.0, 6.0], [9.6, 4.2, 7.6], X4)]
    for k, cell in enumerate((X1, X3, GLOW, SECOND)):
        body.append(centered("Schwanzfeder", 8 + (k - 1.5) * 1.1, 6.2 - k * 0.2, 13.0 + k * 0.8, 1.1, 1.0, 5.0 + k * 1.2, cell))
    head = [box("Kopf", [5.8, 9.0, 5.6], [10.2, 13.2, 10.2], SECOND, north=FACE),
            box("Schnabel oben", [6.8, 10.2, 3.6], [9.2, 12.0, 5.8], X4),
            box("Schnabelspitze", [7.2, 8.8, 3.4], [8.8, 10.4, 5.0], X4),
            box("Schnabel unten", [7.1, 9.6, 4.4], [8.9, 10.4, 5.8], X5),
            box("Wange", [5.6, 9.6, 5.4], [10.4, 11.4, 6.0], LIGHT)]
    for k, cell in enumerate((X1, X3, GLOW)):
        head.append(centered("Haube", 8, 13.6 + k * 1.0, 8.2 - k * 0.6, 1.4 - k * 0.2, 1.6, 2.6 - k * 0.5, cell))
    wing = []
    for k, cell in enumerate((MAIN, X1, X3, GLOW)):
        wing.append(centered("Flügelfeder", 8.6 + k * 0.25, 7.4 - k * 0.7, 8.6 + k * 0.5,
                             1.0, 5.4 - k * 0.6, 5.2 - k * 0.4, cell))
    icon = body + shift(head, 0, 1.5, 0.25) + shift(wing, 2.5, 1.5, 0.5) + shift(mirror(wing), -2.5, 1.5, 0.5)
    return dict(kind="pet", parts={"body": body, "head": head, "wing_a": wing, "wing_b": mirror(wing)}, icon=icon)


RAINBOW_EXTRAS = [hexc(0xFF3C3C), hexc(0xFFA02A), hexc(0xFFE83A), hexc(0x3ADF6A), hexc(0x3A6AFF)]
RAINBOW_FACE = [hexc(0xFF3C3C), hexc(0xFFA02A), hexc(0xFFE83A), (hexc(0x3ADF6A), "face"), hexc(0x3A6AFF)]

item("rainbow", "rainbow_arc", "HEAD", "HALO", "ULTRA", "Regenbogen", "Rainbow Arc", rainbow_arc, scale=1.35,
     extras=RAINBOW_EXTRAS)
item("rainbow", "rainbow_afro", "HEAD", "NONE", "EPIC", "Regenbogen-Afro", "Rainbow Afro", rainbow_afro, scale=1.1,
     extras=RAINBOW_EXTRAS)
item("rainbow", "rainbow_wings", "BACK", "WINGS", "ULTRA", "Prismenschwingen", "Prism Wings", rainbow_wings, scale=1.35,
     extras=RAINBOW_EXTRAS)
item("rainbow", "rainbow_cape", "BACK", "NONE", "EPIC", "Regenbogenmantel", "Rainbow Cloak", rainbow_cape, scale=1.05)
item("rainbow", "rainbow_pegasus", "PET", "DRAGON", "ULTRA", "Regenbogen-Pegasus", "Rainbow Pegasus", rainbow_pegasus, scale=1.25,
     extras=[hexc(0xFF3C3C), hexc(0xFFA02A), hexc(0xFFE83A), (hexc(0xFFF8FF), "face"), hexc(0xFF9AC0)])
item("rainbow", "rainbow_parrot", "PET", "OWL", "LEGENDARY", "Papagei", "Parrot", rainbow_parrot,
     extras=[hexc(0xFF3C3C), hexc(0xFFE83A), hexc(0x3ADF6A), hexc(0xE8A02A), hexc(0x8A5A1A)])


# =========================================================================== LEERE
def void_black_hole():
    els = ball("Ereignishorizont", 8, 8, 8, 3.0, DARK, step=0.7)
    # Akkretionsscheibe: flache, gekippte Spirale
    for k in range(4):
        r0, r1 = 3.8 + k * 1.1, 4.4 + k * 1.1
        cell = (SECOND, MAIN, GLOW, LIGHT)[k]
        for i in range(30):
            a = math.tau * i / 30 + k * 0.5
            r = r0 + (r1 - r0) * (i / 30)
            els.append(centered("Scheibe", 8 + math.cos(a) * r, 8 + math.sin(a) * r * 0.22, 8 + math.sin(a) * r,
                                1.3 - k * 0.15, 0.7, 1.3 - k * 0.15, cell))
    els += ring("Lichtring", 8, 8, 8, 3.5, 0.6, LIGHT, count=22, size=0.8, axis="xy")
    # Jets nach oben und unten
    for direction in (-1, 1):
        for k in range(5):
            els.append(centered("Strahl", 8, 8 + direction * (3.4 + k * 1.5), 8, 1.4 - k * 0.2, 1.5, 1.4 - k * 0.2,
                                GLOW if k < 2 else LIGHT))
    for x, z, deg, i in around(7, 8.4):
        els.append(centered("Verschlungener Stein", x, 8 + math.sin(math.radians(deg * 3)) * 2.2, z, 1.0, 1.0, 1.0, SECOND))
    return dict(kind="halo", elements=els)


def void_third_eye():
    t = T_TOP
    els = band("Stirnband", t - 3.4, t - 1.4, 7.7, X1, thick=1.3)
    els += band("Bandkante", t - 1.4, t - 0.9, 7.9, MAIN, thick=1.4)
    # großes Auge auf der Stirn
    els += disc("Augenfassung", 8, t - 2.4, 0.6, 2.9, 1.2, X1, axis="z", step=0.6)
    els += disc("Lederhaut", 8, t - 2.4, 0.1, 2.3, 0.8, LIGHT, axis="z", step=0.5)
    els += disc("Iris", 8, t - 2.4, -0.3, 1.5, 0.7, GLOW, axis="z", step=0.5)
    els.append(centered("Pupille", 8, t - 2.4, -0.7, 0.7, 1.9, 0.5, DARK))
    for x, z, deg, i in around(8, 3.6, cx=8, cz=t - 2.4):
        els.append(centered("Wimper", x, z, 0.4, 0.7, 0.7, 1.0, X1))
    # schwebende Runen über dem Kopf
    for x, z, deg, i in around(5, 5.4, phase=0.3):
        y = t + 2.5 + (i % 3) * 1.4
        els.append(centered("Rune", x, y, z, 1.4, 0.5, 0.5, GLOW))
        els.append(centered("Rune", x, y, z, 0.5, 1.4, 0.5, GLOW))
        els.append(centered("Runenschatten", x, y - 0.6, z, 1.0, 0.4, 1.0, SECOND))
    return dict(kind="head", elements=els)


def void_tentacle_wings():
    els = ball("Ursprung", 9.5, 8, 8, 2.4, DARK, step=0.7)
    for angle, length in ((66, 15), (44, 19), (20, 22), (-4, 21), (-28, 17), (-50, 12)):
        rad = math.radians(angle)
        n = max(8, int(length / 1.4))
        pts = []
        for k in range(n):
            f = k / (n - 1)
            curl = math.sin(f * 2.6) * 3.4 * (1 if angle > 10 else -1)
            pts.append((9.5 + math.cos(rad) * length * f - math.sin(rad) * curl * f,
                        8 + math.sin(rad) * length * f + math.cos(rad) * curl * f,
                        8 + math.sin(f * 3.0) * 0.8))
        els += chain("Tentakel", pts, taper(2.6, 0.5, n), MAIN, tip=GLOW)
        for k in range(1, n - 1, 2):
            x, y, z = pts[k]
            f = k / (n - 1)
            els.append(centered("Saugnapf", x, y, z - (2.6 - 2.1 * f) * 0.6, 0.8, 0.8, 0.4, SECOND))
        for k in range(0, n, 3):
            x, y, z = pts[k]
            els.append(centered("Glutader", x, y, z + 0.6, 0.6, 0.6, 0.35, GLOW))
    return wings_spec(els)


def void_portal():
    els = []
    for k in range(4):
        r = 6.4 - k * 0.9
        cell = (X1, MAIN, SECOND, GLOW)[k]
        els += ring("Portalring", 8, 9, 12.5 + k * 0.4, r, 1.1 - k * 0.15, cell, count=int(r * 4), size=1.4 - k * 0.2, axis="xy")
    els += disc("Portalfläche", 8, 9, 13.6, 3.6, 0.7, LIGHT, axis="z", step=0.7)
    els += disc("Portaltiefe", 8, 9, 14.1, 2.4, 0.6, DARK, axis="z", step=0.6)
    for x, z, deg, i in around(6, 7.4, cx=8, cz=9):
        els.append(centered("Ankerstein", x, z, 12.4, 1.6, 1.6, 1.8, X1))
        els.append(centered("Ankerglut", x, z, 11.6, 0.8, 0.8, 0.5, GLOW))
    for x, y, s in ((4.5, 14.5, 0.7), (12.0, 15.2, 0.6), (8.0, 16.8, 0.8)):
        els.append(centered("Partikel", x, y, 13.0, s, s, s, GLOW))
    els.append(box("Halterung", [6.8, 7.5, 9.0], [9.2, 10.5, 12.4], X1))
    return dict(kind="back", elements=els)


def void_worm():
    head = []
    # Trichtermaul aus mehreren Ringen
    for k in range(5):
        r = 5.4 - k * 0.75
        head += ring("Maulring", 8, 8, 0.8 + k * 1.5, r, 1.4, DARK if k % 2 else MAIN, count=int(r * 3.4), size=1.7, axis="xy")
    head += disc("Schlund", 8, 8, 6.4, 2.6, 1.0, GLOW, axis="z", step=0.6)
    for x, z, deg, i in around(12, 5.0, cx=8, cz=8):
        head.append(centered("Zahn", x, z, 0.6, 0.9, 0.9, 2.2, LIGHT))
    for side in (-1, 1):
        for k, y in enumerate((10.4, 8.4, 6.4)):
            head.append(centered("Auge", 8 + side * 5.2, y, 4.0 + k * 0.4, 1.0, 1.0, 1.0, GLOW))
    head.append(box("Nackenpanzer", [4.6, 4.6, 6.2], [11.4, 11.4, 10.5], DARK))
    body = [box("Segment", [5.0, 5.0, 4.2], [11.0, 11.0, 11.2], MAIN),
            box("Panzerring", [4.4, 4.4, 5.4], [11.6, 11.6, 7.2], DARK)]
    for x, z, deg, i in around(8, 3.6, cx=8, cz=8):
        rad = math.radians(deg)
        body.append(centered("Dorn", 8 + math.cos(rad) * 4.0, 8 + math.sin(rad) * 4.0, 8.6, 1.1, 1.1, 2.4, SECOND))
        body.append(centered("Glutpunkt", 8 + math.cos(rad) * 4.4, 8 + math.sin(rad) * 4.4, 8.6, 0.6, 0.6, 0.8, GLOW))
    tail = [box("Schwanz", [5.6, 5.6, 3.2], [10.4, 10.4, 9.5], MAIN)]
    for x, z, deg, i in around(6, 2.8, cx=8, cz=8):
        rad = math.radians(deg)
        tail += chain("Enddorn", [(8 + math.cos(rad) * 3.0, 8 + math.sin(rad) * 3.0, 9.5),
                                  (8 + math.cos(rad) * 4.4, 8 + math.sin(rad) * 4.4, 11.5),
                                  (8 + math.cos(rad) * 5.2, 8 + math.sin(rad) * 5.2, 13.2)],
                      [1.2, 0.9, 0.6], DARK, tip=GLOW)
    icon = head + shift(body, 0, 0, 5.2) + shift(body, 0, 0, 10.4) + shift(tail, 0, 0, 15.6)
    return dict(kind="pet", parts={"head": head, "body": body, "tail": tail}, icon=icon)


def void_watcher():
    els = ball("Kernmasse", 8, 8, 8, 3.4, DARK, step=0.8)
    eyes = ((8, 8, 3.6, 2.6), (4.6, 10.2, 6.0, 1.7), (11.6, 9.6, 6.4, 1.5),
            (5.4, 5.6, 6.8, 1.4), (10.8, 5.2, 6.2, 1.3), (8.2, 12.4, 7.0, 1.5))
    for cx, cy, cz, r in eyes:
        dx, dy, dz = cx - 8, cy - 8, cz - 8
        d = math.sqrt(dx * dx + dy * dy + dz * dz) or 1
        els += ball("Augapfel", cx, cy, cz, r, X1, step=0.5)
        els += ball("Iris", cx - dx / d * r * 0.6, cy - dy / d * r * 0.6, cz - dz / d * r * 0.6, r * 0.6, GLOW, step=0.4)
        els.append(centered("Pupille", cx - dx / d * r * 0.95, cy - dy / d * r * 0.95, cz - dz / d * r * 0.95,
                            r * 0.5, r * 0.5, r * 0.5, DARK))
        els.append(centered("Lid", cx, cy + r * 0.85, cz - dz / d * r * 0.4, r * 1.7, r * 0.5, r * 1.7, MAIN))
    for x, z, deg, i in around(6, 4.6, cx=8, cz=9):
        els += chain("Ranke", [(x, 5.0, z), (x * 0.9 + 0.8, 2.6, z * 0.9 + 0.8), (x * 0.8 + 1.6, 0.8, z * 0.8 + 1.6)],
                     [1.0, 0.8, 0.5], SECOND, tip=GLOW)
    return dict(kind="pet", elements=els)


item("void", "void_black_hole", "HEAD", "HALO", "ULTRA", "Schwarzes Loch", "Black Hole", void_black_hole, scale=1.35)
item("void", "void_third_eye", "HEAD", "NONE", "LEGENDARY", "Drittes Auge", "Third Eye", void_third_eye,
     extras=[(hexc(0x2A1040), "metal")])
item("void", "void_tentacle_wings", "BACK", "WINGS", "ULTRA", "Leerenranken", "Void Tendrils", void_tentacle_wings, scale=1.3)
item("void", "void_portal", "BACK", "BACKPACK", "LEGENDARY", "Portalring", "Portal Ring", void_portal,
     extras=[(hexc(0x2A1040), "metal")])
item("void", "void_worm", "PET", "SERPENT", "ULTRA", "Leerenwurm", "Void Worm", void_worm, scale=1.2)
item("void", "void_watcher", "PET", "GHOST", "LEGENDARY", "Wächteraugen", "Watcher", void_watcher,
     extras=[hexc(0xF0E8FF)])


# =========================================================================== MESSING
def steampunk_top_hat():
    t = T_TOP
    els = disc("Krempe", 8, t + 0.4, 8, 7.6, 1.0, X1, axis="y", step=0.9)
    els += disc("Krempenrand", 8, t + 1.0, 8, 7.6, 0.5, MAIN, axis="y", step=0.9)
    for k in range(9):
        els += disc("Zylinder", 8, t + 1.6 + k * 1.15, 8, 5.4 + math.sin(k * 0.5) * 0.12, 1.25, X1, axis="y", step=1.0)
    els += disc("Hutdeckel", 8, t + 12.0, 8, 5.5, 0.8, MAIN, axis="y", step=0.9)
    els += ring("Hutband", 8, t + 3.0, 8, 5.5, 1.8, X2, count=18, size=1.3)
    els.append(centered("Bandschnalle", 8, t + 3.0, 2.4, 2.2, 2.2, 0.8, SECOND))
    # Zahnräder an der Seite
    els += gear("Zahnrad groß", 12.6, t + 6.0, 5.2, 2.6, 9, 0.9, SECOND, MAIN, axis="z")
    els += gear("Zahnrad klein", 10.0, t + 9.0, 5.0, 1.7, 7, 0.8, LIGHT, MAIN, axis="z", phase=0.3)
    els.append(centered("Feder", 4.4, t + 7.0, 6.0, 0.8, 4.0, 0.8, SECOND))
    els.append(centered("Manometer", 4.2, t + 9.6, 5.6, 2.4, 2.4, 1.0, LIGHT))
    els.append(centered("Zeiger", 4.2, t + 9.8, 5.0, 1.4, 0.4, 0.4, DARK))
    # Schutzbrille auf der Krempe
    for side in (-1, 1):
        els += disc("Brillenglas", 8 + side * 3.0, t + 2.2, 2.4, 2.3, 1.1, GLOW, axis="z", step=0.6)
        els += ring("Brillenfassung", 8 + side * 3.0, t + 2.2, 2.0, 2.7, 1.0, SECOND, count=14, size=1.1, axis="xy")
    els.append(box("Brillensteg", [6.4, t + 1.8, 2.2], [9.6, t + 2.6, 2.8], SECOND))
    return dict(kind="head", elements=els)


def steampunk_gear_halo():
    els = gear("Hauptrad", 8, 8, 8, 5.4, 14, 1.2, SECOND, X1, axis="y")
    els += gear("Nebenrad", 8, 8.9, 8, 3.2, 10, 1.0, LIGHT, X1, axis="y", phase=0.25)
    for x, z, deg, i in around(3, 7.2):
        els += gear("Trabantenrad", x, 8 + (1.6, -1.4, 0.4)[i], z, 2.0, 8, 0.9, X2, MAIN, axis="y", phase=i * 0.4)
        els.append(centered("Achse", x, 8 + (1.6, -1.4, 0.4)[i], z, 0.8, 2.6, 0.8, MAIN))
    els += ring("Kette", 8, 8, 8, 7.0, 0.7, MAIN, count=22, size=1.0)
    for x, z, deg, i in around(5, 6.2, phase=0.6):
        els.append(centered("Dampfstoß", x, 10.4 + (i % 2) * 1.2, z, 1.4, 1.4, 1.4, GLOW))
    return dict(kind="halo", elements=els)


def steampunk_wings():
    root = (9.5, 8.3)
    vanes = ((60, 7.5), (46, 11.0), (32, 14.0), (18, 16.5), (4, 17.5), (-10, 16.5), (-26, 13.5), (-44, 9.0))
    # genietete Grundplatte …
    els = sail(root, tuple((a, l * 0.58) for a, l in vanes), X1, z=8.0, thick=0.8, sag=0.05, step=0.5)
    els += sail(root, tuple((a, l * 0.3) for a, l in vanes), SECOND, z=8.7, thick=0.6, sag=0.08, step=0.5)
    # … davor die einzelnen Metalllamellen mit Scharnier
    for angle, length in vanes:
        rad = math.radians(angle)
        inner = length * 0.5
        hinge = (root[0] + math.cos(rad) * inner, root[1] + math.sin(rad) * inner)
        els += blade("Metallfeder", hinge, angle, length - inner, 2.3, X1, tip=SECOND, z=8.0, thick=0.8, taper_to=0.38)
        els += chain("Niete", [(root[0] + math.cos(rad) * length * j / 6, root[1] + math.sin(rad) * length * j / 6, 8.5)
                               for j in range(1, 7)], [0.6] * 6, SECOND)
        els.append(centered("Scharnier", hinge[0], hinge[1], 8.7, 1.5, 1.5, 1.4, MAIN))
    els.append(box("Hauptholm", [8.0, 7.8, 7.1], [16.0, 8.9, 8.9], X1))
    els.append(box("Gelenkblock", [8.4, 6.4, 6.8], [11.6, 10.2, 9.4], SECOND))
    els += gear("Antriebsrad", 10.0, 8.3, 9.8, 2.4, 10, 0.9, LIGHT, MAIN, axis="z")
    els += gear("Nebenrad", 13.2, 5.4, 9.4, 1.6, 8, 0.8, SECOND, MAIN, axis="z", phase=0.3)
    els += chain("Druckleitung", bezier((11, 5.4, 9.2), (15, 3.2, 9.2), (19, 5.2, 9.2), 7), [0.8] * 7, MAIN)
    for x in (13.0, 16.5, 19.5):
        els.append(centered("Kolben", x, 4.6, 9.2, 1.5, 2.6, 1.5, LIGHT))
        els.append(centered("Dampf", x, 2.6, 9.2, 1.3, 1.3, 1.3, GLOW))
    return wings_spec(els)


def steampunk_engine():
    els = [box("Kessel", [4.6, 3.5, 9.0], [11.4, 13.0, 13.5], X1),
           box("Kesselband", [4.4, 6.0, 8.8], [11.6, 7.0, 13.7], SECOND),
           box("Kesselband", [4.4, 10.0, 8.8], [11.6, 11.0, 13.7], SECOND),
           box("Feuerluke", [6.2, 4.4, 8.6], [9.8, 7.2, 9.1], MAIN),
           box("Feuerschein", [6.6, 4.8, 8.4], [9.4, 6.8, 8.7], GLOW)]
    els += disc("Kesselboden", 8, 3.4, 11.2, 3.4, 0.9, SECOND, axis="y", step=0.8)
    # Schornstein mit Dampfwolke
    els += disc("Schornstein", 8, 15.0, 11.0, 1.5, 4.0, SECOND, axis="y", step=0.9)
    els += disc("Schornsteinkranz", 8, 17.2, 11.0, 2.0, 0.8, X2, axis="y", step=0.7)
    for cx, cy, cz, r in ((8, 19.0, 11.0, 2.0), (6.0, 20.6, 10.4, 1.6), (10.0, 21.2, 11.6, 1.4), (8, 22.4, 11.0, 1.2)):
        els += ball("Dampfwolke", cx, cy, cz, r, LIGHT, step=1.0)
    els += gear("Schwungrad", 12.4, 7.0, 11.5, 2.8, 11, 1.0, SECOND, MAIN, axis="x")
    els += gear("Kleinrad", 3.8, 9.5, 11.0, 1.8, 8, 0.9, LIGHT, MAIN, axis="x", phase=0.3)
    els += chain("Pleuelstange", [(12.4, 7.0, 14.2), (10.0, 4.4, 14.0), (7.0, 3.8, 13.8)], [0.8, 0.8, 0.8], X2)
    els.append(centered("Manometer", 11.0, 12.0, 8.7, 2.2, 2.2, 0.8, LIGHT))
    els.append(centered("Zeiger", 11.0, 12.2, 8.3, 1.2, 0.4, 0.4, DARK))
    els.append(box("Gurt", [4.2, 9.0, 8.0], [11.8, 10.4, 8.9], MAIN))
    return dict(kind="back", elements=els)


def steampunk_owl():
    body = [box("Rumpf", [5.6, 4.2, 6.2], [10.4, 10.0, 11.4], X1),
            box("Bauchplatte", [6.2, 4.6, 5.4], [9.8, 9.0, 6.6], SECOND),
            box("Fuß", [6.2, 3.2, 6.0], [7.6, 4.4, 7.8], MAIN), box("Fuß", [8.4, 3.2, 6.0], [9.6, 4.4, 7.8], MAIN),
            box("Schwanzblech", [6.6, 5.0, 11.4], [9.4, 6.0, 15.0], SECOND)]
    body += gear("Bauchwerk", 8, 7.0, 5.2, 2.0, 9, 0.7, LIGHT, MAIN, axis="z")
    for z in (7.6, 9.2, 10.8):
        body.append(centered("Niete", 5.7, 7.0, z, 0.5, 0.5, 0.5, MAIN))
        body.append(centered("Niete", 10.3, 7.0, z, 0.5, 0.5, 0.5, MAIN))
    head = [box("Kopf", [5.2, 9.6, 5.2], [10.8, 14.2, 10.6], X1, north=X4),
            box("Schnabel", [7.2, 10.6, 3.6], [8.8, 12.2, 5.4], SECOND),
            box("Schnabelspitze", [7.4, 9.8, 3.4], [8.6, 11.0, 4.6], SECOND)]
    for side in (-1, 1):
        head += disc("Augenblende", 8 + side * 2.2, 12.4, 4.8, 2.0, 0.9, SECOND, axis="z", step=0.6)
        head += disc("Linse", 8 + side * 2.2, 12.4, 4.2, 1.3, 0.7, GLOW, axis="z", step=0.5)
        head.append(centered("Blendenring", 8 + side * 2.2, 12.4, 3.8, 1.0, 1.0, 0.4, MAIN))
        head.append(centered("Federohr", 8 + side * 2.0, 14.8, 7.4, 1.2, 1.8, 1.2, X1))
        head.append(centered("Schraube", 8 + side * 2.8, 10.4, 5.0, 0.6, 0.6, 0.5, MAIN))
    head += gear("Kopfrad", 8, 14.4, 8.6, 1.6, 8, 0.7, LIGHT, MAIN, axis="y")
    wing = [box("Flügelblech", [8, 4.2, 6.4], [9.2, 9.6, 12.0], SECOND),
            box("Flügelkante", [8.2, 3.2, 8.5], [9.0, 4.4, 12.4], X1)]
    for k in range(4):
        wing.append(centered("Lamelle", 8.9, 8.6 - k * 1.5, 9.2 + k * 0.7, 0.6, 1.2, 4.6 - k * 0.5, X1))
    wing += gear("Flügelgelenk", 8.7, 9.4, 7.6, 1.3, 7, 0.6, LIGHT, MAIN, axis="x")
    icon = body + shift(head, 0, 1.5, 0.25) + shift(wing, 2.5, 1.0, 0.5) + shift(mirror(wing), -2.5, 1.0, 0.5)
    return dict(kind="pet", parts={"body": body, "head": head, "wing_a": wing, "wing_b": mirror(wing)}, icon=icon)


def steampunk_robot():
    body = [box("Rumpf", [4.8, 3.0, 5.4], [11.2, 10.0, 10.6], X1),
            box("Brustplatte", [5.4, 4.6, 4.8], [10.6, 8.6, 5.6], SECOND),
            box("Hals", [7.0, 10.0, 7.0], [9.0, 11.0, 9.0], MAIN),
            box("Kopf", [5.4, 11.0, 5.6], [10.6, 15.2, 10.4], X1, north=X4),
            box("Fuß", [4.8, 1.0, 5.6], [7.4, 3.2, 9.8], SECOND), box("Fuß", [8.6, 1.0, 5.6], [11.2, 3.2, 9.8], SECOND)]
    body += gear("Brustwerk", 8, 6.6, 4.6, 2.0, 9, 0.7, LIGHT, MAIN, axis="z")
    body += disc("Kesselbauch", 8, 6.6, 8.0, 2.8, 4.4, X1, axis="y", step=1.0)
    for side in (-1, 1):
        body += disc("Auge", 8 + side * 1.8, 13.2, 5.2, 1.1, 0.8, GLOW, axis="z", step=0.5)
        body.append(centered("Augenring", 8 + side * 1.8, 13.2, 4.8, 0.7, 0.7, 0.4, MAIN))
        body.append(centered("Kopfniete", 8 + side * 2.6, 14.6, 8.0, 0.6, 0.6, 0.6, MAIN))
    body.append(box("Mundgitter", [6.4, 11.6, 5.3], [9.6, 12.4, 5.6], MAIN))
    body += disc("Schornstein", 5.8, 16.4, 8.4, 1.0, 2.8, SECOND, axis="y", step=0.8)
    body += ball("Dampf", 5.8, 18.6, 8.4, 1.4, LIGHT, step=0.8)
    body.append(centered("Ventil", 10.4, 16.0, 8.4, 1.4, 1.4, 1.4, SECOND))
    flipper = [box("Schulter", [10.6, 7.4, 6.4], [13.2, 10.2, 9.6], SECOND),
               box("Oberarm", [11.0, 4.6, 7.0], [12.8, 7.6, 9.0], X1),
               box("Unterarm", [10.8, 2.2, 6.8], [13.0, 4.8, 9.2], X1),
               box("Greifer", [10.6, 0.8, 6.6], [13.2, 2.4, 9.4], SECOND),
               box("Kolbenstange", [11.4, 4.4, 6.5], [12.4, 8.0, 7.0], MAIN)]
    return dict(kind="pet", parts={"body": body, "flipper_a": flipper, "flipper_b": mirror(flipper)},
                icon=body + flipper + mirror(flipper))


item("steampunk", "steampunk_top_hat", "HEAD", "NONE", "LEGENDARY", "Zylinder mit Brille", "Geared Top Hat", steampunk_top_hat, scale=1.1,
     extras=[(hexc(0x3A2818), "metal"), (hexc(0x6A4A22), "metal")])
item("steampunk", "steampunk_gear_halo", "HEAD", "HALO", "LEGENDARY", "Zahnradkranz", "Gear Halo", steampunk_gear_halo, scale=1.15,
     extras=[(hexc(0x8A6A2A), "metal"), (hexc(0xD8A850), "metal")])
item("steampunk", "steampunk_wings", "BACK", "WINGS", "LEGENDARY", "Maschinenschwingen", "Machine Wings", steampunk_wings, scale=1.2,
     extras=[(hexc(0x8A6A2A), "metal")])
item("steampunk", "steampunk_engine", "BACK", "BACKPACK", "EPIC", "Dampfmaschine", "Steam Engine", steampunk_engine,
     extras=[(hexc(0x6A4A22), "metal"), (hexc(0xD8A850), "metal")])
item("steampunk", "steampunk_owl", "PET", "OWL", "LEGENDARY", "Uhrwerk-Eule", "Clockwork Owl", steampunk_owl,
     extras=[(hexc(0x8A6A2A), "metal"), (hexc(0xD8A850), "metal"), hexc(0x4A3418), (hexc(0xB8823A), "face")])
item("steampunk", "steampunk_robot", "PET", "PENGUIN", "LEGENDARY", "Kupferblechling", "Copper Automaton", steampunk_robot,
     extras=[(hexc(0x8A6A2A), "metal"), (hexc(0xD8A850), "metal"), hexc(0x4A3418), (hexc(0xB8823A), "face")])


# =========================================================================== SPUK
def spooky_lanterns():
    els = []
    for x, z, deg, i in around(4, 5.8):
        y = 8 + (2.4, -1.8, 1.0, -2.6)[i]
        # Papierlaterne mit Deckel, Boden und Seelenflamme
        els += disc("Laternenkörper", x, y, z, 2.0, 3.2, MAIN, axis="y", step=0.9)
        els += disc("Deckel", x, y + 1.8, z, 2.3, 0.6, X1, axis="y", step=0.8)
        els += disc("Boden", x, y - 1.8, z, 2.3, 0.6, X1, axis="y", step=0.8)
        for k in (-0.9, 0.0, 0.9):
            els += ring("Rippe", x, y + k * 1.5, z, 2.1, 0.4, X1, count=8, size=0.6)
        els += ball("Seelenflamme", x, y, z, 1.1, GLOW, step=0.5)
        els.append(centered("Bügel", x, y + 2.6, z, 0.4, 1.8, 0.4, X1))
        els.append(centered("Ring", x, y + 3.4, z, 1.2, 0.4, 1.2, X1))
        els.append(centered("Quaste", x, y - 2.8, z, 0.6, 1.6, 0.6, SECOND))
        for k in range(3):
            els.append(centered("Funken", x + math.sin(i + k) * 1.6, y + 3.0 + k * 1.1, z + math.cos(i + k) * 1.6,
                                0.5, 0.5, 0.5, LIGHT))
    els += ring("Nebelband", 8, 8, 8, 7.0, 0.3, SECOND, count=16, size=0.5)
    return dict(kind="halo", elements=els)


def spooky_spider_hat():
    t = T_TOP
    els = ball("Spinnenleib", 8, t + 3.6, 9.5, 4.0, X1, step=0.85, squash=0.85)
    els.append(box("Kopfbruststück", [6.0, t + 2.2, 4.2], [10.0, t + 5.4, 7.0], X1))
    els += band("Halteband", t - 2.4, t - 1.2, 7.6, X1, thick=1.2)
    for k in range(4):
        els.append(centered("Rückenmal", 8, t + 5.4 - k * 0.3, 8.6 + k * 1.1, 2.6 - k * 0.5, 1.0, 1.4, SECOND))
    for side in (-1, 1):
        for k, (spread, lift, reach) in enumerate(((-38, 3.6, 7.0), (-12, 4.6, 8.0), (14, 4.4, 7.6), (40, 3.4, 6.4))):
            a = math.radians(spread)
            knee = (8 + side * 3.2 + side * math.cos(a) * reach * 0.55,
                    t + 2.0 + lift,
                    6.0 + math.sin(a) * reach * 0.55)
            foot = (8 + side * 3.2 + side * math.cos(a) * reach,
                    t - 3.2,
                    6.0 + math.sin(a) * reach)
            els += chain("Bein", densify([(8 + side * 3.2, t + 3.2, 6.6), knee], 1.0), [0.9] * 24, X1)
            els += chain("Bein", densify([knee, foot], 1.0), [0.75] * 24, X1)
            els.append(centered("Fußspitze", foot[0], foot[1] - 0.3, foot[2], 0.8, 0.8, 0.8, SECOND))
        for k, (dx, dy, r) in enumerate(((1.0, 0.8, 0.7), (2.2, 0.4, 0.5), (1.4, -0.6, 0.45), (2.6, -0.8, 0.4))):
            els.append(centered("Auge", 8 + side * dx, t + 4.0 + dy, 4.0, r * 2, r * 2, r, GLOW))
    els.append(centered("Kieferklaue", 7.0, t + 2.2, 3.8, 0.7, 1.6, 1.2, SECOND))
    els.append(centered("Kieferklaue", 9.0, t + 2.2, 3.8, 0.7, 1.6, 1.2, SECOND))
    els += chain("Spinnfaden", [(8, t + 7.6, 9.5), (8, t + 10.5, 9.5)], [0.35, 0.35], LIGHT)
    return dict(kind="head", elements=els)


def spooky_bone_wings():
    shoulder = (10.5, 8.4)
    elbow = (14.5, 12.0)
    wrist = (19.5, 12.8)
    fingers = ((34, 9.0), (12, 12.5), (-10, 13.5), (-32, 12.0), (-56, 8.5))
    # vergammelte Flughaut zwischen den Fingerknochen …
    els = sail(wrist, fingers, DARK, z=8.0, thick=0.4, sag=0.18, step=0.5)
    # … und ein Rest zwischen Arm und erstem Finger
    els += sail(shoulder, ((58, 7.5), (30, 11.0), (6, 12.0)), DARK, z=8.0, thick=0.4, sag=0.22, step=0.5)
    els.append(box("Schulterblatt", [8, 6.8, 7.3], [10.8, 10.2, 8.7], LIGHT))
    for a, b, w in ((shoulder, elbow, 1.3), (elbow, wrist, 1.1)):
        els += chain("Armknochen", densify([(a[0], a[1], 8.0), (b[0], b[1], 8.0)], 0.7), [w] * 30, LIGHT)
    for jx, jy, r in ((shoulder[0], shoulder[1], 1.2), (elbow[0], elbow[1], 1.3), (wrist[0], wrist[1], 1.2)):
        els += ball("Gelenk", jx, jy, 8.0, r, LIGHT, step=0.5)
    for angle, length in fingers:
        rad = math.radians(angle)
        n = max(6, int(length / 0.8))
        pts = [(wrist[0] + math.cos(rad) * length * k / (n - 1), wrist[1] + math.sin(rad) * length * k / (n - 1), 8.0)
               for k in range(n)]
        els += chain("Fingerknochen", pts, taper(1.0, 0.45, n), LIGHT)
        for k in range(3, n - 1, 4):
            els.append(centered("Fingergelenk", pts[k][0], pts[k][1], 8.0, 1.3, 1.3, 1.3, LIGHT))
        els.append(centered("Kralle", wrist[0] + math.cos(rad) * (length + 0.8),
                            wrist[1] + math.sin(rad) * (length + 0.8), 8.0, 1.2, 1.2, 1.2, SECOND))
    for x, y in ((24.0, 9.0), (17.0, 4.5), (13.0, 15.5)):
        els += ball("Irrlicht", x, y, 8.7, 0.9, GLOW, step=0.5)
    return wings_spec(els)


def spooky_scythe():
    els = []
    shaft = densify([(6.0, 1.0, 13.5), (9.5, 18.0, 11.0)], 0.9)
    els += chain("Sensenstiel", shaft, [1.0] * len(shaft), X1)
    els.append(centered("Griffwicklung", 7.2, 7.0, 12.7, 1.5, 2.6, 1.5, SECOND))
    els.append(centered("Griffwicklung", 8.4, 12.4, 11.9, 1.4, 2.2, 1.4, SECOND))
    els.append(centered("Knauf", 5.9, 0.6, 13.6, 1.5, 1.2, 1.5, SECOND))
    els += ball("Fassung", 9.5, 18.0, 11.0, 1.5, SECOND, step=0.6)
    # geschwungene Klinge
    blade_pts = bezier((9.5, 18.6, 11.0), (4.0, 20.8, 10.0), (-2.0, 17.5, 9.4), 12)
    els += chain("Sensenblatt", blade_pts, taper(2.2, 0.9, 12), LIGHT)
    for k, (x, y, z) in enumerate(blade_pts):
        f = k / (len(blade_pts) - 1)
        els.append(centered("Schneide", x, y - 1.1 + f * 0.5, z, 1.2, 0.7, 1.0, GLOW))
    els += chain("Seelenfaden", bezier((9.5, 17.4, 11.2), (11.5, 13.0, 11.8), (10.0, 9.0, 12.0), 6), [0.4] * 6, GLOW)
    for x, y, z, r in ((11.6, 10.4, 12.0, 0.9), (12.4, 6.6, 12.4, 0.7), (10.6, 4.0, 12.8, 0.6)):
        els += ball("Irrlicht", x, y, z, r, GLOW, step=0.5)
    els.append(box("Rückenhalterung", [6.6, 8.0, 9.6], [9.4, 12.5, 12.4], X1))
    els.append(box("Riemen", [4.6, 9.4, 8.6], [11.4, 10.6, 9.8], SECOND))
    return dict(kind="back", elements=els)


def spooky_lantern_spirit():
    # offene Laterne: nur Deckel, Boden, Eckstreben und Papier an den Seiten
    els = disc("Deckel", 8, 11.6, 8, 3.6, 1.0, X1, axis="y", step=0.8)
    els += disc("Dachschräge", 8, 12.4, 8, 2.6, 0.8, X1, axis="y", step=0.8)
    els += disc("Boden", 8, 5.4, 8, 3.6, 1.0, X1, axis="y", step=0.8)
    for x, z, deg, i in around(4, 3.1, phase=0.785):
        els.append(centered("Eckstrebe", x, 8.5, z, 0.8, 6.4, 0.8, X1))
    for x, z, deg, i in around(3, 3.0, phase=1.9):        # Papier nur hinten und an den Seiten
        els.append(centered("Papierwand", x, 8.5, z, 4.4 - abs(x - 8) * 0.9, 5.4, 4.4 - abs(z - 8) * 0.9, SECOND))
    els.append(centered("Querstrebe", 8, 8.5, 10.9, 4.6, 0.6, 0.6, X1))
    # Geist schwebt vorn aus der Laterne heraus
    els += ball("Geisterkopf", 8, 9.0, 6.6, 2.3, GLOW, step=0.5)
    els.append(centered("Geistergesicht", 8, 9.0, 4.3, 3.4, 2.8, 0.4, X2, north=X2))
    els.append(centered("Geisterarm", 5.6, 8.4, 6.2, 2.2, 0.9, 1.6, LIGHT))
    els.append(centered("Geisterarm", 10.4, 8.4, 6.2, 2.2, 0.9, 1.6, LIGHT))
    for k in range(4):
        els.append(centered("Geisterschweif", 8 + math.sin(k * 1.9) * 0.8, 7.4 - k * 0.55, 8 + math.cos(k * 1.9) * 0.8,
                            2.4 - k * 0.45, 0.7, 2.4 - k * 0.45, LIGHT))
    els.append(centered("Bügel", 8, 12.6, 8, 0.6, 2.4, 0.6, X1))
    els += ring("Tragering", 8, 14.0, 8, 1.4, 0.5, X1, count=10, size=0.7)
    for k in range(3):
        els.append(centered("Rußfleck", 6.0 + k * 2.0, 6.6 + k * 0.7, 5.2, 1.2, 0.9, 0.3, SECOND))
    return dict(kind="pet", elements=els)


def spooky_spider():
    body = ball("Hinterleib", 8, 6.0, 11.0, 3.8, X1, step=0.85, squash=0.9)
    body.append(box("Vorderleib", [5.8, 4.2, 5.0], [10.2, 8.2, 8.6], X1, north=X2))
    for k in range(3):
        body.append(centered("Rückenmuster", 8, 8.6 - k * 0.6, 9.6 + k * 1.3, 2.8 - k * 0.7, 1.0, 1.4, SECOND))
    for side in (-1, 1):
        for k, (dx, dy, r) in enumerate(((0.9, 1.6, 0.55), (2.0, 1.3, 0.45), (1.2, 0.4, 0.4), (2.4, 0.2, 0.35))):
            body.append(centered("Auge", 8 + side * dx, 6.0 + dy, 4.8, r * 2, r * 2, r, GLOW))
        body.append(centered("Kieferklaue", 8 + side * 1.0, 4.4, 4.4, 0.7, 1.4, 1.2, SECOND))
    flipper = []
    for k, (spread, lift, reach) in enumerate(((-30, 3.4, 6.5), (-6, 4.0, 7.2), (18, 3.8, 6.8), (44, 3.0, 5.8))):
        a = math.radians(spread)
        hip = (10.0, 6.6, 6.6)
        knee = (hip[0] + math.cos(a) * reach * 0.6, hip[1] + lift, hip[2] + math.sin(a) * reach * 0.6)
        foot = (hip[0] + math.cos(a) * reach, 0.8, hip[2] + math.sin(a) * reach)
        flipper += chain("Bein", densify([hip, knee], 1.0), [0.85] * 24, X1)
        flipper += chain("Bein", densify([knee, foot], 1.0), [0.7] * 24, X1)
        flipper.append(centered("Fußspitze", foot[0], foot[1], foot[2], 0.8, 0.8, 0.8, SECOND))
    return dict(kind="pet", parts={"body": body, "flipper_a": flipper, "flipper_b": mirror(flipper)},
                icon=body + flipper + mirror(flipper))


item("spooky", "spooky_lanterns", "HEAD", "HALO", "LEGENDARY", "Seelenlaternen", "Soul Lanterns", spooky_lanterns, scale=1.15,
     extras=[(hexc(0x2A1A16), "metal")])
item("spooky", "spooky_spider_hat", "HEAD", "NONE", "EPIC", "Spinnenhut", "Spider Hat", spooky_spider_hat,
     extras=[hexc(0x1A1020)])
item("spooky", "spooky_bone_wings", "BACK", "WINGS", "LEGENDARY", "Knochenschwingen", "Bone Wings", spooky_bone_wings, scale=1.2)
item("spooky", "spooky_scythe", "BACK", "BACKPACK", "LEGENDARY", "Seelensense", "Soul Scythe", spooky_scythe, scale=1.1,
     extras=[hexc(0x3A2A20)])
item("spooky", "spooky_lantern_spirit", "PET", "GHOST", "LEGENDARY", "Laternengeist", "Lantern Spirit", spooky_lantern_spirit,
     extras=[(hexc(0x2A1A16), "metal"), (hexc(0x9AFFB8), "face")])
item("spooky", "spooky_spider", "PET", "PENGUIN", "EPIC", "Spinne", "Spider", spooky_spider,
     extras=[hexc(0x1A1020), (hexc(0x1A1020), "face")])
