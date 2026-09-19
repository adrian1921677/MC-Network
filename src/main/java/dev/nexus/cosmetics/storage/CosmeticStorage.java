package dev.nexus.cosmetics.storage;

import dev.nexus.cosmetics.cosmetic.CosmeticSlot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Speichert, welche Cosmetics ein Spieler trägt (pro Slot die Cosmetic-ID).
 *
 * Alle Methoden arbeiten im Hintergrund, damit der Server nie auf Festplatte oder Datenbank warten muss.
 * Weitere Speicherarten (z. B. MySQL für Netzwerke) implementieren einfach dieses Interface.
 */
public interface CosmeticStorage {

    CompletableFuture<Map<CosmeticSlot, String>> load(UUID player);

    CompletableFuture<Void> save(UUID player, Map<CosmeticSlot, String> equipped);

    /** Wartet, bis alle offenen Speichervorgänge fertig sind (beim Server-Stopp). */
    void close();
}
