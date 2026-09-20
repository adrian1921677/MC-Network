package dev.nexus.cosmetics.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Der Abschnitt "storage" aus der config.yml.
 *
 * Für einen einzelnen Server reicht "yaml". Netzwerke stellen auf "mysql" um, damit das Profil
 * dem Spieler über alle Server folgt.
 */
public record StorageSettings(
        String type,
        String host,
        int port,
        String database,
        String user,
        String password,
        String tablePrefix,
        String serverName,
        int poolSize,
        int syncIntervalSeconds,
        Map<String, String> properties) {

    /** Voreinstellungen, die fast jeder MySQL-Server braucht. Der Owner kann sie überschreiben. */
    private static final Map<String, String> DEFAULT_PROPERTIES = Map.of(
            "useSSL", "false",
            "allowPublicKeyRetrieval", "true",
            "characterEncoding", "utf8");

    public static StorageSettings from(FileConfiguration config) {
        Map<String, String> properties = new LinkedHashMap<>(DEFAULT_PROPERTIES);
        ConfigurationSection custom = config.getConfigurationSection("storage.mysql.properties");
        if (custom != null) {
            for (String key : custom.getKeys(false)) {
                properties.put(key, String.valueOf(custom.get(key)));
            }
        }
        return new StorageSettings(
                config.getString("storage.type", "yaml").toLowerCase(Locale.ROOT).trim(),
                config.getString("storage.mysql.host", "127.0.0.1"),
                config.getInt("storage.mysql.port", 3306),
                config.getString("storage.mysql.database", "nexus"),
                config.getString("storage.mysql.user", "root"),
                config.getString("storage.mysql.password", ""),
                sanitize(config.getString("storage.mysql.table-prefix", "nexus_"), "nexus_"),
                config.getString("storage.mysql.server-name", "").trim(),
                Math.clamp(config.getInt("storage.mysql.pool-size", 6), 1, 32),
                Math.max(0, config.getInt("storage.mysql.sync-interval-seconds", 5)),
                Map.copyOf(properties));
    }

    public boolean mysql() {
        return "mysql".equals(type) || "mariadb".equals(type);
    }

    public String jdbcUrl() {
        StringBuilder url = new StringBuilder("jdbc:mysql://").append(host).append(':').append(port)
                .append('/').append(database);
        char separator = '?';
        for (Map.Entry<String, String> property : properties.entrySet()) {
            url.append(separator).append(property.getKey()).append('=').append(property.getValue());
            separator = '&';
        }
        return url.toString();
    }

    /**
     * Tabellennamen landen direkt im SQL und dürfen deshalb nur aus harmlosen Zeichen bestehen.
     * Alles andere wird verworfen.
     */
    private static String sanitize(String value, String fallback) {
        if (value == null || !value.matches("[A-Za-z0-9_]*")) {
            return fallback;
        }
        return value;
    }
}
