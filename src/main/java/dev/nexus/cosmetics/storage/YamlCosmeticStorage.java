package dev.nexus.cosmetics.storage;

import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Speichert pro Spieler eine kleine Datei: plugins/NexusCosmetics/playerdata/<uuid>.yml
 *
 * Alle Zugriffe laufen nacheinander in einem eigenen Hintergrund-Thread. So kann ein Laden nie
 * einen gerade laufenden Speichervorgang überholen.
 */
public final class YamlCosmeticStorage implements CosmeticStorage {

    private final File folder;
    private final Logger logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, "NexusCosmetics-Storage"));

    public YamlCosmeticStorage(File dataFolder, Logger logger) {
        this.folder = new File(dataFolder, "playerdata");
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Map<CosmeticSlot, String>> load(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            Map<CosmeticSlot, String> equipped = new EnumMap<>(CosmeticSlot.class);
            File file = file(player);
            if (!file.exists()) {
                return equipped;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            for (CosmeticSlot slot : CosmeticSlot.values()) {
                String id = yaml.getString("equipped." + slot.name().toLowerCase());
                if (id != null) {
                    equipped.put(slot, id);
                }
            }
            return equipped;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(UUID player, Map<CosmeticSlot, String> equipped) {
        Map<CosmeticSlot, String> snapshot = Map.copyOf(equipped);
        return CompletableFuture.runAsync(() -> {
            File file = file(player);
            if (snapshot.isEmpty()) {
                file.delete();
                return;
            }
            YamlConfiguration yaml = new YamlConfiguration();
            snapshot.forEach((slot, id) -> yaml.set("equipped." + slot.name().toLowerCase(), id));
            try {
                folder.mkdirs();
                yaml.save(file);
            } catch (IOException exception) {
                logger.warning("Cosmetics von " + player + " konnten nicht gespeichert werden: " + exception.getMessage());
            }
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Nicht alle Cosmetics konnten rechtzeitig gespeichert werden.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private File file(UUID player) {
        return new File(folder, player + ".yml");
    }
}
