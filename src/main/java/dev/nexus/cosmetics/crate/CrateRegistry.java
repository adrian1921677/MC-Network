package dev.nexus.cosmetics.crate;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import dev.nexus.cosmetics.cosmetic.Rarity;
import dev.nexus.cosmetics.emote.Emote;
import dev.nexus.cosmetics.emote.EmoteRegistry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Liste aller Truhen, gelesen aus crates.yml. */
public final class CrateRegistry {

    private static final Map<Rarity, Integer> DEFAULT_CHANCES = Map.of(
            Rarity.COMMON, 60, Rarity.RARE, 25, Rarity.EPIC, 12, Rarity.LEGENDARY, 3, Rarity.ULTRA, 1);

    private final Map<String, Crate> crates = new LinkedHashMap<>();

    public void load(YamlConfiguration config, CosmeticRegistry cosmetics, EmoteRegistry emotes, Logger logger) {
        crates.clear();
        ConfigurationSection section = config.getConfigurationSection("crates");
        if (section == null) {
            return;
        }
        NamespacedKey emoteIcon = new NamespacedKey(CosmeticRegistry.NAMESPACE, "emoji_party");

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null || !entry.getBoolean("enabled", true)) {
                continue;
            }

            Map<Rarity, Integer> chances = new EnumMap<>(Rarity.class);
            for (Rarity rarity : Rarity.values()) {
                chances.put(rarity, Math.max(0, entry.getInt("chances." + rarity.name(), DEFAULT_CHANCES.get(rarity))));
            }

            List<CrateReward> rewards = new ArrayList<>();
            if (entry.isList("rewards")) {
                // Feste Liste: "crown" für Cosmetics, "emote:dance" für Emotes
                for (String reward : entry.getStringList("rewards")) {
                    String key = reward.toLowerCase(Locale.ROOT);
                    if (key.startsWith("emote:")) {
                        Emote emote = emotes.get(key.substring(6));
                        if (emote != null) {
                            rewards.add(CrateReward.of(emote, emoteIcon));
                            continue;
                        }
                    } else {
                        Cosmetic cosmetic = cosmetics.get(key);
                        if (cosmetic != null) {
                            rewards.add(CrateReward.of(cosmetic));
                            continue;
                        }
                    }
                    logger.warning("Truhe '" + id + "': unbekannter Gewinn '" + reward + "'.");
                }
            } else {
                // "all": alles, was nicht ohnehin für jeden frei ist
                cosmetics.all().stream().filter(cosmetic -> !cosmetic.unlockedByDefault())
                        .forEach(cosmetic -> rewards.add(CrateReward.of(cosmetic)));
                emotes.all().stream().filter(emote -> !emote.free())
                        .forEach(emote -> rewards.add(CrateReward.of(emote, emoteIcon)));
            }

            crates.put(id, new Crate(id, MiniMessage.miniMessage().deserialize(entry.getString("name", id)), chances, rewards));
        }
    }

    public Crate get(String id) {
        return crates.get(id);
    }

    public Collection<Crate> all() {
        return crates.values();
    }
}
