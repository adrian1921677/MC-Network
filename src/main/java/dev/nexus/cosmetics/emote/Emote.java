package dev.nexus.cosmetics.emote;

import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;

/**
 * Ein Emote.
 *
 * @param free true = jeder Spieler darf es benutzen, false = nur mit Permission (z. B. für Ränge)
 */
public record Emote(String id, Component displayName, EmoteType type, boolean free) {

    public String permission() {
        return "nexuscosmetics.emote." + id;
    }

    /** 3D-Modell des Emojis im Resource Pack (nur für Emoji-Emotes). */
    public NamespacedKey model() {
        return new NamespacedKey(CosmeticRegistry.NAMESPACE, "emoji_" + id);
    }
}
