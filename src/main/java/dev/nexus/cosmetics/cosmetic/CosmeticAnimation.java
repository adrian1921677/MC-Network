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

    // Capes
    /** Langer Umhang bis zu den Knöcheln, schwerer Stoff */
    ROBE,

    // Kopf
    /** Hut, der spricht (Sprechblase + Mundbewegung) */
    TALKING,
    /** Schwebender Heiligenschein statt Helm-Item */
    HALO
}
