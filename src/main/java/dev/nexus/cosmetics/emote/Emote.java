package dev.nexus.cosmetics.emote;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;

/**
 * Ein Emote (aus emotes.yml).
 *
 * @param free  true = jeder Spieler darf es benutzen, false = nur mit Permission (z. B. für Ränge)
 * @param model 3D-Modell des Emojis im Resource Pack (nur für Emoji-Emotes)
 */
public record Emote(String id, Component displayName, EmoteType type, boolean free, NamespacedKey model) {

    public String permission() {
        return "nexuscosmetics.emote." + id;
    }
}
