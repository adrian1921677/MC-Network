package dev.nexus.cosmetics.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Leitet Klicks im Cosmetics-Menü an das Menü weiter und verhindert, dass Items herausgenommen werden.
 */
public final class CosmeticMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof CosmeticMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            menu.handleClick(event.getSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof CosmeticMenu) {
            event.setCancelled(true);
        }
    }
}
