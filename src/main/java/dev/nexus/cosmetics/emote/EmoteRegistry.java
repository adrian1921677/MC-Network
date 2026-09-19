package dev.nexus.cosmetics.emote;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Liste aller Emotes. */
public final class EmoteRegistry {

    private final Map<String, Emote> emotes = new LinkedHashMap<>();

    public EmoteRegistry() {
        // Emojis (für alle frei)
        register("heart", "Herz", NamedTextColor.RED, EmoteType.HEART, true);
        register("laugh", "Lachen", NamedTextColor.YELLOW, EmoteType.LAUGH, true);
        register("angry", "Wütend", TextColor.color(0xFF5533), EmoteType.ANGRY, true);
        register("wow", "Staunen", NamedTextColor.YELLOW, EmoteType.WOW, true);
        register("thumbs", "Daumen hoch", NamedTextColor.YELLOW, EmoteType.THUMBS, true);
        register("sleepy", "Müde", NamedTextColor.AQUA, EmoteType.SLEEPY, true);
        register("party", "Party", NamedTextColor.LIGHT_PURPLE, EmoteType.PARTY, false);

        // Posen
        register("sit", "Hinsetzen", NamedTextColor.GOLD, EmoteType.SIT, true);
        register("lie", "Hinlegen", NamedTextColor.GOLD, EmoteType.LIE, true);
        register("wave", "Winken", NamedTextColor.GOLD, EmoteType.WAVE, true);
        register("clap", "Klatschen", NamedTextColor.GOLD, EmoteType.CLAP, false);
        register("spin", "Wirbel", NamedTextColor.AQUA, EmoteType.SPIN, false);
        register("dance", "Tanzen", NamedTextColor.LIGHT_PURPLE, EmoteType.DANCE, false);
    }

    private void register(String id, String name, TextColor color, EmoteType type, boolean free) {
        emotes.put(id, new Emote(id, Component.text(name, color), type, free));
    }

    public Emote get(String id) {
        return emotes.get(id);
    }

    public Collection<Emote> all() {
        return emotes.values();
    }
}
