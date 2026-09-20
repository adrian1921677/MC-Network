package dev.nexus.showcase;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.nexus.cosmetics.NexusCosmetics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Das Showcase-Plugin: führt Besucher des Demo-Servers automatisch durch NexusCosmetics.
 *
 * Es gehört bewusst nicht zum verkauften Plugin. Wer NexusCosmetics kauft, bekommt keine
 * Führung mitgeliefert, die er nie braucht — und dieses Plugin darf dafür ungeniert alles
 * tun, was auf einem Demo-Server erlaubt ist: Welt anlegen, Inventare leeren, Spieler festhalten.
 */
public final class NexusShowcase extends JavaPlugin {

    private NexusCosmetics nexus;
    private Script script;
    private ShowcaseWorld showcaseWorld;
    private TourManager tours;
    private PlayerProfile guideSkin;
    private Component guideName = Component.text("Guide");

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!(getServer().getPluginManager().getPlugin("NexusCosmetics") instanceof NexusCosmetics found)) {
            getLogger().severe("NexusCosmetics fehlt. Ohne das Plugin gibt es nichts vorzuführen.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        nexus = found;

        loadScript();
        showcaseWorld = new ShowcaseWorld(this,
                getConfig().getString("world.name", "nexus_showcase"),
                getConfig().getInt("world.spacing", 60),
                getConfig().getInt("world.height", 70));
        showcaseWorld.prepare();
        showcaseWorld.build(script.stations());

        tours = new TourManager(this);
        tours.start();
        getServer().getPluginManager().registerEvents(tours, this);
        registerCommand("tour", "Startet oder beendet die Führung", List.of("fuehrung"), new TourCommand(this));

        fetchGuideSkin();
    }

    @Override
    public void onDisable() {
        if (tours != null) {
            tours.stop();
        }
    }

    /** Texte und Einstellungen neu einlesen, ohne den Server zu starten. */
    public void reloadEverything() {
        reloadConfig();
        loadScript();
        fetchGuideSkin();
    }

    private void loadScript() {
        script = Script.load(this,
                getConfig().getString("language", "de"),
                getConfig().getInt("tour.ticks-per-word", 9),
                getConfig().getInt("tour.min-hold-ticks", 45));
        guideName = MiniMessage.miniMessage().deserialize(getConfig().getString("guide.name", "Guide"));
    }

    /**
     * Holt die Haut des Führers bei Mojang.
     *
     * Das ist ein Netzwerk-Aufruf und läuft deshalb nebenher. Klappt er nicht — kein Internet,
     * Name falsch geschrieben — steht der Führer eben ohne Haut da, aber die Führung läuft.
     */
    private void fetchGuideSkin() {
        String name = getConfig().getString("guide.skin", "");
        if (name.isBlank()) {
            return;
        }
        getServer().getAsyncScheduler().runNow(this, task -> {
            PlayerProfile profile = Bukkit.createProfile(name);
            boolean complete = profile.complete(true);
            getServer().getScheduler().runTask(this, () -> {
                if (complete && profile.isComplete()) {
                    guideSkin = profile;
                    getLogger().info("Haut des Führers geladen: " + name);
                } else {
                    getLogger().warning("Für '" + name + "' gibt es keine Haut bei Mojang. "
                            + "Der Führer bleibt ohne Haut.");
                }
            });
        });
    }

    // ------------------------------------------------------------------ Zugriff

    /** Das verkaufte Plugin, das hier vorgeführt wird. */
    public NexusCosmetics nexus() {
        return nexus;
    }

    public Script script() {
        return script;
    }

    public ShowcaseWorld showcaseWorld() {
        return showcaseWorld;
    }

    public TourManager tours() {
        return tours;
    }

    /** Kann null sein, solange die Haut noch lädt oder nicht gefunden wurde. */
    public PlayerProfile guideSkin() {
        return guideSkin;
    }

    public Component guideName() {
        return guideName;
    }
}
