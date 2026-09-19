package dev.nexus.cosmetics.storage;

import dev.nexus.cosmetics.cosmetic.CosmeticSlot;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Alles, was wir uns pro Spieler merken:
 * - welche Cosmetics er trägt,
 * - was er besitzt (aus Truhen gewonnen oder geschenkt bekommen),
 * - wie viele Truhen-Schlüssel er hat.
 *
 * Besitz wird als Text gespeichert: "crown" für Cosmetics, "emote:dance" für Emotes.
 */
public final class PlayerProfile {

    private final Map<CosmeticSlot, String> equipped = new EnumMap<>(CosmeticSlot.class);
    private final Set<String> owned = new LinkedHashSet<>();
    private final Map<String, Integer> keys = new HashMap<>();

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

    /** Unabhängige Kopie, damit das Speichern im Hintergrund nicht mit Änderungen kollidiert. */
    public PlayerProfile copy() {
        PlayerProfile copy = new PlayerProfile();
        copy.equipped.putAll(equipped);
        copy.owned.addAll(owned);
        copy.keys.putAll(keys);
        return copy;
    }
}
