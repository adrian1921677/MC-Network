package dev.nexus.cosmetics.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Speichert die Profile der Spieler (getragene Cosmetics, Besitz, Schlüssel).
 *
 * Alle Methoden arbeiten im Hintergrund, damit der Server nie auf Festplatte oder Datenbank warten muss.
 * Für einzelne Server genügt {@link YamlCosmeticStorage}, für Netzwerke gibt es {@link MySqlCosmeticStorage}.
 */
public interface CosmeticStorage {

    CompletableFuture<PlayerProfile> load(UUID player);

    CompletableFuture<Void> save(UUID player, PlayerProfile profile);

    /** Lädt, ändert und speichert ein Profil in einem Schritt (z. B. Schlüssel für Offline-Spieler). */
    CompletableFuture<Void> modify(UUID player, Consumer<PlayerProfile> change);

    /** Wartet, bis alle offenen Speichervorgänge fertig sind (beim Server-Stopp). */
    void close();

    /**
     * Der Spieler hat diesen Server verlassen.
     *
     * Im Netzwerk gibt der Speicher das Profil damit wieder frei, sodass der nächste Server es
     * ohne Wartezeit übernehmen kann. Bei einem einzelnen Server passiert nichts.
     */
    default void release(UUID player) {
    }

    /**
     * Meldet Profile, die ein anderer Server im Netzwerk verändert hat (z. B. ein Shop hat
     * Schlüssel vergeben), damit sie hier neu geladen werden.
     *
     * Der Aufruf kommt aus einem Hintergrund-Thread.
     */
    default void onExternalChange(Consumer<UUID> listener) {
    }

    /** Kurze Beschreibung für die Konsole, z. B. "MySQL (nexus@127.0.0.1:3306, Server 'lobby-1')". */
    default String describe() {
        return getClass().getSimpleName();
    }
}
