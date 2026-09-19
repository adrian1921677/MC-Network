package dev.nexus.cosmetics.emote;

import org.bukkit.Material;

/**
 * Alle Emote-Arten. Emoji-Emotes zeigen ein 3D-Emoji über dem Kopf,
 * Posen-Emotes bewegen den Körper des Spielers.
 */
public enum EmoteType {
    // Emoji über dem Kopf
    HEART(null),
    LAUGH(null),
    ANGRY(null),
    WOW(null),
    THUMBS(null),
    SLEEPY(null),
    PARTY(null),

    // Körper-Posen (Material = Symbol im Menü)
    SIT(Material.OAK_STAIRS),
    LIE(Material.RED_BED),
    SPIN(Material.TRIDENT),
    WAVE(Material.FEATHER),
    CLAP(Material.RABBIT_HIDE),
    DANCE(Material.MUSIC_DISC_CAT);

    private final Material icon;

    EmoteType(Material icon) {
        this.icon = icon;
    }

    public boolean isEmoji() {
        return icon == null;
    }

    public Material icon() {
        return icon;
    }
}
