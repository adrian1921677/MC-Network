package dev.nexus.cosmetics.menu;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Das Auswahl-Menü. Jedes Cosmetic wird als 3D-Modell im Menü angezeigt.
 * Reihe 2: Hüte, Reihe 3: Capes, Reihe 4: Haustiere.
 */
public final class CosmeticMenu implements InventoryHolder {

    private static final int SIZE = 45;
    private static final int UNEQUIP_SLOT = 40;
    private static final Map<CosmeticSlot, Integer> ROW_START = Map.of(
            CosmeticSlot.HEAD, 10,
            CosmeticSlot.BACK, 19,
            CosmeticSlot.PET, 28);

    private final CosmeticManager manager;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, Cosmetic> cosmeticSlots = new HashMap<>();

    public CosmeticMenu(CosmeticManager manager, Player player) {
        this.manager = manager;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, Component.text("Cosmetics"));
        render();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void render() {
        inventory.clear();
        cosmeticSlots.clear();

        Map<CosmeticSlot, Integer> nextSlot = new HashMap<>(ROW_START);
        for (Cosmetic cosmetic : manager.registry().all()) {
            List<Component> lore;
            if (cosmetic.equals(manager.equipped(player, cosmetic.slot()))) {
                lore = List.of(Component.text("✔ Ausgerüstet", NamedTextColor.GREEN),
                        Component.text("Klicken zum Ablegen", NamedTextColor.GRAY));
            } else if (player.hasPermission(cosmetic.permission())) {
                lore = List.of(Component.text("Klicken zum Tragen", NamedTextColor.YELLOW));
            } else {
                lore = List.of(Component.text("🔒 Nicht freigeschaltet", NamedTextColor.RED));
            }
            int slot = nextSlot.merge(cosmetic.slot(), 1, Integer::sum) - 1;
            inventory.setItem(slot, manager.createItem(cosmetic, lore));
            cosmeticSlots.put(slot, cosmetic);
        }

        ItemStack unequip = ItemStack.of(Material.BARRIER);
        unequip.editMeta(meta -> meta.itemName(Component.text("Alles ablegen", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)));
        inventory.setItem(UNEQUIP_SLOT, unequip);
    }

    public void handleClick(int slot) {
        if (slot == UNEQUIP_SLOT) {
            manager.unequipAll(player);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
            render();
            return;
        }

        Cosmetic cosmetic = cosmeticSlots.get(slot);
        if (cosmetic == null) {
            return;
        }

        // Nochmal auf ein getragenes Cosmetic klicken = ablegen
        if (cosmetic.equals(manager.equipped(player, cosmetic.slot()))) {
            manager.unequip(player, cosmetic.slot());
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
            render();
            return;
        }

        switch (manager.equip(player, cosmetic)) {
            case EQUIPPED -> {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1.2f);
                player.sendMessage(CosmeticManager.prefix()
                        .append(Component.text("Du trägst jetzt: ", NamedTextColor.GRAY))
                        .append(cosmetic.displayName()));
            }
            case NO_PERMISSION -> {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(CosmeticManager.prefix()
                        .append(Component.text("Dieses Cosmetic hast du noch nicht freigeschaltet.", NamedTextColor.RED)));
            }
            case HEAD_OCCUPIED -> {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(CosmeticManager.prefix()
                        .append(Component.text("Nimm zuerst deinen Helm ab.", NamedTextColor.RED)));
            }
        }
        render();
    }
}
