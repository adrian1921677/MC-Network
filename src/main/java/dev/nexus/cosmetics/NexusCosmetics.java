package dev.nexus.cosmetics;

import dev.nexus.cosmetics.command.CosmeticsCommand;
import dev.nexus.cosmetics.command.EmoteCommand;
import dev.nexus.cosmetics.emote.Emote;
import dev.nexus.cosmetics.emote.EmoteRegistry;
import dev.nexus.cosmetics.emote.EmoteService;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticProtectionListener;
import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import dev.nexus.cosmetics.menu.CosmeticMenuListener;
import dev.nexus.cosmetics.pack.ResourcePackService;
import dev.nexus.cosmetics.render.FakeCosmeticRenderer;
import dev.nexus.cosmetics.storage.CosmeticStorage;
import dev.nexus.cosmetics.storage.YamlCosmeticStorage;
import dev.nexus.cosmetics.cosmetic.Cosmetic;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Hauptklasse des Plugins. Paper ruft beim Serverstart onEnable() und beim Stoppen onDisable() auf.
 */
public final class NexusCosmetics extends JavaPlugin {

    private CosmeticManager cosmeticManager;
    private FakeCosmeticRenderer renderer;
    private CosmeticStorage storage;
    private EmoteService emoteService;
    private ResourcePackService resourcePackService;

    @Override
    public void onEnable() {
        // Legt plugins/NexusCosmetics/config.yml an, falls sie noch nicht existiert
        saveDefaultConfig();

        CosmeticRegistry registry = new CosmeticRegistry();
        renderer = new FakeCosmeticRenderer(this);
        renderer.start();
        storage = new YamlCosmeticStorage(getDataFolder(), getLogger());
        cosmeticManager = new CosmeticManager(this, registry, renderer, storage);

        EmoteRegistry emotes = new EmoteRegistry();
        emoteService = new EmoteService(this, emotes, renderer);
        emoteService.start();

        resourcePackService = new ResourcePackService(this, getFile());
        resourcePackService.start();

        PluginManager pluginManager = getServer().getPluginManager();
        registerPermissions(pluginManager, registry, emotes);
        pluginManager.registerEvents(new CosmeticProtectionListener(this, cosmeticManager), this);
        pluginManager.registerEvents(new CosmeticMenuListener(), this);
        pluginManager.registerEvents(renderer, this);
        pluginManager.registerEvents(emoteService, this);
        pluginManager.registerEvents(resourcePackService, this);

        registerCommand("cosmetics", "Öffnet das Cosmetics-Menü", List.of("cosmetic"),
                new CosmeticsCommand(cosmeticManager));
        registerCommand("emote", "Öffnet das Emote-Menü oder spielt ein Emote ab", List.of("emotes"),
                new EmoteCommand(emoteService));

        // Falls das Plugin im laufenden Betrieb neu geladen wird: Cosmetics der Online-Spieler laden
        getServer().getOnlinePlayers().forEach(cosmeticManager::handleJoin);

        getLogger().info(registry.all().size() + " Cosmetics geladen.");
    }

    /**
     * Legt für jedes Cosmetic und Emote ein eigenes Recht an und hängt es an
     * nexuscosmetics.cosmetic.* bzw. nexuscosmetics.emote.*
     */
    private void registerPermissions(PluginManager pluginManager, CosmeticRegistry registry, EmoteRegistry emotes) {
        Permission allEmotes = pluginManager.getPermission("nexuscosmetics.emote.*");
        for (Emote emote : emotes.all()) {
            if (pluginManager.getPermission(emote.permission()) == null) {
                // Freie Emotes darf jeder benutzen, die anderen nur mit Recht (z. B. für Ränge)
                pluginManager.addPermission(new Permission(emote.permission(),
                        emote.free() ? PermissionDefault.TRUE : PermissionDefault.OP));
            }
            if (allEmotes != null) {
                allEmotes.getChildren().put(emote.permission(), true);
            }
        }
        if (allEmotes != null) {
            allEmotes.recalculatePermissibles();
        }

        Permission all = pluginManager.getPermission("nexuscosmetics.cosmetic.*");
        for (Cosmetic cosmetic : registry.all()) {
            if (pluginManager.getPermission(cosmetic.permission()) == null) {
                pluginManager.addPermission(new Permission(cosmetic.permission(), PermissionDefault.OP));
            }
            if (all != null) {
                all.getChildren().put(cosmetic.permission(), true);
            }
        }
        if (all != null) {
            all.recalculatePermissibles();
        }
    }

    @Override
    public void onDisable() {
        if (emoteService != null) {
            emoteService.stop();
        }
        if (cosmeticManager != null) {
            cosmeticManager.shutdown();
        }
        if (renderer != null) {
            renderer.stop();
        }
        if (storage != null) {
            storage.close();
        }
        if (resourcePackService != null) {
            resourcePackService.stop();
        }
    }
}
