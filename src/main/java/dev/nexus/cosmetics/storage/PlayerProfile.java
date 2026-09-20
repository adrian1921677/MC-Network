package dev.nexus.cosmetics.storage;

import dev.nexus.cosmetics.cosmetic.CosmeticSlot;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Alles, was wir uns pro Spieler merken:
 * - welche Cosmetics er trägt,
 * - was er besitzt (aus Truhen gewonnen oder geschenkt bekommen),
 * - wie viele Truhen-Schlüssel er hat,
 * - welche Cosmetics er als Favorit markiert hat,
 * - welche Outfits (gespeicherte Kombinationen) er angelegt hat.
 *
 * Besitz wird als Text gespeichert: "crown" für Cosmetics, "emote:dance" für Emotes.
 */
public final class PlayerProfile {

    /** Mehr Outfits passen nicht ins Menü und niemand braucht so viele. */
    public static final int MAX_OUTFITS = 9;

    private final Map<CosmeticSlot, String> equipped = new EnumMap<>(CosmeticSlot.class);
    private final Set<String> owned = new LinkedHashSet<>();
    private final Map<String, Integer> keys = new HashMap<>();
    private final Set<String> favorites = new LinkedHashSet<>();
    private final Map<String, Map<CosmeticSlot, String>> outfits = new LinkedHashMap<>();

    public Map<CosmeticSlot, String> equipped() {
        return equipped;
    }

    public Set<String> owned() {
        return owned;
    }

    public int keys(String crate) {
        return keys.getOrDefault(crate, 0);
    }

    public Map<String, Integer> allKeys() {
        return keys;
    }

    public void addKeys(String crate, int amount) {
        int total = Math.max(0, keys(crate) + amount);
        if (total == 0) {
            keys.remove(crate);
        } else {
            keys.put(crate, total);
        }
    }

    // ------------------------------------------------------------------ Favoriten

    public Set<String> favorites() {
        return favorites;
    }

    public boolean isFavorite(String cosmeticId) {
        return favorites.contains(cosmeticId);
    }

    /** Schaltet den Favoriten um und sagt, ob er jetzt gesetzt ist. */
    public boolean toggleFavorite(String cosmeticId) {
        if (!favorites.remove(cosmeticId)) {
            favorites.add(cosmeticId);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ Outfits

    public Map<String, Map<CosmeticSlot, String>> outfits() {
        return outfits;
    }

    /** Legt ein Outfit an oder überschreibt eines. Gibt false zurück, wenn kein Platz mehr frei ist. */
    public boolean saveOutfit(String name, Map<CosmeticSlot, String> pieces) {
        if (!outfits.containsKey(name) && outfits.size() >= MAX_OUTFITS) {
            return false;
        }
        outfits.put(name, new EnumMap<>(pieces));
        return true;
    }

    /** Unabhängige Kopie, damit das Speichern im Hintergrund nicht mit Änderungen kollidiert. */
    public PlayerProfile copy() {
        PlayerProfile copy = new PlayerProfile();
        copy.equipped.putAll(equipped);
        copy.owned.addAll(owned);
        copy.keys.putAll(keys);
        copy.favorites.addAll(favorites);
        outfits.forEach((name, pieces) -> copy.outfits.put(name, new EnumMap<>(pieces)));
        return copy;
    }
}
