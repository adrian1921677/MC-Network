"""
Welten 1-5: Inferno, Frost, Sturm, Wald, Schatten.
Jedes Cosmetic hat eine eigene Form. Koordinaten in Modell-Einheiten (16 = 1 Block).

Konventionen:
- Kopf (HEAD NONE): der Kopf reicht von 1.6 bis 14.4, Oberkante t = 14.4, vorne = Norden (kleines z)
- Schwebend (HALO): um die Modellmitte (8, 8, 8), dreht sich über dem Kopf
- Rückenteil (BACKPACK): liegt ab z = 8 hinter dem Rücken
- Flügel (WINGS): wing_a wächst vom Gelenk (8, 8, 8) nach +x, wing_b ist gespiegelt
- Haustiere: Teile-Namen und Gelenke passend zur jeweiligen Animation
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


def flame(cx, cz, y, height, width, cell=MAIN, tip=LIGHT, lean=(0.0, 0.0)):
    """Eine Flammenzunge: unten breit, oben spitz."""
    n = max(3, int(height / 1.2))
    layers = [(height / n, width * (1 - 0.8 * i / (n - 1)) / 2 + 0.2) for i in range(n)]
    return stack("Flamme", cx, cz, y, layers, cell, tip=tip, lean=lean)


# =========================================================================== INFERNO
def inferno_crown():
    els = []
    for x, z, deg, i in around(8, 5.2):
        els += flame(x, z, 6.2, 5.5 if i % 2 == 0 else 3.8, 2.6, MAIN, LIGHT,
                     lean=((x - 8) * 0.04, (z - 8) * 0.04))
    els += ring("Glut", 8, 6, 8, 5.2, 1.2, DARK, count=16, size=1.6)
    els += ball("Glutkern", 8, 8.5, 8, 1.7, GLOW)
    return dict(kind="halo", elements=els)


def inferno_horns():
    t = T_TOP
    curl = bezier((3.6, t - 0.4, 7), (-1.5, t + 5.5, 8.5), (0.2, t + 1.2, 13), 8) + \
        bezier((0.2, t + 1.2, 13), (2.2, t - 1.8, 14.5), (2.8, t + 0.8, 13.2), 4)[1:]
    sizes = taper(2.8, 1.1, len(curl))
    left = chain("Horn", curl, sizes, MAIN, tip=GLOW)
    left += flame(curl[-1][0], curl[-1][2], curl[-1][1] + 0.4, 2.4, 1.2, GLOW, LIGHT)
    band = [box("Reif", [1.4, t - 0.8, 5.8], [14.6, t + 0.2, 7.2], DARK)]
    return dict(kind="head", elements=band + left + mirror(left))


def inferno_wings():
    els = []
    for angle, length in ((72, 9), (48, 12.5), (24, 14.5), (2, 13.5), (-20, 11), (-42, 8)):
        els += blade("Glutfeder", (8.6, 8), angle, length, 2.1, MAIN, tip=LIGHT, z=8)
    for angle, length in ((60, 7), (36, 9.5), (12, 10.5), (-10, 8.5)):
        els += blade("Innenfeder", (8.6, 8), angle, length, 1.4, GLOW, tip=LIGHT, z=8.7, thick=0.5)
    return dict(kind="wings", parts={"wing_a": els, "wing_b": mirror(els)}, icon=shift(els, 2) + shift(mirror(els), -2))


def inferno_wheel():
    els = [box("Halterung", [7, 7, 8], [9, 9.5, 11.5], DARK)]
    els += ring("Feuerrad", 8, 9, 12, 5.6, 1.2, MAIN, count=18, size=1.7, axis="z")
    for x, y, deg, i in around(8, 5.6, cx=8, cz=9):
        rad = math.radians(deg)
        pts = [(8 + math.cos(rad) * r, 9 + math.sin(rad) * r, 12) for r in (6.8, 7.9, 8.8)]
        els += chain("Flamme", pts, [1.4, 1.0, 0.6], GLOW, tip=LIGHT)
        spoke = [(8 + math.cos(rad) * r, 9 + math.sin(rad) * r, 12) for r in (1.8, 3.2, 4.4)]
        els += chain("Speiche", spoke, [0.7, 0.7, 0.7], DARK)
    els += ball("Nabe", 8, 9, 12, 1.6, GLOW)
    return dict(kind="back", elements=els)


def inferno_salamander():
    body = [
        box("Körper", [4.5, 2, 4], [11.5, 8, 13], MAIN, north=SECOND),
        box("Kopf", [5, 3, 0.2], [11, 8.5, 4.5], MAIN, north=FACE),
        box("Maul", [5.5, 2.5, -0.6], [10.5, 4, 1], SECOND),
        box("Hinterbein links", [3.4, 1, 10], [5, 3.5, 12.5], DARK),
        box("Hinterbein rechts", [11, 1, 10], [12.6, 3.5, 12.5], DARK),
    ]
    tail = bezier((8, 4, 13), (8, 3, 17.5), (11.5, 4.5, 18.5), 6)
    body += chain("Schwanz", tail, taper(3, 1.1, 6), SECOND, tip=GLOW)
    for z, h in ((5, 2.5), (7.5, 3.5), (10, 3), (12.2, 2)):
        body += flame(8, z, 8, h, 1.8, GLOW, LIGHT)
    leg_a = [box("Vorderbein", [8, 1, 5.5], [9.5, 8, 7.5], DARK), box("Kralle", [8, 1, 4.8], [9.7, 1.6, 7.5], GLOW)]
    return dict(kind="pet", parts={"body": body, "flipper_a": leg_a, "flipper_b": mirror(leg_a)},
                icon=body + shift(leg_a, 3.5, 0, 0.5) + shift(mirror(leg_a), -3.5, 0, 0.5))


def inferno_flameling():
    els = [box("Körper", [4.6, 3.5, 4.6], [11.4, 9.5, 11.4], MAIN, north=FACE)]
    els += stack("Flammenleib", 8, 8, 1.5, [(1, 2.2), (1, 3.0)], SECOND)
    els += flame(8, 8.4, 9.5, 5.5, 5.6, MAIN, LIGHT)
    els += flame(5.6, 8, 8.8, 3.2, 2.2, GLOW, LIGHT, lean=(-0.25, 0))
    els += flame(10.4, 8, 8.8, 3.2, 2.2, GLOW, LIGHT, lean=(0.25, 0))
    els += chain("Ärmchen", [(3.8, 6.2, 8), (2.8, 7, 7.6)], [1.4, 1.0], SECOND, tip=GLOW)
    els += chain("Ärmchen", [(12.2, 6.2, 8), (13.2, 7, 7.6)], [1.4, 1.0], SECOND, tip=GLOW)
    return dict(kind="pet", elements=els)


item("inferno", "inferno_flame_crown", "HEAD", "HALO", "LEGENDARY", "Flammenkrone", "Flame Crown", inferno_crown, scale=1.15)
item("inferno", "inferno_ram_horns", "HEAD", "NONE", "EPIC", "Glutwidder-Hörner", "Ember Ram Horns", inferno_horns)
item("inferno", "inferno_ember_wings", "BACK", "WINGS", "LEGENDARY", "Glutschwingen", "Ember Wings", inferno_wings, scale=1.15)
item("inferno", "inferno_fire_wheel", "BACK", "BACKPACK", "EPIC", "Feuerrad", "Fire Wheel", inferno_wheel)
item("inferno", "inferno_salamander", "PET", "PENGUIN", "LEGENDARY", "Glut-Salamander", "Ember Salamander", inferno_salamander)
item("inferno", "inferno_flameling", "PET", "GHOST", "EPIC", "Flämmchen", "Flameling", inferno_flameling)


# =========================================================================== FROST
def frost_crown():
    els = ring("Reif", 8, 7, 8, 5, 1.4, LIGHT, count=16, size=1.5)
    for x, z, deg, i in around(12, 5):
        length = (4.5, 2.5, 3.5)[i % 3]
        for k in range(int(length)):
            w = 1.3 * (1 - k / length) + 0.3
            els.append(centered("Eiszapfen", x, 6.2 - k - 0.5, z, w, 1.0, w, MAIN if k < length - 1 else LIGHT))
    for x, z, deg, i in around(6, 5, phase=0.26):
        els += stack("Kristall", x, z, 7.7, [(1.5, 0.8), (1.5, 0.6), (1.2, 0.35)], SECOND, tip=LIGHT)
    els.append(box("Schneeflocke", [3.5, 7.8, 7.6], [12.5, 8.2, 8.4], GLOW))
    els.append(box("Schneeflocke", [7.6, 7.8, 3.5], [8.4, 8.2, 12.5], GLOW))
    els.append(rot(box("Schneeflocke", [3.5, 7.8, 7.6], [12.5, 8.2, 8.4], GLOW), 45))
    els.append(rot(box("Schneeflocke", [7.6, 7.8, 3.5], [8.4, 8.2, 12.5], GLOW), 45))
    return dict(kind="halo", elements=els)


def frost_antlers():
    t = T_TOP
    beam = bezier((4, t - 0.3, 7), (1.5, t + 4, 7.5), (0.2, t + 8, 9.5), 7)
    left = chain("Geweih", beam, taper(1.7, 0.8, 7), MAIN, tip=LIGHT)
    for start, end in ((3, (-1.8, t + 5.5, 6)), (4, (2.2, t + 8, 7)), (5, (-1.2, t + 8.5, 11.5))):
        p0 = beam[start]
        mid = ((p0[0] + end[0]) / 2, (p0[1] + end[1]) / 2 + 0.6, (p0[2] + end[2]) / 2)
        left += chain("Spross", bezier(p0, mid, end, 4)[1:], [1.0, 0.8, 0.6], SECOND, tip=GLOW)
    band = [box("Reif", [1.4, t - 0.8, 6], [14.6, t + 0.2, 7.4], DARK)]
    return dict(kind="head", elements=band + left + mirror(left))


def frost_wings():
    els = []
    for angle, length, width, cell in ((78, 7, 2.6, MAIN), (52, 10.5, 3.2, LIGHT), (26, 13, 3.4, MAIN),
                                       (0, 11, 3.0, LIGHT), (-24, 8, 2.6, MAIN)):
        els += blade("Kristallsplitter", (8.8, 8), angle, length, width, cell, tip=GLOW, thick=1.2, taper_to=0.2)
    for angle, length in ((64, 5.5), (38, 7.5), (13, 8)):
        els += blade("Splitter", (9.5, 8.5), angle, length, 1.4, SECOND, tip=LIGHT, z=8.9, thick=0.6, taper_to=0.2)
    return dict(kind="wings", parts={"wing_a": els, "wing_b": mirror(els)}, icon=shift(els, 2) + shift(mirror(els), -2))


def frost_cape():
    top = [cape_box("Fellkragen", [2.2, 14.2, 7.0], [13.8, 16.8, 9.6], CAPE_EDGE),
           cape_box("Fellkragen Rand", [1.6, 14.6, 7.4], [14.4, 16.2, 9.2], CAPE_EDGE)]
    bottom = []
    for i, x in enumerate([3.4 + k * 1.3 for k in range(8)]):
        length = (3.2, 1.6, 2.6, 1.2, 3.6, 2.0, 1.4, 2.8)[i]
        bottom.append(cape_box("Eiszapfen", [x, 10 - length, 7.7], [x + 0.9, 10, 8.3], CAPE_EDGE))
    return dict(kind="cape", top=top, bottom=bottom)


def frost_polar_cub():
    body = [
        box("Körper", [4, 1.5, 5], [12, 8, 12], LIGHT, north=MAIN),
        box("Kopf", [4.5, 8, 4.5], [11.5, 13, 10.5], LIGHT, north=X1),
        box("Schnauze", [6.3, 8.5, 3.6], [9.7, 10.6, 4.6], LIGHT),
        box("Nase", [7.3, 9.8, 3.4], [8.7, 10.6, 3.7], DARK),
        box("Ohr links", [4.5, 13, 7], [6.5, 14.6, 8.5], LIGHT),
        box("Ohr rechts", [9.5, 13, 7], [11.5, 14.6, 8.5], LIGHT),
        box("Fuß links", [4.8, 1, 4.2], [7.2, 2, 7], LIGHT),
        box("Fuß rechts", [8.8, 1, 4.2], [11.2, 2, 7], LIGHT),
        box("Eisschal", [4.2, 7.3, 4.8], [11.8, 8.3, 11.8], GLOW),
    ]
    arm = [box("Tatze", [8, 2.5, 6.5], [9.8, 8, 9.5], LIGHT)]
    return dict(kind="pet", parts={"body": body, "flipper_a": arm, "flipper_b": mirror(arm)},
                icon=body + shift(arm, 3.5, 0, 0.5) + shift(mirror(arm), -3.5, 0, 0.5))


def frost_moth():
    body = [
        box("Leib", [6.6, 6, 5.5], [9.4, 9.5, 12.5], LIGHT),
        box("Kopf", [6.2, 7, 3], [9.8, 10.5, 5.5], LIGHT, north=X1),
        box("Hinterleib", [7, 6.3, 12.5], [9, 8.8, 15], SECOND),
    ]
    body += chain("Fühler", bezier((7.2, 10.5, 3.6), (6, 13.5, 2.5), (4.8, 13.2, 0.5), 4), [0.7, 0.6, 0.6, 0.9], GLOW)
    body += chain("Fühler", bezier((8.8, 10.5, 3.6), (10, 13.5, 2.5), (11.2, 13.2, 0.5), 4), [0.7, 0.6, 0.6, 0.9], GLOW)
    wing = [box("Flügel oben", [8, 8, 4.5], [15.5, 8.6, 10], MAIN), box("Flügelrand", [15.5, 8, 5.5], [16.5, 8.6, 9], LIGHT),
            box("Flügel unten", [8, 7.9, 10], [13.5, 8.5, 14], SECOND), box("Muster", [11, 8.6, 6.2], [13, 8.8, 8.2], GLOW)]
    return dict(kind="pet", parts={"body": body, "wing_a": wing, "wing_b": mirror(wing)},
                icon=body + shift(wing, 3, 2, 0.5) + shift(mirror(wing), -3, 2, 0.5))


item("frost", "frost_icicle_crown", "HEAD", "HALO", "LEGENDARY", "Eiszapfenkrone", "Icicle Crown", frost_crown, scale=1.15)
item("frost", "frost_antlers", "HEAD", "NONE", "EPIC", "Frostgeweih", "Frost Antlers", frost_antlers)
item("frost", "frost_crystal_wings", "BACK", "WINGS", "LEGENDARY", "Kristallschwingen", "Crystal Shard Wings", frost_wings, scale=1.15)
item("frost", "frost_cape", "BACK", "NONE", "EPIC", "Frostmantel", "Frost Mantle", frost_cape, scale=1.05)
item("frost", "frost_polar_cub", "PET", "PENGUIN", "EPIC", "Eisbärjunges", "Polar Cub", frost_polar_cub,
     extras=[(hexc(0xF4FAFF), "face")])
item("frost", "frost_moth", "PET", "DRAGON", "LEGENDARY", "Frostfalter", "Frost Moth", frost_moth,
     extras=[(hexc(0xE8F6FF), "face")])


# =========================================================================== STURM
def storm_cloud():
    els = []
    for cx, cy, cz, r in ((8, 10.5, 8, 3.3), (5, 9.8, 7.5, 2.6), (11, 9.8, 8.5, 2.6), (7, 10.2, 11, 2.4),
                          (9.5, 10.4, 5, 2.4), (8, 12.3, 8.5, 2.2)):
        els += ball("Wolke", cx, cy, cz, r, SECOND if cy < 10 else LIGHT, step=1.2)
    bolt = densify([(8, 7.4, 8), (9.3, 6.2, 8), (7.6, 5.0, 8), (9.0, 3.6, 8), (7.9, 2.3, 8), (8.6, 1.0, 8)], 0.5)
    els += chain("Blitz", bolt, taper(1.1, 0.6, len(bolt)), GLOW, tip=LIGHT)
    for x, z in ((5.5, 6.5), (10.5, 9.5), (6.5, 10.5)):
        els.append(centered("Regen", x, 6.5, z, 0.3, 1.2, 0.3, MAIN))
    return dict(kind="halo", elements=els)


def storm_helm():
    t = T_TOP
    els = [box("Helm", [1.2, t - 3, 1.2], [14.8, t + 1, 14.8], X1),
           box("Kuppel", [3, t + 1, 3], [13, t + 2.2, 13], X1),
           box("Stirnband", [1, t - 3.2, 1], [15, t - 2.2, 15], GLOW)]
    bolt = [(8, t + 2.4, 5), (8, t + 3.6, 6.8), (8, t + 3.1, 8.6), (8, t + 4.3, 10.4), (8, t + 3.8, 12.2)]
    els += chain("Blitzkamm", bolt, [(1, 1.4, 1.6)] * 5, GLOW, tip=LIGHT)
    wing = []
    for i, (dy, dz, length) in enumerate(((0, 0, 5), (1.2, 1.6, 4.2), (2.4, 3.2, 3.2))):
        pts = [(0.7, t - 1.5 + dy + k * 0.9, 8 + dz + k * 0.7) for k in range(int(length))]
        wing += chain("Helmflügel", pts, [(0.6, 1.1, 1.3)] * len(pts), LIGHT, tip=GLOW)
    return dict(kind="head", elements=els + wing + mirror(wing))


def storm_wings():
    els = []
    for angle, length in ((58, 10), (28, 13.5), (-4, 11.5)):
        rad, perp = math.radians(angle), math.radians(angle + 90)
        pts = []
        for k in range(8):
            d = length * k / 7
            zig = (1.4 if k % 2 else -1.4) * (1 - k / 9)
            pts.append((8.6 + math.cos(rad) * d + math.cos(perp) * zig, 8 + math.sin(rad) * d + math.sin(perp) * zig, 8))
        pts = densify(pts, 0.7)
        els += chain("Blitz", pts, taper(2.2, 0.9, len(pts)), GLOW, tip=LIGHT)
    els += blade("Gewitterhaut", (8.6, 7.6), 20, 10, 3.2, SECOND, z=8.4, thick=0.4, taper_to=0.3)
    return dict(kind="wings", parts={"wing_a": els, "wing_b": mirror(els)}, icon=shift(els, 2) + shift(mirror(els), -2))


def storm_tesla():
    els = [box("Rückenplatte", [4.5, 2.5, 8], [11.5, 10.5, 9.2], X1)]
    for x in (5, 11):
        els += stack("Spule", x, 11.5, 1.5, [(1.2, 1.6), (0.8, 1.3), (0.6, 1.6), (0.8, 1.3), (0.6, 1.6), (0.8, 1.3),
                                             (0.6, 1.6), (0.8, 1.3), (1.2, 0.7)], X1)
        for y in (3.4, 5.2, 7.0):
            els.append(centered("Kupferwicklung", x, y, 11.5, 2.8, 0.5, 2.8, X2))
        els += ball("Kugel", x, 11.6, 11.5, 1.7, GLOW)
    bolt = [(5.8, 12.4, 11.5), (7, 13.4, 11.5), (8, 12.2, 11.5), (9, 13.3, 11.5), (10.2, 12.4, 11.5)]
    els += chain("Lichtbogen", bolt, [0.8] * 5, LIGHT)
    return dict(kind="back", elements=els)


def storm_serpent():
    head = [box("Kopf", [5, 5.5, 3.5], [11, 10.5, 11], MAIN, north=FACE),
            box("Schnauze", [6, 5.5, 0.5], [10, 8.6, 3.5], SECOND),
            box("Unterkiefer", [6.3, 4.6, 1], [9.7, 5.6, 5], DARK)]
    head += chain("Horn", bezier((6, 10.5, 8), (5, 13.5, 10), (4.4, 13, 13.5), 4), taper(1.3, 0.6, 4), LIGHT, tip=GLOW)
    head += chain("Horn", bezier((10, 10.5, 8), (11, 13.5, 10), (11.6, 13, 13.5), 4), taper(1.3, 0.6, 4), LIGHT, tip=GLOW)
    head += chain("Barthaar", bezier((6, 6.5, 1.2), (3, 6, 0.5), (1.2, 4.2, 2), 4), [0.5] * 4, GLOW)
    head += chain("Barthaar", bezier((10, 6.5, 1.2), (13, 6, 0.5), (14.8, 4.2, 2), 4), [0.5] * 4, GLOW)
    body = [box("Glied", [5.4, 5.4, 4.8], [10.6, 10.6, 11.2], MAIN, down=LIGHT),
            box("Rückenkamm", [7.4, 10.6, 5.5], [8.6, 12.4, 10], GLOW),
            box("Bauchschuppen", [6, 5, 5.2], [10, 5.4, 10.8], LIGHT)]
    tail = [box("Schwanz", [6.4, 6.4, 3.5], [9.6, 9.6, 10], MAIN),
            box("Schwanzquaste", [6.8, 6.8, 10], [9.2, 11.5, 13.5], GLOW), box("Quaste", [7.2, 5, 11], [8.8, 9, 14.5], LIGHT)]
    icon = head + shift(body, 0, 0, 5.2) + shift(body, 0, 0, 10.4) + shift(tail, 0, 0, 15.6)
    return dict(kind="pet", parts={"head": head, "body": body, "tail": tail}, icon=icon)


def storm_sparkball():
    els = ball("Funkenball", 8, 4.6, 8, 3.6, MAIN, step=1.0)
    els.append(box("Gesicht", [5.4, 3.2, 4.2], [10.6, 6.6, 4.5], X1, north=X1))
    for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, 0, 1), (0.7, 0.7, 0), (-0.7, 0.7, 0), (0, 0.7, 0.7),
                       (0.7, 0.4, 0.7), (-0.7, 0.4, 0.7)):
        pts = [(8 + dx * r, 4.6 + dy * r, 8 + dz * r) for r in (4.1, 5.0, 5.8)]
        els += chain("Stachel", pts, [1.1, 0.8, 0.5], GLOW, tip=LIGHT)
    return dict(kind="pet", elements=els)


item("storm", "storm_thundercloud", "HEAD", "HALO", "LEGENDARY", "Gewitterwolke", "Thundercloud", storm_cloud, scale=1.2)
item("storm", "storm_winged_helm", "HEAD", "NONE", "EPIC", "Blitzflügelhelm", "Winged Storm Helm", storm_helm,
     extras=[(hexc(0xC0C8D8), "metal")])
item("storm", "storm_bolt_wings", "BACK", "WINGS", "LEGENDARY", "Blitzschwingen", "Lightning Wings", storm_wings, scale=1.15)
item("storm", "storm_tesla_coils", "BACK", "BACKPACK", "EPIC", "Tesla-Spulen", "Tesla Coils", storm_tesla,
     extras=[(hexc(0x8A90A0), "metal"), (hexc(0xD2823C), "metal")])
item("storm", "storm_serpent", "PET", "SERPENT", "LEGENDARY", "Donnerdrache", "Thunder Serpent", storm_serpent)
item("storm", "storm_sparkball", "PET", "MUSHROOM", "EPIC", "Funkenball", "Sparkball", storm_sparkball,
     extras=[(hexc(0x2E3A78), "face")])


# =========================================================================== WALD
def forest_wreath():
    els = []
    for x, z, deg, i in around(10, 5.4):
        els += disc("Blatt", x, 8 + (0.6 if i % 2 else 0), z, 2.2, 0.5, MAIN if i % 2 else SECOND, axis="y", step=0.8)
        els.append(centered("Blattader", x, 8.4 + (0.6 if i % 2 else 0), z, 0.4, 0.3, 3.4, LIGHT))
    for x, z, deg, i in around(5, 5.4, phase=0.3):
        els += ball("Beere", x, 9.3, z, 0.9, X1, step=0.6)
    for x, z, deg, i in around(3, 5.4, phase=1.2):
        els.append(centered("Blüte", x, 9.4, z, 2, 0.6, 2, X2))
        els.append(centered("Blütenmitte", x, 9.8, z, 0.8, 0.4, 0.8, GLOW))
    return dict(kind="halo", elements=els)


def forest_toadstool():
    t = T_TOP
    els = ball("Pilzhut", 8, t - 0.3, 8, 8.4, X1, step=0.7, squash=0.62, top_only=True)
    els.append(box("Lamellen", [0.4, t - 0.6, 0.4], [15.6, t - 0.2, 15.6], X3))
    # Weiße Punkte sitzen auf der Oberfläche der Kappe
    for ax, az, height in ((0.55, 0.3, 0.45), (-0.6, 0.35, 0.4), (0.1, -0.7, 0.42), (-0.25, 0.1, 0.9),
                           (0.62, -0.45, 0.35), (-0.55, -0.4, 0.5), (0.3, 0.7, 0.3)):
        r = 8.4 * math.sqrt(max(0.0, 1 - height ** 2))
        norm = math.hypot(ax, az) or 1
        x, z = 8 + ax / norm * r * min(1, math.hypot(ax, az) * 1.4), 8 + az / norm * r * min(1, math.hypot(ax, az) * 1.4)
        els.append(centered("Punkt", x, t - 0.3 + 8.4 * 0.62 * height + 0.35, z, 2, 0.7, 2, X2))
    els.append(centered("Blatt", 13.5, t + 0.8, 3, 3, 0.4, 1.6, MAIN))
    els += ball("Marienkäfer", 5, t + 3.5, 8, 0.9, X1, step=0.5)
    return dict(kind="head", elements=els)


def forest_wings():
    els = []
    length, angle = 15, 22
    rad = math.radians(angle)
    for k in range(14):
        d = length * k / 13
        w = max(0.9, 6.0 * math.sin(math.pi * (d + 0.5) / (length + 1)))
        els.append(centered("Blatt", 8.6 + math.cos(rad) * d, 8 + math.sin(rad) * d, 8, w, w, 0.6, MAIN))
    els += blade("Blattrippe", (8.6, 8), angle, length - 1, 0.7, LIGHT, z=8.45, thick=0.3, taper_to=0.5)
    rad2 = math.radians(-18)
    for k in range(9):
        d = 9 * k / 8
        w = max(0.7, 3.6 * math.sin(math.pi * (d + 0.5) / 10))
        els.append(centered("Blatt klein", 9 + math.cos(rad2) * d, 7.2 + math.sin(rad2) * d, 8.3, w, w, 0.5, SECOND))
    return dict(kind="wings", parts={"wing_a": els, "wing_b": mirror(els)}, icon=shift(els, 2) + shift(mirror(els), -2))


def forest_log():
    els = []
    for x in (3.5, 5.5, 7.5, 9.5, 11.5):
        els += disc("Baumstamm", x, 9, 12, 3, 2.05, X1, axis="x", step=0.8)
    els += disc("Jahresringe", 2.4, 9, 12, 2.6, 0.2, X3, axis="x", step=0.8)
    els += disc("Jahresringe", 13.6, 9, 12, 2.6, 0.2, X3, axis="x", step=0.8)
    els.append(box("Moos", [3, 11.6, 10.2], [13, 12.4, 13.8], MAIN))
    els += chain("Setzling", [(6, 12.8, 12), (6.2, 14, 12), (6.5, 15.2, 12)], [0.6] * 3, X1)
    els += ball("Setzling-Krone", 6.6, 16.2, 12, 1.4, MAIN, step=0.6)
    els += stack("Pilz", 10.5, 12.5, 12.3, [(1, 0.35), (0.6, 1.1), (0.4, 0.7)], X2)
    els += [box("Gurt", [4.5, 6, 8], [5.5, 12.5, 9.5], DARK), box("Gurt", [10.5, 6, 8], [11.5, 12.5, 9.5], DARK)]
    return dict(kind="back", elements=els)


def forest_squirrel():
    body = [box("Körper", [6, 4, 7], [10, 8, 10.5], X1, north=X2),
            box("Kopf", [5.5, 8, 5], [10.5, 12, 9.5], X1, north=X4),
            box("Ohr links", [5.6, 12, 6.8], [6.8, 13.8, 7.8], X1), box("Ohr rechts", [9.2, 12, 6.8], [10.4, 13.8, 7.8], X1),
            box("Ohrpinsel", [5.8, 13.8, 7], [6.6, 14.6, 7.6], X3), box("Ohrpinsel", [9.4, 13.8, 7], [10.2, 14.6, 7.6], X3),
            box("Eichel", [7, 5.4, 5.6], [9, 7.2, 7], X3), box("Eichelhut", [6.8, 7.2, 5.4], [9.2, 7.8, 7.2], X1)]
    tail = chain("Schwanz", [(8, 8.5, 9), (8, 10.5, 10.6), (8, 13, 11.2), (8, 15, 10.4), (8, 16, 8.8)],
                 [2.6, 3.2, 3.6, 3.4, 2.8], X1, tip=X2)
    sleep = [box("Körper", [4.8, 4, 6.5], [11.2, 7.5, 11.5], X1),
             box("Kopf", [5.2, 4, 3], [10.8, 8, 7], X1, north=X5),
             box("Ohr", [5.5, 8, 4.4], [6.8, 9.4, 5.4], X1), box("Ohr", [9.2, 8, 4.4], [10.5, 9.4, 5.4], X1)]
    sleep += chain("Schwanzdecke", [(3.8, 6.5, 10), (3.8, 8, 7), (5.5, 8.6, 4.5)], [3, 3.2, 2.8], X1, tip=X2)
    return dict(kind="pet", parts={"body": body, "tail": tail, "sleep": sleep}, icon=body + shift(tail, 0, -3, 3))


def forest_treant():
    body = [box("Stamm", [5, 1.5, 5.5], [11, 9, 11], X1, north=X4),
            box("Wurzel", [4, 1, 4.5], [6.5, 2, 7.5], X1), box("Wurzel", [9.5, 1, 4.5], [12, 2, 7.5], X1),
            box("Wurzel", [6.5, 1, 10.5], [9.5, 2, 12.5], X1)]
    for cx, cy, cz, r in ((8, 11.5, 8.5, 3.4), (5.5, 10.8, 9, 2.2), (10.5, 10.8, 9, 2.2), (8, 13.5, 9.5, 2.2)):
        body += ball("Krone", cx, cy, cz, r, MAIN if r > 3 else SECOND, step=1)
    body += ball("Apfel", 10.4, 12.5, 6, 0.8, X2, step=0.5)
    arm = chain("Ast", [(8.6, 7.5, 8), (9.2, 6, 7.2), (9.6, 4.6, 6.6)], [1.2, 1.0, 0.8], X1)
    arm += ball("Blätter", 9.8, 3.8, 6.2, 1.1, MAIN, step=0.6)
    return dict(kind="pet", parts={"body": body, "flipper_a": arm, "flipper_b": mirror(arm)},
                icon=body + shift(arm, 3.5, 0, 0.5) + shift(mirror(arm), -3.5, 0, 0.5))


item("forest", "forest_leaf_wreath", "HEAD", "HALO", "LEGENDARY", "Blätterkranz", "Leaf Wreath", forest_wreath, scale=1.1)
item("forest", "forest_toadstool_hat", "HEAD", "NONE", "RARE", "Fliegenpilz-Hut", "Toadstool Hat", forest_toadstool,
     extras=[hexc(0xD8282C), hexc(0xFFFFFF), hexc(0xE6D2A8)])
item("forest", "forest_leaf_wings", "BACK", "WINGS", "LEGENDARY", "Blattschwingen", "Leaf Wings", forest_wings, scale=1.1)
item("forest", "forest_moss_log", "BACK", "BACKPACK", "EPIC", "Moos-Baumstamm", "Mossy Log Pack", forest_log,
     extras=[hexc(0x6A4426), hexc(0xD8282C), hexc(0xC8A070)])
item("forest", "forest_squirrel", "PET", "KITTEN", "EPIC", "Eichhörnchen", "Squirrel", forest_squirrel,
     extras=[hexc(0xC0642C), hexc(0xF6E6CC), hexc(0x7A3E18), (hexc(0xC0642C), "face"), (hexc(0xC0642C), "sleep")])
item("forest", "forest_treant", "PET", "PENGUIN", "LEGENDARY", "Baumgeist", "Little Treant", forest_treant,
     extras=[hexc(0x7A5030), hexc(0xE83A3A), hexc(0x5A3A20), (hexc(0x7A5030), "face")])


# =========================================================================== SCHATTEN
def shadow_crown():
    els = ring("Reif", 8, 6.5, 8, 5, 1.4, DARK, count=16, size=1.5)
    heights = (5.5, 2.5, 4.0, 6.5, 1.5, 4.8, 3.0, 5.8)
    for (x, z, deg, i), h in zip(around(8, 5), heights):
        lean = ((x - 8) * 0.06 * (1 if i % 3 else -1), (z - 8) * 0.06)
        els += stack("Zacke", x, z, 7.2, [(h * 0.4, 0.8), (h * 0.35, 0.55), (h * 0.25, 0.3)], MAIN, tip=GLOW, lean=lean)
    for x, y, z in ((10.5, 13.5, 6), (5, 12.8, 10), (8.5, 14.5, 10.5), (4.8, 13.8, 5.5)):
        els.append(centered("Splitter", x, y, z, 0.8, 1.6, 0.8, GLOW))
    return dict(kind="halo", elements=els)


def shadow_hood():
    t = T_TOP
    els = [box("Kapuze oben", [0.8, t - 0.6, 1.2], [15.2, t + 1.4, 15.6], MAIN),
           box("Kapuze links", [0.4, 1.2, 1.2], [1.6, t, 15.6], MAIN),
           box("Kapuze rechts", [14.4, 1.2, 1.2], [15.6, t, 15.6], MAIN),
           box("Kapuze hinten", [1.6, 1.2, 14.4], [14.4, t, 15.6], MAIN),
           box("Zipfel", [6, t + 1, 12], [10, t + 2.4, 17], SECOND),
           box("Zipfelspitze", [7, t + 0.6, 17], [9, t + 1.6, 19.5], SECOND),
           box("Schatten", [1.6, 1.6, 1.4], [14.4, 14.4, 1.6], DARK),
           box("Auge links", [4, 8.5, 1.1], [6.6, 9.8, 1.4], GLOW),
           box("Auge rechts", [9.4, 8.5, 1.1], [12, 9.8, 1.4], GLOW),
           box("Kapuzenrand", [0.4, t - 0.8, 0.6], [15.6, t + 1, 1.2], SECOND),
           box("Umhang-Schulter", [-0.5, 0, 2], [16.5, 1.4, 15], SECOND)]
    return dict(kind="head", elements=els)


def shadow_wings():
    els = []
    lengths = (8, 11, 13, 14, 12, 9.5, 7)
    for (angle, length) in zip((68, 50, 32, 14, -4, -22, -40), lengths):
        els += blade("Rauchfeder", (8.6, 8), angle, length, 2.0, MAIN if angle % 2 else SECOND, tip=GLOW,
                     thick=0.7, taper_to=0.3)
        rad = math.radians(angle)
        tip = (8.6 + math.cos(rad) * (length + 1.2), 8 + math.sin(rad) * (length + 1.2) - 0.8)
        els.append(centered("Rauchfetzen", tip[0], tip[1], 8, 0.8, 0.8, 0.5, GLOW))
    return dict(kind="wings", parts={"wing_a": els, "wing_b": mirror(els)}, icon=shift(els, 2) + shift(mirror(els), -2))


def shadow_cape():
    bottom = []
    x = 3.0
    lengths = (4.5, 2.0, 3.2, 5.0, 1.5, 3.8, 2.5, 4.2)
    for length in lengths:
        bottom.append(cape_box("Fetzen", [x, 10 - length, 7.6], [x + 1.25, 10, 8.4], CAPE_OUTER))
        x += 1.25
    top = [cape_box("Hoher Kragen", [2.4, 15, 7.6], [13.6, 18.5, 9], CAPE_EDGE)]
    return dict(kind="cape", top=top, bottom=bottom)


def shadow_wraith():
    els = [box("Kapuze", [4.2, 8, 4.4], [11.8, 14.5, 12], MAIN),
           box("Gesicht", [5.2, 8.6, 4.2], [10.8, 13.2, 4.5], DARK),
           box("Auge", [5.9, 10.6, 4.0], [7.5, 11.6, 4.2], GLOW), box("Auge", [8.5, 10.6, 4.0], [10.1, 11.6, 4.2], GLOW),
           box("Zipfel", [7, 14.5, 9], [9, 16, 12.5], SECOND)]
    els += stack("Gewand", 8, 8.4, 2, [(1.5, 2), (1.5, 2.8), (1.5, 3.4), (1.5, 3.8)], SECOND)
    for x, z, h in ((5.2, 6, 2.2), (8, 5.2, 1.4), (10.8, 6, 2.6), (6, 11, 1.8), (10, 11, 2.0)):
        els.append(box("Fetzen", [x - 0.7, 2 - h, z - 0.7], [x + 0.7, 2, z + 0.7], SECOND))
    els += chain("Arm", [(3.6, 7, 8), (2.6, 6, 7)], [1.4, 1.1], SECOND, tip=GLOW)
    els += chain("Arm", [(12.4, 7, 8), (13.4, 6, 7)], [1.4, 1.1], SECOND, tip=GLOW)
    return dict(kind="pet", elements=els)


def shadow_raven():
    body = [box("Körper", [5.5, 4.5, 6], [10.5, 9.5, 11], MAIN),
            box("Schwanz", [6.5, 5, 11], [9.5, 6, 15.5], DARK),
            box("Fuß", [6.2, 3.5, 5.5], [7.4, 4.5, 7], X1), box("Fuß", [8.6, 3.5, 5.5], [9.8, 4.5, 7], X1)]
    head = [box("Kopf", [5, 8, 5], [11, 12.5, 10.5], MAIN, north=FACE),
            box("Schnabel", [7.3, 9, 3], [8.7, 10.4, 5], X1), box("Schnabelspitze", [7.6, 8.7, 2.2], [8.4, 9.8, 3], X1),
            box("Federschopf", [7.4, 12.5, 7], [8.6, 14, 9.5], GLOW)]
    wing = [box("Flügel", [8, 3.2, 6.2], [9, 8.2, 12], SECOND), box("Flügelspitze", [8, 2.4, 10.5], [8.8, 3.4, 13], DARK)]
    icon = body + shift(head, 0, 2, 0.25) + shift(wing, 3, 1.5, 0.5) + shift(mirror(wing), -3, 1.5, 0.5)
    return dict(kind="pet", parts={"body": body, "head": head, "wing_a": wing, "wing_b": mirror(wing)}, icon=icon)


item("shadow", "shadow_broken_crown", "HEAD", "HALO", "LEGENDARY", "Zerbrochene Krone", "Broken Crown", shadow_crown, scale=1.15)
item("shadow", "shadow_hood", "HEAD", "NONE", "EPIC", "Schattenkapuze", "Shadow Hood", shadow_hood)
item("shadow", "shadow_smoke_wings", "BACK", "WINGS", "LEGENDARY", "Rauchschwingen", "Smoke Wings", shadow_wings, scale=1.15)
item("shadow", "shadow_tattered_cape", "BACK", "NONE", "EPIC", "Zerrissener Umhang", "Tattered Cloak", shadow_cape, scale=1.05)
item("shadow", "shadow_wraith", "PET", "GHOST", "LEGENDARY", "Nachtmahr", "Wraith", shadow_wraith)
item("shadow", "shadow_raven", "PET", "OWL", "EPIC", "Schattenrabe", "Shadow Raven", shadow_raven,
     extras=[hexc(0x6A6A78)])
