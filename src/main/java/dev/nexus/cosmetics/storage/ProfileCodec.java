package dev.nexus.cosmetics.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nexus.cosmetics.cosmetic.CosmeticSlot;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Übersetzt ein {@link PlayerProfile} nach JSON und zurück.
 *
 * In der Datenbank steht pro Spieler eine Zeile mit drei Textspalten. JSON deshalb, weil Cosmetic-
 * und Truhen-Namen frei vergeben werden und in einem selbstgebauten Trennzeichen-Format kaputtgehen
 * könnten.
 */
final class ProfileCodec {

    private static final Gson GSON = new Gson();

    private ProfileCodec() {
    }

    static String equippedToJson(PlayerProfile profile) {
        JsonObject json = new JsonObject();
        profile.equipped().forEach((slot, id) -> json.addProperty(slot.name(), id));
        return GSON.toJson(json);
    }

    static String ownedToJson(PlayerProfile profile) {
        JsonArray json = new JsonArray();
        profile.owned().forEach(json::add);
        return GSON.toJson(json);
    }

    static String keysToJson(PlayerProfile profile) {
        JsonObject json = new JsonObject();
        profile.allKeys().forEach(json::addProperty);
        return GSON.toJson(json);
    }

    static String favoritesToJson(PlayerProfile profile) {
        JsonArray json = new JsonArray();
        profile.favorites().forEach(json::add);
        return GSON.toJson(json);
    }

    static String outfitsToJson(PlayerProfile profile) {
        JsonObject json = new JsonObject();
        profile.outfits().forEach((name, pieces) -> {
            JsonObject entry = new JsonObject();
            pieces.forEach((slot, id) -> entry.addProperty(slot.name(), id));
            json.add(name, entry);
        });
        return GSON.toJson(json);
    }

    /**
     * Baut das Profil aus den Spalten. Unbekannte oder kaputte Einträge werden still
     * übersprungen — ein beschädigtes Feld darf niemals den Login blockieren.
     */
    static PlayerProfile fromJson(String equipped, String owned, String keys, String favorites, String outfits) {
        PlayerProfile profile = new PlayerProfile();
        JsonObject equippedJson = object(equipped);
        if (equippedJson != null) {
            for (Map.Entry<String, JsonElement> entry : equippedJson.entrySet()) {
                CosmeticSlot slot = slot(entry.getKey());
                if (slot != null && entry.getValue().isJsonPrimitive()) {
                    profile.equipped().put(slot, entry.getValue().getAsString());
                }
            }
        }
        JsonElement ownedJson = parse(owned);
        if (ownedJson != null && ownedJson.isJsonArray()) {
            for (JsonElement element : ownedJson.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    profile.owned().add(element.getAsString());
                }
            }
        }
        JsonObject keysJson = object(keys);
        if (keysJson != null) {
            for (Map.Entry<String, JsonElement> entry : keysJson.entrySet()) {
                try {
                    profile.addKeys(entry.getKey(), entry.getValue().getAsInt());
                } catch (RuntimeException ignored) {
                    // kein gültiger Zahlenwert: Eintrag überspringen
                }
            }
        }
        JsonElement favoritesJson = parse(favorites);
        if (favoritesJson != null && favoritesJson.isJsonArray()) {
            for (JsonElement element : favoritesJson.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    profile.favorites().add(element.getAsString());
                }
            }
        }
        JsonObject outfitsJson = object(outfits);
        if (outfitsJson != null) {
            for (Map.Entry<String, JsonElement> entry : outfitsJson.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                Map<CosmeticSlot, String> pieces = new EnumMap<>(CosmeticSlot.class);
                for (Map.Entry<String, JsonElement> piece : entry.getValue().getAsJsonObject().entrySet()) {
                    CosmeticSlot slot = slot(piece.getKey());
                    if (slot != null && piece.getValue().isJsonPrimitive()) {
                        pieces.put(slot, piece.getValue().getAsString());
                    }
                }
                profile.outfits().put(entry.getKey(), pieces);
            }
        }
        return profile;
    }

    private static CosmeticSlot slot(String name) {
        try {
            return CosmeticSlot.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static JsonObject object(String raw) {
        JsonElement element = parse(raw);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonElement parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return JsonParser.parseString(raw);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
