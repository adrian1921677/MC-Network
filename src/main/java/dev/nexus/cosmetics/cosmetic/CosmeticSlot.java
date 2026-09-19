package dev.nexus.cosmetics.cosmetic;

/** Wo ein Cosmetic am Spieler sitzt. Pro Slot kann ein Spieler ein Cosmetic gleichzeitig tragen. */
public enum CosmeticSlot {
    /** Hüte, Kronen ... (im Helm-Slot) */
    HEAD,
    /** Capes (als Paket-Entity hinter dem Spieler) */
    BACK,
    /** Haustiere, die neben dem Spieler schweben (als Paket-Entity) */
    PET
}
