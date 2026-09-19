package dev.nexus.cosmetics.cosmetic;

/** Besonderes Verhalten eines Cosmetics (Haustier-Persönlichkeit, Umhang-Form, Spezial-Hüte). */
public enum CosmeticAnimation {
    /** Nichts Besonderes */
    NONE,

    // Haustiere
    GHOST,
    DRAGON,
    PENGUIN,
    KITTEN,
    BEE,
    MUSHROOM,
    OWL,
    /** Kugel-Droide: rollender Körper, Kopf bleibt oben (Teile _body und _head) */
    ROLLER,

    // Capes
    /** Langer Umhang bis zu den Knöcheln, schwerer Stoff */
    ROBE,
    /** Schlagende Flügel (Modell besteht aus _wing_a und _wing_b) */
    WINGS,
    /** Festes Teil auf dem Rücken: Rucksack, Jetpack, Klinge ... */
    BACKPACK,

    // Kopf
    /** Hut, der spricht (Sprechblase + Mundbewegung) */
    TALKING,
    /** Schwebender Heiligenschein statt Helm-Item */
    HALO
}
