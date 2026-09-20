package dev.nexus.cosmetics.preview;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Die Vorschau im Menü.
 *
 * Ein Chest-Menü blendet die eigene Spielfigur aus — man kann sich beim Aussuchen also nicht
 * selbst ansehen. Für die Vorschau schliesst sich deshalb das Menü, und eine Schaufensterpuppe
 * mit der Haut des Spielers stellt sich frei vor ihn. Danach geht das Menü von selbst wieder
 * auf, an genau derselben Stelle; Schleichen bringt sofort zurück.
 *
 * Der erste Versuch stellte die Puppe neben das geöffnete Menü. Wie viel Platz daneben bleibt,
 * hängt aber von Fenstergrösse, GUI-Grösse und Sichtfeld ab — bei zu wenig Platz verschwand
 * sie hinter dem Menü. Menü zu ist der verlässlichere Weg.
 */
public final class PreviewService implements Listener {

    /** Eine laufende Vorschau: die Puppe und der Weg zurück ins Menü. */
    private static final class Showing {
        private final PreviewStand stand;
        private final Runnable reopenMenu;
        private final Component label;
        private int remaining;

        private Showing(PreviewStand stand, Runnable reopenMenu, Component label, int remaining) {
            this.stand = stand;
            this.reopenMenu = reopenMenu;
            this.label = label;
            this.remaining = remaining;
        }
    }

    private final NexusCosmetics plugin;
    private final Map<UUID, Showing> showing = new HashMap<>();
    private BukkitTask task;
    private int tickCounter;

    public PreviewService(NexusCosmetics plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        clearAll();
    }

    /** Räumt alle Puppen ab, z. B. weil die Cosmetics gerade neu eingelesen werden. */
    public void clearAll() {
        showing.values().forEach(entry -> entry.stand.destroy());
        showing.clear();
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("preview.enabled", true);
    }

    // ------------------------------------------------------------------ Zeigen

    /**
     * Schliesst das Menü und stellt die Puppe vor den Spieler.
     *
     * @param reopenMenu wird aufgerufen, wenn die Vorschau vorbei ist — damit der Spieler
     *                   wieder genau auf der Seite landet, von der er gekommen ist
     */
    public void preview(Player viewer, Cosmetic cosmetic, Runnable reopenMenu) {
        if (!enabled()) {
            return;
        }
        try {
            clear(viewer);
            PreviewStand stand = new PreviewStand(viewer, placement());
            stand.wear(plugin.cosmetics(), outfitWith(viewer, cosmetic), cosmetic.slot());
            showing.put(viewer.getUniqueId(), new Showing(stand, reopenMenu, cosmetic.displayName(), seconds()));
            // Erst jetzt schliessen: Das Schliessen selbst raeumt keine Vorschau ab
            viewer.closeInventory();
        } catch (RuntimeException | LinkageError failure) {
            clear(viewer);
            plugin.getLogger().warning("Vorschau konnte nicht aufgebaut werden: " + failure);
        }
    }

    private PreviewStand.Placement placement() {
        double turnSeconds = plugin.getConfig().getDouble("preview.turn-seconds", 9.0);
        return new PreviewStand.Placement(
                (float) plugin.getConfig().getDouble("preview.side-angle", 0),
                Math.max(1.4, plugin.getConfig().getDouble("preview.distance", 2.6)),
                plugin.getConfig().getDouble("preview.height-offset", 0),
                turnSeconds <= 0 ? 0f : (float) (360.0 / (turnSeconds * 20.0)));
    }

    private int seconds() {
        return Math.max(20, (int) (plugin.getConfig().getDouble("preview.seconds", 6) * 20));
    }

    /**
     * Zieht einer laufenden Vorschau an, was der Spieler jetzt wirklich trägt. Steht keine
     * Puppe da, passiert nichts.
     */
    public void refresh(Player viewer) {
        Showing entry = showing.get(viewer.getUniqueId());
        if (entry == null) {
            return;
        }
        try {
            entry.stand.wear(plugin.cosmetics(), wornOutfit(viewer), entry.stand.focus());
        } catch (RuntimeException | LinkageError failure) {
            clear(viewer);
            plugin.getLogger().warning("Vorschau konnte nicht aktualisiert werden: " + failure);
        }
    }

    /** Puppe weg, ohne das Menü wieder zu öffnen. */
    public void clear(Player viewer) {
        Showing entry = showing.remove(viewer.getUniqueId());
        if (entry != null) {
            entry.stand.destroy();
        }
    }

    /** Vorschau vorbei: Puppe weg und zurück ins Menü. */
    private void finish(Player viewer) {
        Showing entry = showing.remove(viewer.getUniqueId());
        if (entry == null) {
            return;
        }
        entry.stand.destroy();
        if (viewer.isOnline() && entry.reopenMenu != null) {
            entry.reopenMenu.run();
        }
    }

    /**
     * Was der Spieler ohnehin trägt, plus das angesehene Stück an seinem Platz. So beurteilt er
     * die Kombination und nicht nur ein einzelnes Teil.
     */
    private Map<CosmeticSlot, Cosmetic> outfitWith(Player viewer, Cosmetic preview) {
        Map<CosmeticSlot, Cosmetic> outfit = wornOutfit(viewer);
        outfit.put(preview.slot(), preview);
        return outfit;
    }

    private Map<CosmeticSlot, Cosmetic> wornOutfit(Player viewer) {
        CosmeticManager manager = plugin.cosmetics();
        Map<CosmeticSlot, Cosmetic> outfit = new EnumMap<>(CosmeticSlot.class);
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            Cosmetic worn = manager.equipped(viewer, slot);
            if (worn != null) {
                outfit.put(slot, worn);
            }
        }
        return outfit;
    }

    private void tick() {
        tickCounter++;
        for (Map.Entry<UUID, Showing> entry : List.copyOf(showing.entrySet())) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            Showing shown = entry.getValue();
            if (viewer == null || !viewer.isOnline()) {
                showing.remove(entry.getKey());
                shown.stand.destroy();
                continue;
            }
            // Weggelaufen: Puppe abraeumen, aber nicht ungefragt das Menue aufreissen
            if (!shown.stand.stillFits(viewer)) {
                clear(viewer);
                continue;
            }
            if (--shown.remaining <= 0) {
                finish(viewer);
                continue;
            }
            // Der Hinweis in der Aktionsleiste verblasst nach drei Sekunden, die Vorschau
            // dauert laenger. Also immer wieder auffrischen.
            if (shown.remaining % 20 == 0) {
                viewer.sendActionBar(plugin.messages().get("menu.preview-shown",
                        Placeholder.component("name", shown.label)));
            }
            try {
                shown.stand.tick(tickCounter);
            } catch (RuntimeException | LinkageError failure) {
                // Eine kaputte Puppe wird abgeräumt, statt zwanzigmal pro Sekunde zu klagen
                clear(viewer);
                plugin.getLogger().warning("Vorschau wurde wegen eines Fehlers entfernt: " + failure);
            }
        }
    }

    // ------------------------------------------------------------------ Events

    /** Schleichen bringt sofort zurück ins Menü, statt die Sekunden abzuwarten. */
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && showing.containsKey(event.getPlayer().getUniqueId())) {
            finish(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        showing.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        clear(event.getPlayer());
    }
}
