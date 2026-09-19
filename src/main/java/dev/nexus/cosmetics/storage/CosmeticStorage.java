package dev.nexus.cosmetics.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Speichert die Profile der Spieler (getragene Cosmetics, Besitz, Schlüssel).
 *
 * Alle Methoden arbeiten im Hintergrund, damit der Server nie auf Festplatte oder Datenbank warten muss.
 * Weitere Speicherarten (z. B. MySQL für Netzwerke) implementieren einfach dieses Interface.
 */
public interface CosmeticStorage {

    CompletableFuture<PlayerProfile> load(UUID player);

    CompletableFuture<Void> save(UUID player, PlayerProfile profile);

    /** Lädt, ändert und speichert ein Profil in einem Schritt (z. B. Schlüssel für Offline-Spieler). */
    CompletableFuture<Void> modify(UUID player, Consumer<PlayerProfile> change);

    /** Wartet, bis alle offenen Speichervorgänge fertig sind (beim Server-Stopp). */
    void close();
}
