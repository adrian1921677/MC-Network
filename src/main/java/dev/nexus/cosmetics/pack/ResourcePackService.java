package dev.nexus.cosmetics.pack;

import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Baut beim Start das Resource Pack aus dem Plugin-Jar (Ordner "pack/") und stellt es über einen
 * kleinen eingebauten Webserver bereit. Spieler bekommen es beim Betreten automatisch angeboten.
 * Server-Owner müssen dafür nichts selbst hochladen.
 */
public final class ResourcePackService implements Listener {

    private static final String PACK_FOLDER = "pack/";
    private static final long FIXED_ENTRY_TIME = 1_577_836_800_000L; // 01.01.2020
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("nexuscosmetics".getBytes(StandardCharsets.UTF_8));

    private final JavaPlugin plugin;
    private final File pluginJar;

    private HttpServer httpServer;
    private ResourcePackRequest request;

    public ResourcePackService(JavaPlugin plugin, File pluginJar) {
        this.plugin = plugin;
        this.pluginJar = pluginJar;
    }

    public void start() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("resource-pack");
        if (config == null || !config.getBoolean("enabled", true)) {
            plugin.getLogger().info("Resource Pack ist deaktiviert.");
            return;
        }

        try {
            byte[] pack = buildPack();
            String sha1 = sha1(pack);
            Files.write(new File(plugin.getDataFolder(), "resource-pack.zip").toPath(), pack);

            String url = config.getString("public-url", "");
            if (url.isBlank()) {
                String host = config.getString("host", "localhost");
                int port = config.getInt("port", 8163);
                startHttpServer(port, pack);
                // Der Hash im Link sorgt dafür, dass Spieler nach Updates die neue Version laden
                url = "http://" + host + ":" + port + "/pack.zip?v=" + sha1.substring(0, 8);
            }

            Component prompt = MiniMessage.miniMessage().deserialize(config.getString("prompt", ""));
            request = ResourcePackRequest.resourcePackRequest()
                    .packs(ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url), sha1))
                    .required(config.getBoolean("required", false))
                    .prompt(prompt)
                    .replace(false)
                    .build();

            plugin.getLogger().info("Resource Pack bereit: " + url + " (" + pack.length / 1024 + " KB)");
        } catch (IOException | NoSuchAlgorithmException exception) {
            plugin.getLogger().severe("Resource Pack konnte nicht erstellt werden: " + exception.getMessage());
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (request != null) {
            event.getPlayer().sendResourcePacks(request);
        }
    }

    /** Kopiert alle Dateien aus "pack/" im Plugin-Jar in eine neue Zip-Datei. */
    private byte[] buildPack() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipFile jar = new ZipFile(pluginJar); ZipOutputStream zip = new ZipOutputStream(bytes)) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(PACK_FOLDER)) {
                    continue;
                }
                ZipEntry packEntry = new ZipEntry(entry.getName().substring(PACK_FOLDER.length()));
                // Feste Zeitangabe: Gleicher Inhalt ergibt gleichen Hash, Spieler laden das Pack nur bei Änderungen neu
                packEntry.setTime(FIXED_ENTRY_TIME);
                zip.putNextEntry(packEntry);
                try (InputStream in = jar.getInputStream(entry)) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private void startHttpServer(int port, byte[] pack) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/pack.zip", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, pack.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(pack);
            }
        });
        httpServer.start();
    }

    private static String sha1(byte[] data) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
    }
}
