package dev.nexus.cosmetics.render;

import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet alle Paket-Cosmetics (Capes, Haustiere ...): wer trägt was, wer sieht es,
 * und bewegt alles jeden Tick.
 *
 * Sichtbarkeit: Ein Spieler sieht die Cosmetics eines anderen genau dann, wenn der Server
 * ihm diesen Spieler anzeigt ("tracking"). Der Träger sieht seine eigenen immer.
 */
public final class FakeCosmeticRenderer implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Map<CosmeticSlot, FakeCosmetic>> worn = new HashMap<>();
    private BukkitTask task;
    private int tickCounter;

    public FakeCosmeticRenderer(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        worn.values().forEach(slots -> slots.values().forEach(FakeCosmetic::destroy));
        worn.clear();
    }

    /** Legt dem Spieler ein Cosmetic an und zeigt es ihm selbst und allen Spielern in der Nähe. */
    public void show(Player wearer, CosmeticSlot slot, FakeCosmetic cosmetic) {
        hide(wearer, slot);
        worn.computeIfAbsent(wearer.getUniqueId(), uuid -> new EnumMap<>(CosmeticSlot.class)).put(slot, cosmetic);
        cosmetic.show(wearer);
        for (Player viewer : wearer.getTrackedBy()) {
            cosmetic.show(viewer);
        }
    }

    public void hide(Player wearer, CosmeticSlot slot) {
        Map<CosmeticSlot, FakeCosmetic> slots = worn.get(wearer.getUniqueId());
        if (slots == null) {
            return;
        }
        FakeCosmetic cosmetic = slots.remove(slot);
        if (cosmetic != null) {
            cosmetic.destroy();
        }
        if (slots.isEmpty()) {
            worn.remove(wearer.getUniqueId());
        }
    }

    private void tick() {
        tickCounter++;
        for (Map<CosmeticSlot, FakeCosmetic> slots : worn.values()) {
            for (FakeCosmetic cosmetic : slots.values()) {
                cosmetic.tick(tickCounter);
            }
        }
    }

    private List<FakeCosmetic> cosmeticsOf(Player wearer) {
        Map<CosmeticSlot, FakeCosmetic> slots = worn.get(wearer.getUniqueId());
        return slots == null ? List.of() : List.copyOf(slots.values());
    }

    // ------------------------------------------------------------------ Events

    /** Ein Spieler kommt in Sichtweite eines Trägers. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrack(PlayerTrackEntityEvent event) {
        if (!(event.getEntity() instanceof Player wearer)) {
            return;
        }
        Player viewer = event.getPlayer();
        // Einen Tick warten, bis der Träger beim Zuschauer wirklich gespawnt ist
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (viewer.isOnline()) {
                cosmeticsOf(wearer).forEach(cosmetic -> cosmetic.show(viewer));
            }
        });
    }

    /** Ein Spieler verliert einen Träger aus der Sichtweite. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onUntrack(PlayerUntrackEntityEvent event) {
        if (event.getEntity() instanceof Player wearer) {
            cosmeticsOf(wearer).forEach(cosmetic -> cosmetic.hide(event.getPlayer()));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        refreshOwnCosmeticsLater(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        refreshOwnCosmeticsLater(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID quitter = event.getPlayer().getUniqueId();
        worn.values().forEach(slots -> slots.values().forEach(cosmetic -> cosmetic.forget(quitter)));
    }

    /** Der Träger selbst bekommt seine Cosmetics nach Respawn/Weltwechsel neu geschickt. */
    private void refreshOwnCosmeticsLater(Player wearer) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (wearer.isOnline()) {
                cosmeticsOf(wearer).forEach(cosmetic -> cosmetic.respawnFor(wearer));
            }
        });
    }
}
