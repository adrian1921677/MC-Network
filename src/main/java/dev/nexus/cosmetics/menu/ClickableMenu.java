package dev.nexus.cosmetics.menu;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;

/** Ein Menü unseres Plugins. Items darin können nicht herausgenommen werden, Klicks landen in handleClick. */
public interface ClickableMenu extends InventoryHolder {

    void handleClick(int slot);

    /**
     * Wie {@link #handleClick(int)}, kennt aber auch die Art des Klicks (links, rechts, Shift).
     * Menüs, die das nicht brauchen, lassen die Vorgabe stehen.
     */
    default void handleClick(int slot, ClickType click) {
        handleClick(slot);
    }
}
