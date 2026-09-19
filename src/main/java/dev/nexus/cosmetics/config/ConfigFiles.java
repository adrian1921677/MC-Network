package dev.nexus.cosmetics.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

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
        saveIfMissing(plugin, "defaults/" + name + "_" + lang + ".yml", name + ".yml");
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name + ".yml"));
    }
}
