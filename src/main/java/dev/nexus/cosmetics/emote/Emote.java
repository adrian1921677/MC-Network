package dev.nexus.cosmetics.emote;

import dev.nexus.cosmetics.cosmetic.Rarity;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;

/**
 * Ein Emote (aus emotes.yml).
 *
 * @param free  true = jeder Spieler darf es benutzen, false = nur mit Permission (z. B. für Ränge)
 * @param model 3D-Modell des Emojis im Resource Pack (nur für Emoji-Emotes)
 * @param rarity Seltenheit (für Truhen)
 */
public record Emote(String id, Component displayName, EmoteType type, boolean free, NamespacedKey model, Rarity rarity) {

    public String permission() {
        return "nexuscosmetics.emote." + id;
    }

    /** So steht das Emote in der Besitz-Liste eines Spielers. */
    public String ownershipKey() {
        return "emote:" + id;
    }
}
