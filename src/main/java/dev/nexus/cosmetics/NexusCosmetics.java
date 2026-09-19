package dev.nexus.cosmetics;

import dev.nexus.cosmetics.command.CosmeticsCommand;
import dev.nexus.cosmetics.command.EmoteCommand;
import dev.nexus.cosmetics.config.ConfigFiles;
import dev.nexus.cosmetics.config.Messages;
import dev.nexus.cosmetics.config.Settings;
import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticProtectionListener;
import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import dev.nexus.cosmetics.emote.Emote;
import dev.nexus.cosmetics.emote.EmoteRegistry;
import dev.nexus.cosmetics.emote.EmoteService;
import dev.nexus.cosmetics.menu.CosmeticMenuListener;
import dev.nexus.cosmetics.pack.ResourcePackService;
import dev.nexus.cosmetics.render.FakeCosmeticRenderer;
import dev.nexus.cosmetics.storage.CosmeticStorage;
import dev.nexus.cosmetics.storage.YamlCosmeticStorage;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Hauptklasse des Plugins. Paper ruft beim Serverstart onEnable() und beim Stoppen onDisable() auf.
 */
public final class NexusCosmetics extends JavaPlugin {

    private final Messages messages = new Messages(this);
    private final CosmeticRegistry cosmeticRegistry = new CosmeticRegistry();
    private final EmoteRegistry emoteRegistry = new EmoteRegistry();
    private Settings settings;

    private CosmeticManager cosmeticManager;
    private FakeCosmeticRenderer renderer;
    private CosmeticStorage storage;
    private EmoteService emoteService;
    private ResourcePackService resourcePackService;

    @Override
    public void onEnable() {
        loadConfiguration();

        renderer = new FakeCosmeticRenderer(this);
        renderer.start();
        storage = new YamlCosmeticStorage(getDataFolder(), getLogger());
        cosmeticManager = new CosmeticManager(this, cosmeticRegistry, renderer, storage);
        emoteService = new EmoteService(this, emoteRegistry, renderer);
        emoteService.start();

        resourcePackService = new ResourcePackService(this, getFile());
        resourcePackService.start();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CosmeticProtectionListener(this, cosmeticManager, messages), this);
        pluginManager.registerEvents(new CosmeticMenuListener(), this);
        pluginManager.registerEvents(renderer, this);
        pluginManager.registerEvents(emoteService, this);
        pluginManager.registerEvents(resourcePackService, this);

        registerCommand("cosmetics", "Öffnet das Cosmetics-Menü", List.of("cosmetic"), new CosmeticsCommand(this));
        registerCommand("emote", "Öffnet das Emote-Menü oder spielt ein Emote ab", List.of("emotes"),
                new EmoteCommand(emoteService, messages));

        // Falls das Plugin im laufenden Betrieb neu geladen wird: Cosmetics der Online-Spieler laden
        getServer().getOnlinePlayers().forEach(cosmeticManager::handleJoin);

        getLogger().info(cosmeticRegistry.all().size() + " Cosmetics und " + emoteRegistry.all().size() + " Emotes geladen.");
    }

    /** Liest config.yml, die Sprachdatei, cosmetics.yml und emotes.yml (neu) ein. */
    private void loadConfiguration() {
        saveDefaultConfig();
        reloadConfig();
        settings = Settings.from(getConfig());
        messages.load(settings.language());
        cosmeticRegistry.load(ConfigFiles.loadWithLanguageDefault(this, "cosmetics", settings.language()), getLogger());
        emoteRegistry.load(ConfigFiles.loadWithLanguageDefault(this, "emotes", settings.language()), getLogger());
        registerPermissions();
    }

    /**
     * /cosmetics reload: Alles neu einlesen, ohne den Server neu zu starten.
     * Getragene Cosmetics werden kurz abgelegt und danach aus dem Speicher wieder angelegt.
     */
    public void reloadAll() {
        emoteService.stopAllPoses();
        cosmeticManager.shutdown();
        loadConfiguration();
        resourcePackService.reload();
        for (Player player : getServer().getOnlinePlayers()) {
            cosmeticManager.handleJoin(player);
        }
    }

    /**
     * Legt für jedes Cosmetic und Emote ein eigenes Recht an und hängt es an
     * nexuscosmetics.cosmetic.* bzw. nexuscosmetics.emote.*
     * "Für alle frei" wird über den Standardwert des Rechts umgesetzt.
     */
    private void registerPermissions() {
        PluginManager pluginManager = getServer().getPluginManager();
        Permission allCosmetics = pluginManager.getPermission("nexuscosmetics.cosmetic.*");
        for (Cosmetic cosmetic : cosmeticRegistry.all()) {
            register(pluginManager, allCosmetics, cosmetic.permission(), cosmetic.unlockedByDefault());
        }
        Permission allEmotes = pluginManager.getPermission("nexuscosmetics.emote.*");
        for (Emote emote : emoteRegistry.all()) {
            register(pluginManager, allEmotes, emote.permission(), emote.free());
        }
        for (Permission parent : new Permission[]{allCosmetics, allEmotes}) {
            if (parent != null) {
                parent.recalculatePermissibles();
            }
        }
    }

    private static void register(PluginManager pluginManager, Permission parent, String name, boolean free) {
        PermissionDefault defaultValue = free ? PermissionDefault.TRUE : PermissionDefault.OP;
        Permission permission = pluginManager.getPermission(name);
        if (permission == null) {
            pluginManager.addPermission(new Permission(name, defaultValue));
        } else {
            permission.setDefault(defaultValue);
        }
        if (parent != null) {
            parent.getChildren().put(name, true);
        }
    }

    // ------------------------------------------------------------------ Zugriff für andere Klassen

    public Messages messages() {
        return messages;
    }

    public Settings settings() {
        return settings;
    }

    public CosmeticManager cosmetics() {
        return cosmeticManager;
    }

    public EmoteService emotes() {
        return emoteService;
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
