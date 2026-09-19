package dev.nexus.cosmetics.menu;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

/**
 * Das Menü-Item in der Hotbar: ein leuchtendes Geschenk. Rechtsklick (oder Klick im Inventar) öffnet
 * das Cosmetics-Menü. Es kann nicht weggeworfen, verschoben oder verloren werden.
 */
public final class MenuItemService implements Listener {

    private final NexusCosmetics plugin;
    private final NamespacedKey itemKey;

    public MenuItemService(NexusCosmetics plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "menu_item");
    }

    public ItemStack createItem() {
        ItemStack item = ItemStack.of(Material.PAPER);
        item.editMeta(meta -> {
            meta.setItemModel(new NamespacedKey(CosmeticRegistry.NAMESPACE, "menu_item"));
            meta.itemName(plugin.messages().item("menu-item.name"));
            meta.lore(plugin.messages().itemList("menu-item.lore"));
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public boolean isMenuItem(ItemStack item) {
        return item != null && !item.isEmpty() && item.getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    /**
     * Legt das Item in den eingestellten Slot. Liegt dort schon etwas anderes, wird nichts überschrieben.
     * @param force true = auf Befehl (/cosmetics item), auch wenn es in der Config abgeschaltet ist
     */
    public void give(Player player, boolean force) {
        if (!force && !plugin.settings().menuItemEnabled()) {
            return;
        }
        if (!force && !plugin.settings().menuItemWorlds().isEmpty()
                && !plugin.settings().menuItemWorlds().contains(player.getWorld().getName())) {
            remove(player);
            return;
        }
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (isMenuItem(item)) {
                return; // hat es schon
            }
        }
        int slot = plugin.settings().menuItemSlot();
        ItemStack current = inventory.getItem(slot);
        if (current == null || current.isEmpty()) {
            inventory.setItem(slot, createItem());
        } else if (force) {
            inventory.addItem(createItem());
        }
    }

    public void remove(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isMenuItem(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    private void open(Player player) {
        new CosmeticMenu(plugin, player).open();
    }

    // ------------------------------------------------------------------ Events

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Einen Moment warten, damit andere Plugins (z. B. Lobby-Plugins) ihr Inventar zuerst setzen können
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                give(event.getPlayer(), false);
            }
        }, 10L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> give(event.getPlayer(), false));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        give(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isMenuItem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            open(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUseOnEntity(PlayerInteractEntityEvent event) {
        if (isMenuItem(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            open(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        boolean hotbarSwap = event.getHotbarButton() >= 0
                && isMenuItem(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
        if (isMenuItem(event.getCurrentItem()) || isMenuItem(event.getCursor()) || hotbarSwap) {
            event.setCancelled(true);
            if (isMenuItem(event.getCurrentItem()) && event.getWhoClicked() instanceof Player player) {
                plugin.getServer().getScheduler().runTask(plugin, () -> open(player));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (isMenuItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (isMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        // Schleichen + F bleibt für das Emote-Menü frei
        boolean emoteShortcut = event.getPlayer().isSneaking() && plugin.settings().sneakSwapShortcut();
        if (!emoteShortcut && (isMenuItem(event.getMainHandItem()) || isMenuItem(event.getOffHandItem()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isMenuItem);
    }
}
