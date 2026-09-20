"""
Welten 6-10: Himmel, Ozean, Kristall, Neon, Zucker.
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


def feather_wing(root, angles, lengths, width, cell, tip, z=8.0, thick=0.7):
    els = []
    for angle, length in zip(angles, lengths):
        els += blade("Feder", root, angle, length, width, cell, tip=tip, z=z, thick=thick)
    return els


def wings_spec(els):
    return dict(kind="wings", parts={"wing_a": els, "wing_b": mirror(els)}, icon=shift(els, 2) + shift(mirror(els), -2))


# =========================================================================== HIMMEL
def heaven_halo():
    els = ring("Heiligenschein", 8, 8, 8, 5, 1.2, LIGHT, count=20, size=1.4)
    els += ring("Innenglanz", 8, 8.1, 8, 4.1, 0.6, GLOW, count=16, size=0.8)
    for side in (-1, 1):
        wing = []
        for i, (angle, length) in enumerate(((70, 4), (45, 5.5), (20, 6.5), (-5, 5.5))):
            wing += blade("Flügelchen", (8 + side * 5.6, 8), angle if side > 0 else 180 - angle, length, 1.3, LIGHT,
                          tip=GLOW, z=8, thick=0.5)
        els += wing
    return dict(kind="halo", elements=els)


def heaven_laurel():
    t = T_TOP
    els = []
    for side in (-1, 1):
        stem = bezier((8 + side * 0.5, t - 1.2, 1.4), (8 + side * 7.5, t - 1.6, 4), (8 + side * 6.4, t - 1.8, 13), 9)
        els += chain("Zweig", stem, [0.6] * len(stem), X1)
        for i, (x, y, z) in enumerate(stem[1:], start=1):
            out = 1 if i % 2 else -1
            els.append(centered("Lorbeerblatt", x + side * 0.9 * out, y + 0.5, z, 1.2, 0.5, 2.3, X1))
            els.append(centered("Lorbeerblatt", x - side * 0.4 * out, y + 1.1, z + 0.5, 1.0, 0.5, 2.0, X2))
    els.append(centered("Edelstein", 8, t - 1.2, 1.2, 1.6, 1.6, 0.6, GLOW))
    return dict(kind="head", elements=els)


def heaven_seraph_wings():
    upper = feather_wing((8.6, 8.5), (78, 60, 42, 24, 6), (9, 12.5, 15, 15.5, 13), 2.4, LIGHT, GLOW)
    lower = feather_wing((8.6, 7), (-8, -26, -44), (11, 9, 7), 2.0, MAIN, GLOW, z=8.6, thick=0.6)
    covert = feather_wing((8.6, 8), (50, 28, 8), (7, 8, 7), 1.6, MAIN, LIGHT, z=8.9, thick=0.5)
    return wings_spec(upper + lower + covert)


def heaven_sun_disc():
    els = [box("Halter", [7.2, 6.5, 8], [8.8, 8.5, 11], X1)]
    els += disc("Sonnenscheibe", 8, 10, 12, 3.6, 1.2, GLOW, axis="z", step=0.6)
    els += ring("Sonnenring", 8, 10, 12.3, 4.6, 0.8, LIGHT, count=20, size=1.1, axis="xy")
    for x, y, deg, i in around(12, 5.2, cx=8, cz=10):
        rad = math.radians(deg)
        length = 3.4 if i % 2 == 0 else 2.2
        pts = [(8 + math.cos(rad) * r, 10 + math.sin(rad) * r, 12.3) for r in taper(5.4, 5.4 + length, 4)]
        els += chain("Strahl", pts, taper(1.2, 0.5, 4), MAIN, tip=LIGHT)
    return dict(kind="back", elements=els)


def heaven_cloudlet():
    body = []
    for cx, cy, cz, r in ((8, 8, 8, 3.2), (5.3, 7.4, 8.4, 2.3), (10.7, 7.4, 8.4, 2.3), (8, 9.6, 9.5, 2.3)):
        body += ball("Wolke", cx, cy, cz, r, LIGHT, step=1)
    body.append(box("Gesicht", [5.8, 6.4, 4.7], [10.2, 9.6, 5], X1, north=X1))
    wing = [box("Flügelchen", [8, 8, 7], [12.5, 8.4, 10], GLOW), box("Feder", [8, 7.8, 10], [11, 8.2, 12], LIGHT)]
    return dict(kind="pet", parts={"body": body, "wing_a": wing, "wing_b": mirror(wing)},
                icon=body + shift(wing, 2, 2, 0.5) + shift(mirror(wing), -2, 2, 0.5))


def heaven_starling():
    els = []
    # fünfzackiger Stern aus Balken, dick
    els.append(box("Stern Mitte", [4.5, 2.5, 6.5], [11.5, 9.5, 9.5], MAIN, north=X1))
    for deg in (90, 18, 162, -54, -126):
        rad = math.radians(deg)
        pts = [(8 + math.cos(rad) * r, 6 + math.sin(rad) * r, 8) for r in (3.8, 5.0, 6.0)]
        els += chain("Zacke", pts, [(2.6, 2.6, 2.6), (1.8, 1.8, 2.2), (1.0, 1.0, 1.6)], MAIN, tip=GLOW)
    els += ball("Glanz", 6, 8.5, 6.4, 0.6, LIGHT, step=0.4)
    return dict(kind="pet", elements=shift(els, 0, 1.4, 0))


item("heaven", "heaven_winged_halo", "HEAD", "HALO", "LEGENDARY", "Geflügelter Heiligenschein", "Winged Halo", heaven_halo, scale=1.15)
item("heaven", "heaven_laurel", "HEAD", "NONE", "EPIC", "Goldener Lorbeer", "Golden Laurel", heaven_laurel,
     extras=[(hexc(0xE8C04A), "metal"), (hexc(0xFFE27A), "metal")])
item("heaven", "heaven_seraph_wings", "BACK", "WINGS", "LEGENDARY", "Seraphenflügel", "Seraph Wings", heaven_seraph_wings, scale=1.2)
item("heaven", "heaven_sun_disc", "BACK", "BACKPACK", "EPIC", "Sonnenscheibe", "Sun Disc", heaven_sun_disc,
     extras=[(hexc(0xE8C04A), "metal")])
item("heaven", "heaven_cloudlet", "PET", "BEE", "EPIC", "Wölkchen", "Cloudlet", heaven_cloudlet,
     extras=[(hexc(0xFFFFFF), "face")])
item("heaven", "heaven_starling", "PET", "MUSHROOM", "LEGENDARY", "Sternchen", "Little Star", heaven_starling,
     extras=[(hexc(0xFFE27A), "face")])


# =========================================================================== OZEAN
def ocean_shell_crown():
    els = ring("Korallenreif", 8, 6.5, 8, 5, 1.4, X2, count=16, size=1.5)
    for x, z, deg, i in around(7, 5):
        # Muschel: gefächert aus mehreren Streben
        for k, spread in enumerate((-0.9, -0.3, 0.3, 0.9)):
            pts = [(x + spread * r * 0.35, 7 + r, z) for r in (0.6, 1.8, 3.0)]
            els += chain("Muschel", pts, [1.0, 0.9, 0.7], X1 if k % 2 else X3, tip=X3)
    for x, z, deg, i in around(7, 5.4, phase=0.45):
        els += ball("Perle", x, 7.6, z, 0.8, GLOW, step=0.5)
    els += chain("Koralle", [(8, 7, 3), (8.4, 9, 3), (7.6, 10.5, 3.2), (8.2, 12, 3)], [1, 0.9, 0.8, 0.7], X2, tip=GLOW)
    return dict(kind="halo", elements=els)


def ocean_shark_hat():
    t = T_TOP
    els = [box("Hai-Kopf", [0.8, t - 3, 0.2], [15.2, t + 1.5, 15.8], MAIN, north=X1),
           box("Bauch", [1.2, t - 3.4, 0.4], [14.8, t - 2.8, 15.6], LIGHT),
           box("Schnauze", [3, t - 1.5, -1.2], [13, t + 1.2, 0.2], MAIN)]
    els += stack("Rückenflosse", 8, 8, t + 1.5, [(1.6, 0.6, 2.8), (1.6, 0.5, 2.0), (1.4, 0.4, 1.2), (1, 0.3, 0.6)], MAIN, tip=SECOND,
                 lean=(0, 0.35))
    for x in (2.5, 4.5, 6.5, 8.5, 10.5, 12.5):
        els.append(box("Zahn", [x - 0.5, t - 3.9, 0.3], [x + 0.5, t - 3, 0.8], X2))
    els.append(box("Seitenflosse", [-0.8, t - 2.5, 6], [0.8, t - 2, 10], MAIN))
    els.append(box("Seitenflosse", [15.2, t - 2.5, 6], [16.8, t - 2, 10], MAIN))
    return dict(kind="head", elements=els)


def ocean_fin_wings():
    els = []
    for angle, length in ((66, 10), (40, 13), (14, 14), (-12, 11)):
        els += blade("Flossenstrahl", (8.6, 8), angle, length, 0.7, X1, z=8.5, thick=0.5, taper_to=0.6)
    for k in range(24):
        d = 13 * k / 23
        rad = math.radians(28)
        w = max(1.0, 9.5 * math.sin(math.pi * (d + 0.4) / 14))
        els.append(centered("Flossenhaut", 8.6 + math.cos(rad) * d, 8 + math.sin(rad) * d, 8.1, w, w * 0.9, 0.4, MAIN))
    return wings_spec(els)


def ocean_tentacles():
    els = ball("Krakenkörper", 8, 9, 11.5, 2.8, MAIN, step=0.8)
    starts = ((5.5, 7.5, 11.5, -1, 1), (10.5, 7.5, 11.5, 1, 1), (6, 10.8, 12.5, -1, -1), (10, 10.8, 12.5, 1, -1))
    for sx, sy, sz, dx, dy in starts:
        path = bezier((sx, sy, sz), (sx + dx * 6, sy + dy * 1.5, sz + 3), (sx + dx * 4, sy + dy * 6, sz + 5), 9)
        els += chain("Tentakel", path, taper(1.9, 0.6, 9), SECOND, tip=GLOW)
        for i in (2, 4, 6):
            x, y, z = path[i]
            els.append(centered("Saugnapf", x, y, z - 0.9, 0.7, 0.7, 0.3, LIGHT))
    els += [box("Halter", [7, 7.5, 8], [9, 9.5, 9.5], DARK)]
    return dict(kind="back", elements=els)


def ocean_serpent():
    head = [box("Kopf", [5.2, 5.5, 2], [10.8, 10, 10.5], MAIN, north=FACE),
            box("Kiefer", [5.8, 4.4, 1.2], [10.2, 5.5, 7], LIGHT),
            box("Flosse", [4, 8, 6], [5.2, 12, 9.5], X1), box("Flosse", [10.8, 8, 6], [12, 12, 9.5], X1)]
    head += stack("Kamm", 8, 7, 10, [(1.2, 0.4, 2.2), (1.2, 0.3, 1.5), (1, 0.25, 0.8)], X1, lean=(0, 0.4))
    body = [box("Glied", [5.6, 5.6, 4.6], [10.4, 10.4, 11.4], MAIN, down=LIGHT),
            box("Rückenflosse", [7.5, 10.4, 5.5], [8.5, 12.6, 10.5], X1)]
    tail = [box("Schwanz", [6.4, 6.4, 3.5], [9.6, 9.6, 9], MAIN),
            box("Schwanzflosse", [4.5, 5.5, 9], [11.5, 10.5, 10], X1), box("Schwanzflosse Ende", [3.5, 4.5, 10], [12.5, 11.5, 11], X1)]
    icon = head + shift(body, 0, 0, 5.2) + shift(body, 0, 0, 10.4) + shift(tail, 0, 0, 15.6)
    return dict(kind="pet", parts={"head": head, "body": body, "tail": tail}, icon=icon)


def ocean_jellyfish():
    body = ball("Schirm", 8, 9, 8, 4.4, MAIN, step=0.8, squash=0.75, top_only=True)
    body.append(box("Schirmrand", [3.4, 8.4, 3.4], [12.6, 9.2, 12.6], LIGHT))
    body.append(box("Gesicht", [5.4, 9.4, 4.3], [10.6, 11.8, 4.6], X1, north=X1))
    body += ball("Leuchtkern", 8, 10.4, 8, 1.6, GLOW, step=0.6)
    tentacles = []
    for x, z, deg, i in around(8, 3, cx=8, cz=8):
        length = (6, 8, 7, 9, 6.5, 8.5, 7, 7.5)[i]
        pts = [(x + math.sin(k * 0.8 + i) * 0.4, 8 - k * 1.1, z) for k in range(int(length))]
        tentacles += chain("Tentakel", pts, taper(0.8, 0.4, len(pts)), SECOND, tip=GLOW)
    for i in range(4):
        pts = [(8 + math.sin(i * 1.6) * 1.2, 8 - k * 1.2, 8 + math.cos(i * 1.6) * 1.2) for k in range(5)]
        tentacles += chain("Mundarm", pts, taper(1.4, 0.8, 5), LIGHT)
    return dict(kind="pet", parts={"body": body, "tentacles": tentacles}, icon=body + shift(tentacles, 0, -2, 0))


item("ocean", "ocean_shell_crown", "HEAD", "HALO", "LEGENDARY", "Muschelkrone", "Seashell Crown", ocean_shell_crown, scale=1.15,
     extras=[hexc(0xF6D2C0), hexc(0xFF7A8A), hexc(0xFFF4EA)])
item("ocean", "ocean_shark_hat", "HEAD", "NONE", "RARE", "Hai-Mütze", "Shark Hat", ocean_shark_hat,
     extras=[(hexc(0x4A8AC8), "face"), hexc(0xFFFFFF)])
item("ocean", "ocean_fin_wings", "BACK", "WINGS", "LEGENDARY", "Flossenschwingen", "Fin Wings", ocean_fin_wings, scale=1.15,
     extras=[hexc(0x9AE8FF)])
item("ocean", "ocean_kraken", "BACK", "BACKPACK", "LEGENDARY", "Kraken-Tentakel", "Kraken Tentacles", ocean_tentacles)
item("ocean", "ocean_sea_serpent", "PET", "SERPENT", "LEGENDARY", "Seeschlange", "Sea Serpent", ocean_serpent,
     extras=[hexc(0x5AE8D0)])
item("ocean", "ocean_jellyfish", "PET", "JELLY", "EPIC", "Qualle", "Jellyfish", ocean_jellyfish,
     extras=[(hexc(0x7AC8FF, 220), "face")])


# =========================================================================== KRISTALL
def crystal_cluster_crown():
    els = ring("Geodenrand", 8, 5.6, 8, 5, 1.4, DARK, count=16, size=1.6)
    for (x, z, deg, i), h in zip(around(9, 4.6), (7, 4, 5.5, 8.5, 3.5, 6, 4.5, 7.5, 5)):
        lean = ((x - 8) * 0.05, (z - 8) * 0.05)
        els += stack("Kristall", x, z, 6, [(h * 0.35, 1.2), (h * 0.3, 0.95), (h * 0.2, 0.65), (h * 0.15, 0.3)],
                     MAIN if i % 2 else SECOND, tip=LIGHT, lean=lean)
    els += stack("Großer Kristall", 8, 8, 6, [(3, 1.6), (3, 1.25), (2.5, 0.85), (1.5, 0.4)], GLOW, tip=LIGHT)
    return dict(kind="halo", elements=els)


def crystal_horn():
    t = T_TOP
    els = stack("Kristallhorn", 8, 2.6, t - 3, [(2, 1.3), (2, 1.05), (2, 0.8), (2, 0.55), (1.6, 0.3)], MAIN, tip=LIGHT,
                lean=(0, -0.25))
    els.append(box("Stirnreif", [1.4, t - 3.6, 1.2], [14.6, t - 2.6, 14.8], X1))
    for side in (-1, 1):
        els += stack("Wangenkristall", 8 + side * 5, 1.8, t - 5.5, [(1.2, 0.6), (1.2, 0.4), (0.8, 0.2)], SECOND, tip=GLOW)
    els.append(box("Fassung", [6.4, t - 3.8, 1.6], [9.6, t - 2.2, 2.2], X1))
    return dict(kind="head", elements=els)


def crystal_prism_wings():
    els = []
    # Facetten-Paneele: Dreiecke aus immer kürzer werdenden Streifen
    for row, (y0, reach, cell) in enumerate(((8, 15, MAIN), (10.5, 12, SECOND), (5.5, 11, LIGHT))):
        for k in range(8):
            h = 0.9
            length = reach * (1 - k / 9)
            y = y0 + (k * h if row != 2 else -k * h)
            els.append(box("Facette", [8.6, y, 7.8 + row * 0.2], [8.6 + length, y + h, 8.4 + row * 0.2], cell))
    for y, length in ((8, 15), (13.5, 11), (5.5, 11)):
        els += blade("Kante", (8.6, y), 0, length, 0.5, GLOW, z=8.7, thick=0.3, taper_to=1)
    return wings_spec(els)


def crystal_floaters():
    els = []
    for cx, cy, cz, h, cell in ((5, 7, 11.5, 7, MAIN), (11, 9, 12, 8.5, SECOND), (8, 12.5, 13.5, 6, LIGHT)):
        els += stack("Schwebekristall", cx, cz, cy - h / 2,
                     [(h * 0.2, 0.3), (h * 0.3, 0.9), (h * 0.3, 0.9), (h * 0.2, 0.3)], cell, tip=GLOW)
        els.append(centered("Glühen", cx, cy - h / 2 - 0.8, cz, 0.6, 0.6, 0.6, GLOW))
    els += ring("Runenkreis", 8, 9, 10, 5.8, 0.4, GLOW, count=24, size=0.6, axis="xy")
    return dict(kind="back", elements=els)


def crystal_tortoise():
    body = [box("Bauch", [4.5, 1.8, 4.5], [11.5, 4, 12], X1),
            box("Kopf", [6, 2.5, 1.2], [10, 6, 4.6], X1, north=X2),
            box("Schwanz", [7.3, 2, 12], [8.7, 3, 13.8], X1)]
    body += ball("Panzer", 8, 4, 8.2, 4.6, DARK, step=0.9, squash=0.8, top_only=True)
    for x, z, h in ((6, 6.5, 3.5), (10, 6.5, 3), (8, 9.5, 4.5), (5.8, 10.5, 2.5), (10.2, 10.5, 3), (8, 7, 3.8)):
        els_h = stack("Panzerkristall", x, z, 6.2, [(h * 0.4, 0.8), (h * 0.35, 0.55), (h * 0.25, 0.25)], MAIN, tip=LIGHT)
        body += els_h
    leg = [box("Vorderbein", [8, 1, 5], [10, 5, 7.5], X1)]
    return dict(kind="pet", parts={"body": body, "flipper_a": leg, "flipper_b": mirror(leg)},
                icon=body + shift(leg, 3.5, 0, 0.5) + shift(mirror(leg), -3.5, 0, 0.5))


def crystal_gemling():
    els = []
    # Oktaeder: oben und unten spitz zulaufend
    for k, w in enumerate((0.8, 1.8, 2.8, 3.6, 3.6, 2.8, 1.8, 0.8)):
        y = 3 + k * 1.3
        els.append(box("Juwel", [8 - w, y, 8 - w], [8 + w, y + 1.3, 8 + w], MAIN if k < 4 else SECOND,
                       north=FACE if k in (3, 4) else (MAIN if k < 4 else SECOND)))
    els += ring("Funkelring", 8, 7.6, 8, 5.2, 0.4, GLOW, count=10, size=0.6)
    return dict(kind="pet", elements=els)


item("crystal", "crystal_cluster_crown", "HEAD", "HALO", "LEGENDARY", "Geodenkrone", "Geode Crown", crystal_cluster_crown, scale=1.15)
item("crystal", "crystal_horn", "HEAD", "NONE", "EPIC", "Kristallhorn", "Crystal Horn", crystal_horn,
     extras=[(hexc(0xC0C8E0), "metal")])
item("crystal", "crystal_prism_wings", "BACK", "WINGS", "LEGENDARY", "Prismenschwingen", "Prism Wings", crystal_prism_wings, scale=1.1)
item("crystal", "crystal_floaters", "BACK", "BACKPACK", "EPIC", "Schwebekristalle", "Floating Crystals", crystal_floaters)
item("crystal", "crystal_tortoise", "PET", "PENGUIN", "LEGENDARY", "Kristallschildkröte", "Crystal Tortoise", crystal_tortoise,
     extras=[hexc(0x8AC878), (hexc(0x8AC878), "face")])
item("crystal", "crystal_gemling", "PET", "GHOST", "EPIC", "Juwelchen", "Gemling", crystal_gemling)


# =========================================================================== NEON
def neon_wire_crown():
    els = []
    for x, z, deg, i in around(8, 5):
        els.append(centered("Strebe", x, 9, z, 0.5, 6, 0.5, GLOW if i % 2 else LIGHT))
        els.append(centered("Spitze", x, 12.4, z, 0.9, 0.9, 0.9, MAIN))
    els += ring("Ring unten", 8, 6, 8, 5, 0.5, MAIN, count=24, size=0.7)
    els += ring("Ring oben", 8, 12, 8, 5, 0.4, SECOND, count=24, size=0.6)
    els += ring("Ring Mitte", 8, 9, 8, 5.4, 0.3, GLOW, count=24, size=0.5)
    return dict(kind="halo", elements=els)


def neon_visor():
    t = T_TOP
    els = [box("Visier", [0.6, 7.5, 0.4], [15.4, 10.5, 1.4], X1),
           box("Leuchtstreifen", [1, 8.4, 0.2], [15, 9.6, 0.4], GLOW),
           box("Bügel links", [0.2, 7.5, 1.4], [1.2, 10, 11], X1), box("Bügel rechts", [14.8, 7.5, 1.4], [15.8, 10, 11], X1),
           box("Band", [1.2, t - 0.4, 6], [14.8, t + 0.6, 8], X1),
           box("Antenne", [13.5, t + 0.6, 7], [14.2, t + 3.5, 7.7], X1), box("Antennenlicht", [13.3, t + 3.5, 6.8], [14.4, t + 4.6, 7.9], MAIN)]
    for x in (3, 6, 10, 13):
        els.append(box("LED", [x - 0.4, 10.7, 0.8], [x + 0.4, 11.3, 1.2], SECOND))
    return dict(kind="head", elements=els)


def neon_holo_wings():
    els = []
    outline = [(8.6, 8), (13, 14), (18, 16), (22, 14), (21.5, 9), (18, 5), (13, 4), (9, 6)]
    for a, b in zip(outline, outline[1:] + outline[:1]):
        pts = densify([(a[0], a[1], 8), (b[0], b[1], 8)], 0.7)
        els += chain("Rahmen", pts, [0.7] * len(pts), GLOW)
    for a in outline[1:]:
        pts = densify([(8.6, 8, 8), (a[0], a[1], 8)], 1.2)
        els += chain("Strebe", pts, [0.4] * len(pts), MAIN)
    for x, y in ((16, 11), (19, 11), (14, 8), (17.5, 7.5)):
        els.append(centered("Knoten", x, y, 8, 1, 1, 1, LIGHT))
    return wings_spec(els)


def neon_cyber_pack():
    els = [box("Gehäuse", [4, 2, 8], [12, 13, 12], X1),
           box("Bildschirm", [5, 7, 12], [11, 11.5, 12.3], GLOW),
           box("Schlitz", [5, 3.5, 12], [11, 4.2, 12.2], MAIN)]
    for x in (4.2, 11.8):
        pts = bezier((x, 12, 10), (x, 16, 12), (x, 12.5, 14), 7)
        els += chain("Leuchtröhre", pts, [0.8] * 7, MAIN)
    els += [box("Lüfter", [6, 12.8, 9], [10, 13.6, 11], DARK), box("Antenne", [11, 13, 10], [11.5, 17, 10.5], X1),
            box("Antennenlicht", [10.8, 17, 9.8], [11.7, 17.9, 10.7], SECOND)]
    return dict(kind="back", elements=els)


def neon_wheelbot():
    body = disc("Rad", 8, 5, 8, 4, 2.4, X1, axis="x", step=0.7)
    body += disc("Felge", 6.7, 5, 8, 2.4, 0.3, MAIN, axis="x", step=0.7)
    body += disc("Felge", 9.3, 5, 8, 2.4, 0.3, MAIN, axis="x", step=0.7)
    head = [box("Kopf", [4.8, 8, 5.5], [11.2, 12.5, 10.5], X1, north=X2),
            box("Hals", [7.2, 7.4, 7.2], [8.8, 8, 8.8], DARK),
            box("Ohrlicht", [4.2, 9.5, 7.5], [4.8, 11, 8.5], MAIN), box("Ohrlicht", [11.2, 9.5, 7.5], [11.8, 11, 8.5], SECOND)]
    return dict(kind="pet", parts={"body": body, "head": head}, icon=body + shift(head, 0, 1, 0))


def neon_drone():
    body = [box("Rumpf", [5.5, 6.5, 5.5], [10.5, 9.5, 10.5], X1, north=X2),
            box("Kamera", [7, 5.5, 5], [9, 6.5, 7], DARK), box("Licht", [7.5, 5.2, 5.3], [8.5, 5.5, 6.3], MAIN)]
    arm = [box("Arm", [8, 8.5, 7.5], [13, 9, 8.5], X1), box("Rotorachse", [12.5, 9, 7.6], [13.3, 10, 8.4], DARK),
           box("Rotor", [10, 10, 7.7], [16, 10.3, 8.3], GLOW)]
    return dict(kind="pet", parts={"body": body, "wing_a": arm, "wing_b": mirror(arm)},
                icon=body + shift(arm, 2, 0, 0) + shift(mirror(arm), -2, 0, 0))


item("neon", "neon_wire_crown", "HEAD", "HALO", "LEGENDARY", "Hologramm-Krone", "Hologram Crown", neon_wire_crown, scale=1.15)
item("neon", "neon_visor", "HEAD", "NONE", "EPIC", "Cyber-Visier", "Cyber Visor", neon_visor,
     extras=[(hexc(0x2A2A36), "metal")])
item("neon", "neon_holo_wings", "BACK", "WINGS", "LEGENDARY", "Hologramm-Schwingen", "Hologram Wings", neon_holo_wings, scale=1.1)
item("neon", "neon_cyber_pack", "BACK", "BACKPACK", "EPIC", "Cyber-Rucksack", "Cyber Pack", neon_cyber_pack,
     extras=[(hexc(0x24242E), "metal")])
item("neon", "neon_wheelbot", "PET", "ROLLER", "LEGENDARY", "Rad-Bot", "Wheel Bot", neon_wheelbot,
     extras=[(hexc(0x30303C), "metal"), (hexc(0x14141C), "face")])
item("neon", "neon_drone", "PET", "BEE", "EPIC", "Mini-Drohne", "Mini Drone", neon_drone,
     extras=[(hexc(0x30303C), "metal"), (hexc(0x14141C), "face")])


# =========================================================================== ZUCKER
def candy_lollipop_crown():
    els = ring("Waffelreif", 8, 6, 8, 5, 1.4, X3, count=16, size=1.6)
    for x, z, deg, i in around(7, 5):
        els.append(centered("Stiel", x, 8.5, z, 0.5, 5, 0.5, X4))
        els += disc("Lolli", x, 12.5, z, 1.8, 0.6, MAIN if i % 2 else SECOND, axis="z", step=0.6)
    els += ball("Kirsche", 8, 8.5, 8, 1.4, X1, step=0.6)
    return dict(kind="halo", elements=els)


def candy_cupcake_hat():
    t = T_TOP
    els = stack("Förmchen", 8, 8, t - 1, [(1.2, 6), (1.2, 6.4), (1, 6.8)], X2)
    for x, z, deg, i in around(16, 6.6):
        els.append(centered("Rillen", x, t + 0.6, z, 0.6, 2.8, 0.6, X3))
    els += stack("Sahne", 8, 8, t + 2.4, [(1.2, 7), (1.2, 6), (1.2, 4.8), (1.2, 3.4), (1, 2)], MAIN, tip=LIGHT)
    els += ball("Kirsche", 8, t + 9, 8, 1.3, X1, step=0.5)
    els.append(box("Kirschstiel", [8.2, t + 10, 7.8], [8.6, t + 11.5, 8.2], X5))
    for x, y, z in ((4, t + 4, 3.5), (11, t + 4.5, 4.5), (6, t + 6, 11), (11.5, t + 4, 11), (3.5, t + 3.5, 9)):
        els.append(centered("Streusel", x, y, z, 0.9, 0.5, 0.4, X4))
    return dict(kind="head", elements=els)


def candy_wings():
    els = []
    for cx, cy, r in ((14, 11.5, 5), (13, 5, 3.6)):
        els += disc("Bonbon", cx, cy, 8, r, 1.0, MAIN, axis="z", step=0.6)
        els += ring("Wirbel", cx, cy, 8.55, r * 0.55, 0.3, LIGHT, count=10, size=0.8, axis="xy")
    els += blade("Verbindung", (8.6, 8), 30, 6, 1.4, SECOND, z=8)
    els += blade("Verbindung", (8.6, 7.6), -20, 5, 1.2, SECOND, z=8)
    return wings_spec(els)


def candy_donut():
    els = []
    for x, y, deg, i in around(16, 4.2, cx=8, cz=9.5):
        els += ball("Teig", x, y, 12, 1.9, X3, step=0.8)
    for x, y, deg, i in around(16, 4.2, cx=8, cz=9.5):
        els.append(centered("Glasur", x, y, 13.6, 2.6, 2.6, 0.8, MAIN))
    for x, y in ((5, 12), (10.5, 12.5), (12.5, 8.5), (4, 7.5), (8, 5.5), (11, 6)):
        els.append(centered("Streusel", x, y, 14.1, 1, 0.5, 0.3, X4))
    els += [box("Riemen", [5, 4, 8], [6, 14, 10], X2), box("Riemen", [10, 4, 8], [11, 14, 10], X2)]
    return dict(kind="back", elements=els)


def candy_gummy_bear():
    els = [box("Bauch", [5, 1, 5.5], [11, 7, 10.5], MAIN), box("Kopf", [5.2, 7, 5.2], [10.8, 11.8, 10.2], MAIN, north=X1),
           box("Schnauze", [6.8, 7.6, 4.4], [9.2, 9.4, 5.2], SECOND),
           box("Ohr", [5.2, 11.8, 7], [6.8, 13.2, 8.4], MAIN), box("Ohr", [9.2, 11.8, 7], [10.8, 13.2, 8.4], MAIN),
           box("Arm", [3.8, 4, 7], [5, 6.5, 8.6], MAIN), box("Arm", [11, 4, 7], [12.2, 6.5, 8.6], MAIN),
           box("Fuß", [5.5, 0.5, 5], [7.5, 1.8, 7.5], MAIN), box("Fuß", [8.5, 0.5, 5], [10.5, 1.8, 7.5], MAIN)]
    return dict(kind="pet", elements=els)


def candy_sheep():
    els = []
    for cx, cy, cz, r in ((8, 8, 8.5, 3.4), (5.6, 8.4, 9, 2.3), (10.4, 8.4, 9, 2.3), (8, 10.4, 9.5, 2.4), (8, 7.6, 11.4, 2.4)):
        els += ball("Zuckerwatte", cx, cy, cz, r, MAIN, step=1)
    els += [box("Köpfchen", [6, 6.4, 4], [10, 10, 6], X2, north=X1),
            box("Ohr", [4.8, 9, 5], [6, 9.8, 5.8], X2), box("Ohr", [10, 9, 5], [11.2, 9.8, 5.8], X2)]
    for x, z in ((6.5, 6.5), (9.5, 6.5), (6.5, 10.5), (9.5, 10.5)):
        els.append(box("Bein", [x - 0.6, 3, z - 0.6], [x + 0.6, 5.5, z + 0.6], X2))
    return dict(kind="pet", elements=els)


item("candy", "candy_lollipop_crown", "HEAD", "HALO", "LEGENDARY", "Lolli-Krone", "Lollipop Crown", candy_lollipop_crown, scale=1.15,
     extras=[hexc(0xE0203A), hexc(0xFFFFFF), hexc(0xE6B874), hexc(0xFFF6F0)])
item("candy", "candy_cupcake_hat", "HEAD", "NONE", "RARE", "Cupcake-Hut", "Cupcake Hat", candy_cupcake_hat,
     extras=[hexc(0xE0203A), hexc(0xF0B8D8), hexc(0xD890B8), hexc(0x6AD8FF), hexc(0x3A7A2A)])
item("candy", "candy_bonbon_wings", "BACK", "WINGS", "LEGENDARY", "Bonbon-Schwingen", "Bonbon Wings", candy_wings, scale=1.1)
item("candy", "candy_donut", "BACK", "BACKPACK", "EPIC", "Riesen-Donut", "Giant Donut", candy_donut,
     extras=[hexc(0xE0203A), hexc(0x9A6A4A), hexc(0xE6B874), hexc(0x6AD8FF)])
item("candy", "candy_gummy_bear", "PET", "MUSHROOM", "EPIC", "Gummibärchen", "Gummy Bear", candy_gummy_bear,
     extras=[(hexc(0xFF7AB0, 230), "face")])
item("candy", "candy_cotton_sheep", "PET", "GHOST", "LEGENDARY", "Zuckerwatte-Schaf", "Cotton Candy Sheep", candy_sheep,
     extras=[(hexc(0xFFF0F6), "face"), hexc(0xFFF0F6)])
