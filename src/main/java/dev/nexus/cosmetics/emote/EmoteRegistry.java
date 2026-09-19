package dev.nexus.cosmetics.emote;

import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Liste aller Emotes, gelesen aus emotes.yml. */
public final class EmoteRegistry {

    private final Map<String, Emote> emotes = new LinkedHashMap<>();

    public void load(YamlConfiguration config, Logger logger) {
        emotes.clear();
        ConfigurationSection section = config.getConfigurationSection("emotes");
        if (section == null) {
            logger.warning("emotes.yml enthält keinen Abschnitt 'emotes:'.");
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null || !entry.getBoolean("enabled", true)) {
                continue;
            }
            try {
                EmoteType type = EmoteType.valueOf(entry.getString("type", "").toUpperCase(Locale.ROOT));
                NamespacedKey model = NamespacedKey.fromString(
                        entry.getString("model", CosmeticRegistry.NAMESPACE + ":emoji_" + id));
                emotes.put(id, new Emote(id,
                        MiniMessage.miniMessage().deserialize(entry.getString("name", id)),
                        type,
                        entry.getBoolean("free", true),
                        model));
            } catch (IllegalArgumentException exception) {
                logger.warning("Emote '" + id + "' wird übersprungen: unbekannter Typ '" + entry.getString("type") + "'.");
            }
        }
    }

    public Emote get(String id) {
        return emotes.get(id);
    }

    public Collection<Emote> all() {
        return emotes.values();
    }
}
