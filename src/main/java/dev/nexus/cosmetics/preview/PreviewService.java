package dev.nexus.cosmetics.preview;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import dev.nexus.cosmetics.menu.ClickableMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
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
 * selbst ansehen. Deshalb stellt dieser Dienst eine Schaufensterpuppe mit der Haut des Spielers
 * neben das Menü und zieht ihr an, was er gerade anschaut.
 *
 * Pro Spieler gibt es höchstens eine Puppe, und sie verschwindet, sobald das Menü zu ist.
 */
public final class PreviewService implements Listener {

    private final NexusCosmetics plugin;
    private final Map<UUID, PreviewStand> stands = new HashMap<>();
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
        stands.values().forEach(PreviewStand::destroy);
        stands.clear();
    }

    /**
     * Zeigt dem Spieler, wie das Cosmetic an ihm aussehen würde.
     *
     * Die Puppe wird aus Netzwerk-Paketen zusammengesetzt. Geht dabei etwas schief, verschwindet
     * sie wieder — das Menü selbst darf daran nie zerbrechen, es ist nur eine Vorschau.
     */
    public void preview(Player viewer, Cosmetic cosmetic) {
        try {
            PreviewStand stand = stands.get(viewer.getUniqueId());
            if (stand != null && !stand.stillFits(viewer)) {
                stand.destroy();
                stand = null;
            }
            if (stand == null) {
                stand = new PreviewStand(viewer);
                stands.put(viewer.getUniqueId(), stand);
            }
            stand.wear(plugin.cosmetics(), outfitWith(viewer, cosmetic), cosmetic.slot());
        } catch (RuntimeException | LinkageError failure) {
            clear(viewer);
            plugin.getLogger().warning("Vorschau konnte nicht aufgebaut werden: " + failure);
        }
    }

    /**
     * Zieht einer offenen Puppe an, was der Spieler jetzt wirklich trägt.
     *
     * Wird nach jedem An- und Ablegen aufgerufen: Ohne das würde die Puppe weiter ein Stück
     * zeigen, das der Spieler gerade durch ein anderes ersetzt hat. Steht keine Puppe da,
     * passiert nichts.
     */
    public void refresh(Player viewer) {
        PreviewStand stand = stands.get(viewer.getUniqueId());
        if (stand == null) {
            return;
        }
        try {
            stand.wear(plugin.cosmetics(), wornOutfit(viewer), stand.focus());
        } catch (RuntimeException | LinkageError failure) {
            clear(viewer);
            plugin.getLogger().warning("Vorschau konnte nicht aktualisiert werden: " + failure);
        }
    }

    public void clear(Player viewer) {
        PreviewStand stand = stands.remove(viewer.getUniqueId());
        if (stand != null) {
            stand.destroy();
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
        for (Map.Entry<UUID, PreviewStand> entry : List.copyOf(stands.entrySet())) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer == null || !viewer.isOnline() || !entry.getValue().stillFits(viewer)) {
                entry.getValue().destroy();
                stands.remove(entry.getKey());
                continue;
            }
            try {
                entry.getValue().tick(tickCounter);
            } catch (RuntimeException | LinkageError failure) {
                // Eine kaputte Puppe wird abgeräumt, statt zwanzigmal pro Sekunde zu klagen
                stands.remove(entry.getKey());
                entry.getValue().destroy();
                plugin.getLogger().warning("Vorschau wurde wegen eines Fehlers entfernt: " + failure);
            }
        }
    }

    // ------------------------------------------------------------------ Events

    /**
     * Menü zu, Puppe weg.
     *
     * Beim Blättern und bei jedem Klick baut sich das Menü neu auf: das alte schließt sich und
     * das neue öffnet sich im selben Tick. Deshalb wird erst einen Tick später geprüft, ob
     * wirklich kein Menü mehr offen ist — sonst würde die Puppe bei jedem Klick kurz flackern.
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !stands.containsKey(player.getUniqueId())) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()
                    && !(player.getOpenInventory().getTopInventory().getHolder(false) instanceof ClickableMenu)) {
                clear(player);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stands.remove(event.getPlayer().getUniqueId());
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
