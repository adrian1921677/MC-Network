package dev.nexus.cosmetics;

import dev.nexus.cosmetics.cape.CapeRenderer;
import dev.nexus.cosmetics.command.CosmeticsCommand;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticProtectionListener;
import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import dev.nexus.cosmetics.menu.CosmeticMenuListener;
import dev.nexus.cosmetics.pack.ResourcePackService;
import dev.nexus.cosmetics.storage.CosmeticStorage;
import dev.nexus.cosmetics.storage.YamlCosmeticStorage;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Hauptklasse des Plugins. Paper ruft beim Serverstart onEnable() und beim Stoppen onDisable() auf.
 */
public final class NexusCosmetics extends JavaPlugin {

    private CosmeticManager cosmeticManager;
    private CapeRenderer capeRenderer;
    private CosmeticStorage storage;
    private ResourcePackService resourcePackService;

    @Override
    public void onEnable() {
        // Legt plugins/NexusCosmetics/config.yml an, falls sie noch nicht existiert
        saveDefaultConfig();

        CosmeticRegistry registry = new CosmeticRegistry();
        capeRenderer = new CapeRenderer(this);
        capeRenderer.start();
        storage = new YamlCosmeticStorage(getDataFolder(), getLogger());
        cosmeticManager = new CosmeticManager(this, registry, capeRenderer, storage);

        resourcePackService = new ResourcePackService(this, getFile());
        resourcePackService.start();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CosmeticProtectionListener(this, cosmeticManager), this);
        pluginManager.registerEvents(new CosmeticMenuListener(), this);
        pluginManager.registerEvents(capeRenderer, this);
        pluginManager.registerEvents(resourcePackService, this);

        registerCommand("cosmetics", "Öffnet das Cosmetics-Menü", List.of("cosmetic"),
                new CosmeticsCommand(cosmeticManager));

        // Falls das Plugin im laufenden Betrieb neu geladen wird: Cosmetics der Online-Spieler laden
        getServer().getOnlinePlayers().forEach(cosmeticManager::handleJoin);

        getLogger().info(registry.all().size() + " Cosmetics geladen.");
    }

    @Override
    public void onDisable() {
        if (cosmeticManager != null) {
            cosmeticManager.shutdown();
        }
        if (capeRenderer != null) {
            capeRenderer.stop();
        }
        if (storage != null) {
            storage.close();
        }
        if (resourcePackService != null) {
            resourcePackService.stop();
        }
    }
}
