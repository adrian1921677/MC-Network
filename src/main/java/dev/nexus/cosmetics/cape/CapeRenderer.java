package dev.nexus.cosmetics.cape;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet alle Capes: wer trägt eine, wer sieht sie, und bewegt sie jeden Tick.
 *
 * Jeder Träger hat zwei Capes: eine, die alle anderen sehen, und eine eigene für sich selbst.
 * So kann die eigene Cape ausgeblendet werden, wenn man nach unten schaut, ohne dass sie
 * für andere verschwindet.
 */
public final class CapeRenderer implements Listener {

    private static final int PASSENGER_RESEND_TICKS = 40;

    private record WornCape(FakeCape forOthers, FakeCape forSelf) {
        void tick(boolean resendPassengers) {
            forOthers.tick();
            forSelf.tick();
            if (resendPassengers) {
                forOthers.resendPassengers();
                forSelf.resendPassengers();
            }
        }

        void destroy() {
            forOthers.destroy();
            forSelf.destroy();
        }
    }

    private final Plugin plugin;
    private final Map<UUID, WornCape> capes = new HashMap<>();
    private BukkitTask task;
    private int tickCounter;

    public CapeRenderer(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        capes.values().forEach(WornCape::destroy);
        capes.clear();
    }

    /** Legt dem Spieler eine Cape an und zeigt sie ihm selbst und allen Spielern in der Nähe. */
    public void show(Player wearer, Cosmetic cosmetic) {
        hide(wearer);
        WornCape cape = new WornCape(new FakeCape(wearer, cosmetic, false), new FakeCape(wearer, cosmetic, true));
        capes.put(wearer.getUniqueId(), cape);
        cape.forSelf().show(wearer);
        for (Player viewer : wearer.getTrackedBy()) {
            cape.forOthers().show(viewer);
        }
    }

    public void hide(Player wearer) {
        WornCape cape = capes.remove(wearer.getUniqueId());
        if (cape != null) {
            cape.destroy();
        }
    }

    private void tick() {
        tickCounter++;
        boolean resendPassengers = tickCounter % PASSENGER_RESEND_TICKS == 0;
        for (WornCape cape : capes.values()) {
            cape.tick(resendPassengers);
        }
    }

    // ------------------------------------------------------------------ Events

    /** Ein Spieler kommt in Sichtweite eines Cape-Trägers. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrack(PlayerTrackEntityEvent event) {
        if (!(event.getEntity() instanceof Player wearer)) {
            return;
        }
        Player viewer = event.getPlayer();
        // Einen Tick warten, bis der Träger beim Zuschauer wirklich gespawnt ist
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            WornCape cape = capes.get(wearer.getUniqueId());
            if (cape != null && viewer.isOnline()) {
                cape.forOthers().show(viewer);
            }
        });
    }

    /** Ein Spieler verliert einen Cape-Träger aus der Sichtweite. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onUntrack(PlayerUntrackEntityEvent event) {
        if (event.getEntity() instanceof Player wearer) {
            WornCape cape = capes.get(wearer.getUniqueId());
            if (cape != null) {
                cape.forOthers().hide(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        refreshOwnCapeLater(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        refreshOwnCapeLater(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID quitter = event.getPlayer().getUniqueId();
        for (WornCape cape : capes.values()) {
            cape.forOthers().forget(quitter);
        }
    }

    /** Der Träger selbst bekommt seine Cape nach Respawn/Weltwechsel neu geschickt. */
    private void refreshOwnCapeLater(Player wearer) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            WornCape cape = capes.get(wearer.getUniqueId());
            if (cape != null && wearer.isOnline()) {
                cape.forSelf().respawnFor(wearer);
            }
        });
    }
}
