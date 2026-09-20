package dev.nexus.cosmetics.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Leitet Klicks in unseren Menüs (Cosmetics, Emotes) an das Menü weiter
 * und verhindert, dass Items herausgenommen werden.
 */
public final class CosmeticMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof ClickableMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            menu.handleClick(event.getSlot(), event.getClick());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof ClickableMenu) {
            event.setCancelled(true);
        }
    }
}
