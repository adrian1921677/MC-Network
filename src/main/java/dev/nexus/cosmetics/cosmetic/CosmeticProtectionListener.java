package dev.nexus.cosmetics.cosmetic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Sorgt dafür, dass Cosmetic-Items nicht verschoben, gedroppt oder bei Tod fallen gelassen werden.
 */
public final class CosmeticProtectionListener implements Listener {

    private final Plugin plugin;
    private final CosmeticManager manager;

    public CosmeticProtectionListener(Plugin plugin, CosmeticManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        boolean hotbarSwapsCosmetic = event.getHotbarButton() >= 0
                && manager.isCosmeticItem(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
        if (manager.isCosmeticItem(event.getCurrentItem())
                || manager.isCosmeticItem(event.getCursor())
                || hotbarSwapsCosmetic) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (manager.isCosmeticItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (manager.isCosmeticItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /** Verhindert, dass ein Helm per Rechtsklick mit dem Hut getauscht wird. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (item != null && item.getType().getEquipmentSlot() == EquipmentSlot.HEAD
                && manager.isCosmeticItem(player.getInventory().getHelmet())) {
            event.setCancelled(true);
            player.sendMessage(CosmeticManager.prefix().append(
                    Component.text("Leg zuerst deinen Hut ab (/cosmetics).", NamedTextColor.RED)));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(manager::isCosmeticItem);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Einen Tick warten, bis der Spieler wirklich respawnt ist
        plugin.getServer().getScheduler().runTask(plugin, () -> manager.restoreHat(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Aufräumen, falls nach einem Absturz noch alte Cosmetic-Items im Inventar liegen
        manager.removeCosmeticItems(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.unequipAll(event.getPlayer());
    }
}
