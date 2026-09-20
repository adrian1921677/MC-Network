package dev.nexus.showcase;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet die laufenden Führungen.
 *
 * Jeder Besucher bekommt seine eigene Führung mit eigenem Führer. Damit sich zwei Besucher
 * nicht gegenseitig die Puppen in die Plattform stellen, sieht jeder nur die Figuren seiner
 * eigenen Führung — dafür sorgt hideFromOthers().
 */
public final class TourManager implements Listener {

    /** Ticks nach dem Betreten, bis die Führung startet. Der Client soll erst ankommen. */
    private static final int START_DELAY = 40;

    private final NexusShowcase plugin;
    private final Map<UUID, Tour> tours = new HashMap<>();
    private BukkitTask task;

    public TourManager(NexusShowcase plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Beendet alles und gibt jedem Besucher sein Inventar zurück. */
    public void stop() {
        if (task != null) {
            task.cancel();
        }
        List.copyOf(tours.values()).forEach(tour -> tour.stop(true));
        tours.clear();
    }

    // ------------------------------------------------------------------ Starten und Beenden

    public void begin(Player visitor) {
        end(visitor, false);
        Tour tour = new Tour(plugin, visitor);
        tours.put(visitor.getUniqueId(), tour);
        tour.ownEntities().forEach(entity -> hideFromOthers(tour, entity));
    }

    public void end(Player visitor, boolean teleportBack) {
        Tour tour = tours.remove(visitor.getUniqueId());
        if (tour != null) {
            tour.stop(teleportBack);
        }
    }

    public Tour of(Player player) {
        return tours.get(player.getUniqueId());
    }

    /** Diese Figur gehört zu einer Führung — alle anderen Spieler sollen sie nicht sehen. */
    public void hideFromOthers(Tour tour, Entity entity) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(tour.visitor().getUniqueId())) {
                other.hideEntity(plugin, entity);
            }
        }
    }

    private void tick() {
        for (Map.Entry<UUID, Tour> entry : List.copyOf(tours.entrySet())) {
            Tour tour = entry.getValue();
            if (tour.stopped() || !tour.visitor().isOnline()) {
                tours.remove(entry.getKey());
                continue;
            }
            try {
                tour.tick();
            } catch (RuntimeException | LinkageError failure) {
                plugin.getLogger().warning("Führung von " + tour.visitor().getName()
                        + " abgebrochen: " + failure);
                tours.remove(entry.getKey());
                tour.stop(true);
            }
        }
    }

    // ------------------------------------------------------------------ Kommen und Gehen

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Laufende Führungen anderer sind für den Neuen unsichtbar
        tours.values().forEach(tour -> tour.ownEntities().forEach(entity -> {
            if (!tour.visitor().getUniqueId().equals(player.getUniqueId())) {
                player.hideEntity(plugin, entity);
            }
        }));

        if (!plugin.getConfig().getBoolean("tour.auto-start", true)
                || player.hasPermission("nexusshowcase.skip")) {
            return;
        }
        // Erst nach kurzer Pause: Der Client laedt noch Welt und Resource Pack
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !tours.containsKey(player.getUniqueId())) {
                begin(player);
            }
        }, START_DELAY);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        end(event.getPlayer(), false);
    }

    // ------------------------------------------------------------------ Der Besucher bleibt stehen

    /**
     * Umsehen ja, weglaufen nein.
     *
     * Der Blick wird durchgelassen und nur die Position zurückgesetzt. Während der Fahrt
     * sitzt der Besucher auf dem Träger — dann muss die Bewegung natürlich durch.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Tour tour = tours.get(event.getPlayer().getUniqueId());
        if (tour == null || tour.riding() || tour.finished()) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }
        Location fixed = from.clone();
        fixed.setYaw(to.getYaw());
        fixed.setPitch(to.getPitch());
        event.setTo(fixed);
    }

    /** Absteigen würde den Besucher mitten in der Luft stehen lassen. */
    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            Tour tour = tours.get(player.getUniqueId());
            if (tour != null && tour.riding()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && tours.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (tours.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (tours.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (tours.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (tours.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ Das Frage-Buch

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Tour tour = tours.get(event.getPlayer().getUniqueId());
        if (tour == null || event.getItem() == null
                || event.getItem().getType() != org.bukkit.Material.WRITABLE_BOOK) {
            return;
        }
        event.setCancelled(true);
        if (event.getAction().isRightClick()) {
            new QuestionMenu(plugin.script(), tour).open();
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder(false) instanceof QuestionMenu menu) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                menu.handleClick(event.getSlot());
            }
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof QuestionMenu) {
            event.setCancelled(true);
        }
    }
}
