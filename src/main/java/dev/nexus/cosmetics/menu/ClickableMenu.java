package dev.nexus.cosmetics.menu;

import org.bukkit.inventory.InventoryHolder;

/** Ein Menü unseres Plugins. Items darin können nicht herausgenommen werden, Klicks landen in handleClick. */
public interface ClickableMenu extends InventoryHolder {

    void handleClick(int slot);
}
