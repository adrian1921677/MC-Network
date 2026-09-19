package dev.nexus.cosmetics.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Hilfen zum Anlegen und Laden der Konfigurationsdateien im Plugin-Ordner. */
public final class ConfigFiles {

    private ConfigFiles() {
    }

    /** Kopiert eine mitgelieferte Datei in den Plugin-Ordner, falls sie dort noch nicht existiert. */
    public static void saveIfMissing(JavaPlugin plugin, String resource, String target) {
        File file = new File(plugin.getDataFolder(), target);
        if (file.exists()) {
            return;
        }
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                plugin.getLogger().warning("Standard-Datei fehlt im Plugin: " + resource);
                return;
            }
            file.getParentFile().mkdirs();
            Files.copy(in, file.toPath());
        } catch (IOException exception) {
            plugin.getLogger().severe("Konnte " + target + " nicht anlegen: " + exception.getMessage());
        }
    }

    /**
     * Lädt cosmetics.yml bzw. emotes.yml. Beim ersten Start wird die Variante in der
     * eingestellten Sprache angelegt (z. B. defaults/cosmetics_de.yml).
     */
    public static YamlConfiguration loadWithLanguageDefault(JavaPlugin plugin, String name, String language) {
        String lang = Messages.LANGUAGES.contains(language) ? language : "en";
        String resource = "defaults/" + name + "_" + lang + ".yml";
        saveIfMissing(plugin, resource, name + ".yml");
        File file = new File(plugin.getDataFolder(), name + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        addNewDefaults(plugin, config, file, resource, name);
        return config;
    }

    /**
     * Ergänzt nach einem Plugin-Update neue Einträge (z. B. neue Cosmetics), die in der Datei des
     * Server-Owners noch fehlen. Vorhandene Einträge und eigene Änderungen bleiben unangetastet.
     * Wer ein Cosmetic nicht will, schaltet es mit "enabled: false" ab, statt es zu löschen.
     */
    private static void addNewDefaults(JavaPlugin plugin, YamlConfiguration config, File file, String resource, String section) {
        InputStream in = plugin.getResource(resource);
        if (in == null) {
            return;
        }
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        ConfigurationSection defaultSection = defaults.getConfigurationSection(section);
        if (defaultSection == null) {
            return;
        }
        List<String> added = new ArrayList<>();
        for (String key : defaultSection.getKeys(false)) {
            String path = section + "." + key;
            if (!config.contains(path)) {
                config.set(path, defaultSection.get(key));
                added.add(key);
            }
        }
        if (added.isEmpty()) {
            return;
        }
        try {
            config.save(file);
            plugin.getLogger().info(added.size() + " neue Einträge in " + file.getName() + " ergänzt: " + String.join(", ", added));
        } catch (IOException exception) {
            plugin.getLogger().warning("Konnte " + file.getName() + " nicht ergänzen: " + exception.getMessage());
        }
    }
}
