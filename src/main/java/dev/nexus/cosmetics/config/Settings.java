package dev.nexus.cosmetics.config;

import org.bukkit.configuration.file.FileConfiguration;

/** Die Einstellungen aus der config.yml, einmal eingelesen. */
public record Settings(
        String language,
        long emoteCooldownMillis,
        boolean sneakSwapShortcut,
        int hatMinSilenceTicks,
        int hatMaxSilenceTicks) {

    public static Settings from(FileConfiguration config) {
        int minSilence = Math.max(1, config.getInt("talking-hat.min-silence-seconds", 30));
        int maxSilence = Math.max(minSilence + 1, config.getInt("talking-hat.max-silence-seconds", 70));
        return new Settings(
                config.getString("language", "de").toLowerCase(),
                Math.round(config.getDouble("emotes.cooldown-seconds", 1.5) * 1000),
                config.getBoolean("emotes.sneak-swap-shortcut", true),
                minSilence * 20,
                maxSilence * 20);
    }
}
