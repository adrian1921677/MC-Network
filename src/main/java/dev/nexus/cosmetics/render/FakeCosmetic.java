package dev.nexus.cosmetics.render;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Ein Cosmetic, das nur aus Netzwerk-Paketen besteht (Capes, Haustiere ...).
 * Der FakeCosmeticRenderer entscheidet, wer es sehen soll, und ruft jeden Tick tick() auf.
 */
public interface FakeCosmetic {

    /** Schickt das Cosmetic an einen Spieler. */
    void show(Player viewer);

    /** Entfernt das Cosmetic bei einem Spieler. */
    void hide(Player viewer);

    /** Vergisst einen Zuschauer, ohne Pakete zu senden (z. B. weil er offline ist). */
    void forget(UUID viewer);

    /** Wird jeden Server-Tick aufgerufen (20x pro Sekunde). */
    void tick(int serverTick);

    /** Entfernt das Cosmetic bei allen Zuschauern. */
    void destroy();

    /** true = fertig abgespielt (z. B. ein Emote), der Renderer entfernt es dann automatisch. */
    default boolean finished() {
        return false;
    }

    /** Entity-IDs, die für diesen Zuschauer als Passagiere auf dem Träger sitzen sollen. */
    default int[] passengerIds(Player viewer) {
        return new int[0];
    }

    /** Neu anzeigen, z. B. nach Respawn oder Weltwechsel. */
    default void respawnFor(Player viewer) {
        hide(viewer);
        show(viewer);
    }
}
