package dev.nexus.cosmetics.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Alle Texte aus lang/messages_<sprache>.yml.
 * Fehlt ein Text in der Datei (z. B. nach einem Plugin-Update), wird der mitgelieferte Standard benutzt.
 */
public final class Messages {

    public static final List<String> LANGUAGES = List.of("de", "en");

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages = new YamlConfiguration();

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(String language) {
        for (String lang : LANGUAGES) {
            ConfigFiles.saveIfMissing(plugin, "lang/messages_" + lang + ".yml", "lang/messages_" + lang + ".yml");
        }
        if (!LANGUAGES.contains(language)) {
            plugin.getLogger().warning("Unbekannte Sprache '" + language + "', benutze 'en'.");
            language = "en";
        }
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "lang/messages_" + language + ".yml"));

        // Mitgelieferte Texte als Rückfall für fehlende Einträge
        InputStream defaults = plugin.getResource("lang/messages_" + language + ".yml");
        if (defaults != null) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
    }

    /** Ein Text als Component, z. B. get("cosmetics.locked"). */
    public Component get(String key, TagResolver... placeholders) {
        return miniMessage.deserialize(messages.getString(key, key), placeholders);
    }

    /** Ein Text mit dem Plugin-Präfix davor (für Chat-Nachrichten). */
    public Component prefixed(String key, TagResolver... placeholders) {
        return get("prefix").append(get(key, placeholders));
    }

    /** Ein Text für Item-Namen und Beschreibungen (ohne automatische Kursivschrift). */
    public Component item(String key, TagResolver... placeholders) {
        return get(key, placeholders).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public List<Component> itemList(String key) {
        return messages.getStringList(key).stream()
                .map(line -> miniMessage.deserialize(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .toList();
    }

    public List<String> rawList(String key) {
        return messages.getStringList(key);
    }

    public String raw(String key) {
        return messages.getString(key, key);
    }

    /** Alle Unter-Einträge eines Abschnitts, z. B. die Team-IDs unter talking-hat.teams. */
    public List<String> keys(String section) {
        return messages.isConfigurationSection(section)
                ? List.copyOf(messages.getConfigurationSection(section).getKeys(false))
                : List.of();
    }

    public Component parse(String miniMessageText, TagResolver... placeholders) {
        return miniMessage.deserialize(miniMessageText, placeholders);
    }
}
