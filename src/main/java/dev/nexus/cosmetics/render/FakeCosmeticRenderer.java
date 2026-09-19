package dev.nexus.cosmetics.render;

import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Verwaltet alle Paket-Cosmetics (Capes, Haustiere, Emotes ...): wer trägt was, wer sieht es,
 * und bewegt alles jeden Tick. Jeder Spieler hat pro "Platz" (z. B. BACK, PET, EMOTE) höchstens eines.
 *
 * Sichtbarkeit: Ein Spieler sieht die Cosmetics eines anderen genau dann, wenn der Server
 * ihm diesen Spieler anzeigt ("tracking"). Der Träger sieht seine eigenen immer.
 */
public final class FakeCosmeticRenderer implements Listener {

    /** Vanilla schickt die Passagier-Liste manchmal neu (z. B. beim Aufsteigen). Dann senden wir unsere erneut. */
    private static final int PASSENGER_RESEND_TICKS = 40;

    private final Plugin plugin;
    private final Map<UUID, Map<String, FakeCosmetic>> worn = new HashMap<>();
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
        show(wearer, slot.name(), cosmetic);
    }

    public void hide(Player wearer, CosmeticSlot slot) {
        hide(wearer, slot.name());
    }

    public void show(Player wearer, String place, FakeCosmetic cosmetic) {
        hide(wearer, place);
        worn.computeIfAbsent(wearer.getUniqueId(), uuid -> new LinkedHashMap<>()).put(place, cosmetic);
        for (Player viewer : viewersOf(wearer)) {
            cosmetic.show(viewer);
            sendPassengers(wearer, viewer);
        }
    }

    public void hide(Player wearer, String place) {
        Map<String, FakeCosmetic> slots = worn.get(wearer.getUniqueId());
        if (slots == null) {
            return;
        }
        FakeCosmetic cosmetic = slots.remove(place);
        if (cosmetic != null) {
            cosmetic.destroy();
        }
        if (slots.isEmpty()) {
            worn.remove(wearer.getUniqueId());
        }
    }

    private void tick() {
        tickCounter++;
        boolean resendPassengers = tickCounter % PASSENGER_RESEND_TICKS == 0;
        for (Map.Entry<UUID, Map<String, FakeCosmetic>> entry : List.copyOf(worn.entrySet())) {
            for (Map.Entry<String, FakeCosmetic> placed : List.copyOf(entry.getValue().entrySet())) {
                FakeCosmetic cosmetic = placed.getValue();
                cosmetic.tick(tickCounter);
                if (cosmetic.finished()) {
                    cosmetic.destroy();
                    entry.getValue().remove(placed.getKey());
                }
            }
            if (entry.getValue().isEmpty()) {
                worn.remove(entry.getKey());
                continue;
            }
            if (resendPassengers) {
                Player wearer = Bukkit.getPlayer(entry.getKey());
                if (wearer != null) {
                    viewersOf(wearer).forEach(viewer -> sendPassengers(wearer, viewer));
                }
            }
        }
    }

    private List<FakeCosmetic> cosmeticsOf(Player wearer) {
        Map<String, FakeCosmetic> slots = worn.get(wearer.getUniqueId());
        return slots == null ? List.of() : List.copyOf(slots.values());
    }

    /** Der Träger selbst und alle Spieler, die ihn gerade sehen. */
    private static List<Player> viewersOf(Player wearer) {
        List<Player> viewers = new ArrayList<>();
        viewers.add(wearer);
        viewers.addAll(wearer.getTrackedBy());
        return viewers;
    }

    /** Schickt die gemeinsame Passagier-Liste aller Cosmetics des Trägers (Cape, Schulter-Haustier ...). */
    private void sendPassengers(Player wearer, Player viewer) {
        int[] ids = cosmeticsOf(wearer).stream()
                .flatMapToInt(cosmetic -> IntStream.of(cosmetic.passengerIds(viewer)))
                .toArray();
        if (ids.length > 0) {
            Packets.send(viewer, Packets.passengers(wearer, ids));
        }
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
            if (viewer.isOnline() && wearer.isOnline()) {
                cosmeticsOf(wearer).forEach(cosmetic -> cosmetic.show(viewer));
                sendPassengers(wearer, viewer);
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
                sendPassengers(wearer, wearer);
            }
        });
    }
}
