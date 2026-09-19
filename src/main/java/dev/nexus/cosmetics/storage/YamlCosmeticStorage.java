package dev.nexus.cosmetics.storage;

import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
    public CompletableFuture<PlayerProfile> load(UUID player) {
        return CompletableFuture.supplyAsync(() -> read(player), executor);
    }

    @Override
    public CompletableFuture<Void> save(UUID player, PlayerProfile profile) {
        PlayerProfile snapshot = profile.copy();
        return CompletableFuture.runAsync(() -> write(player, snapshot), executor);
    }

    @Override
    public CompletableFuture<Void> modify(UUID player, Consumer<PlayerProfile> change) {
        return CompletableFuture.runAsync(() -> {
            PlayerProfile profile = read(player);
            change.accept(profile);
            write(player, profile);
        }, executor);
    }

    private PlayerProfile read(UUID player) {
        PlayerProfile profile = new PlayerProfile();
        File file = file(player);
        if (!file.exists()) {
            return profile;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            String id = yaml.getString("equipped." + slot.name().toLowerCase(Locale.ROOT));
            if (id != null) {
                profile.equipped().put(slot, id);
            }
        }
        profile.owned().addAll(yaml.getStringList("owned"));
        ConfigurationSection keys = yaml.getConfigurationSection("keys");
        if (keys != null) {
            for (String crate : keys.getKeys(false)) {
                profile.addKeys(crate, keys.getInt(crate));
            }
        }
        return profile;
    }

    private void write(UUID player, PlayerProfile profile) {
        File file = file(player);
        if (profile.equipped().isEmpty() && profile.owned().isEmpty() && profile.allKeys().isEmpty()) {
            file.delete();
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        profile.equipped().forEach((slot, id) -> yaml.set("equipped." + slot.name().toLowerCase(Locale.ROOT), id));
        yaml.set("owned", new ArrayList<>(profile.owned()));
        profile.allKeys().forEach((crate, amount) -> yaml.set("keys." + crate, amount));
        try {
            folder.mkdirs();
            yaml.save(file);
        } catch (IOException exception) {
            logger.warning("Profil von " + player + " konnte nicht gespeichert werden: " + exception.getMessage());
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Nicht alle Profile konnten rechtzeitig gespeichert werden.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private File file(UUID player) {
        return new File(folder, player + ".yml");
    }
}
