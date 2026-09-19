package dev.nexus.cosmetics.cosmetic;

/**
 * Seltenheit eines Cosmetics oder Emotes. Bestimmt die Gewinnchance in Truhen
 * und den Effekt beim Öffnen.
 */
public enum Rarity {
    COMMON(0xB8B8B8),
    RARE(0x4C8DFF),
    EPIC(0xB06AFF),
    LEGENDARY(0xFFB020),
    /** Die höchste Stufe: größer, spektakulärer, mit eigener Truhen-Show. */
    ULTRA(0xFF4DD8);

    private final int color;

    Rarity(int color) {
        this.color = color;
    }

    /** Farbe für Partikel (RGB). */
    public int color() {
        return color;
    }
}
