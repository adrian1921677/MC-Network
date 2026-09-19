package dev.nexus.cosmetics.cosmetic;

import dev.nexus.cosmetics.cape.CapeRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet, welcher Spieler welches Cosmetic trägt.
 *
 * - Hüte (HEAD) liegen im Helm-Slot und sind dort geschützt. Ideal für Lobbys ohne Rüstung.
 * - Capes (BACK) sind Fake-Entities, die nur per Netzwerk-Paket existieren (siehe CapeRenderer).
 */
public final class CosmeticManager {

    public enum EquipResult { EQUIPPED, NO_PERMISSION, HEAD_OCCUPIED }

    private final CosmeticRegistry registry;
    private final CapeRenderer capeRenderer;
    /** Markierung, an der wir unsere Cosmetic-Items erkennen. */
    private final NamespacedKey cosmeticKey;
    private final Map<UUID, Map<CosmeticSlot, Cosmetic>> equipped = new HashMap<>();

    public CosmeticManager(Plugin plugin, CosmeticRegistry registry, CapeRenderer capeRenderer) {
        this.registry = registry;
        this.capeRenderer = capeRenderer;
        this.cosmeticKey = new NamespacedKey(plugin, "cosmetic");
    }

    public CosmeticRegistry registry() {
        return registry;
    }

    public Cosmetic equipped(Player player, CosmeticSlot slot) {
        Map<CosmeticSlot, Cosmetic> slots = equipped.get(player.getUniqueId());
        return slots == null ? null : slots.get(slot);
    }

    public EquipResult equip(Player player, Cosmetic cosmetic) {
        if (!player.hasPermission(cosmetic.permission())) {
            return EquipResult.NO_PERMISSION;
        }
        switch (cosmetic.slot()) {
            case HEAD -> {
                ItemStack current = player.getInventory().getHelmet();
                if (current != null && !current.isEmpty() && !isCosmeticItem(current)) {
                    return EquipResult.HEAD_OCCUPIED;
                }
                player.getInventory().setHelmet(createItem(cosmetic, List.of()));
            }
            case BACK -> capeRenderer.show(player, cosmetic);
        }
        equipped.computeIfAbsent(player.getUniqueId(), uuid -> new EnumMap<>(CosmeticSlot.class))
                .put(cosmetic.slot(), cosmetic);
        return EquipResult.EQUIPPED;
    }

    public void unequip(Player player, CosmeticSlot slot) {
        Map<CosmeticSlot, Cosmetic> slots = equipped.get(player.getUniqueId());
        if (slots != null) {
            slots.remove(slot);
        }
        switch (slot) {
            case HEAD -> removeCosmeticItems(player);
            case BACK -> capeRenderer.hide(player);
        }
    }

    public void unequipAll(Player player) {
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            unequip(player, slot);
        }
        equipped.remove(player.getUniqueId());
    }

    /** Setzt den Hut nach einem Respawn erneut auf, falls er verloren ging. */
    public void restoreHat(Player player) {
        Cosmetic hat = equipped(player, CosmeticSlot.HEAD);
        ItemStack helmet = player.getInventory().getHelmet();
        if (hat != null && (helmet == null || helmet.isEmpty())) {
            player.getInventory().setHelmet(createItem(hat, List.of()));
        }
    }

    public void unequipEveryone() {
        for (UUID uuid : List.copyOf(equipped.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                unequipAll(player);
            }
        }
        equipped.clear();
    }

    /** Entfernt alle Cosmetic-Items aus dem Inventar (z. B. nach einem Server-Absturz). */
    public void removeCosmeticItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isCosmeticItem(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    public boolean isCosmeticItem(ItemStack item) {
        return item != null && !item.isEmpty()
                && item.getPersistentDataContainer().has(cosmeticKey, PersistentDataType.STRING);
    }

    /** Baut ein Item, das mit unserem 3D-Modell angezeigt wird. */
    public ItemStack createItem(Cosmetic cosmetic, List<Component> lore) {
        ItemStack item = ItemStack.of(Material.PAPER);
        item.editMeta(meta -> {
            meta.setItemModel(cosmetic.model());
            meta.itemName(cosmetic.displayName());
            meta.lore(lore.stream()
                    .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .toList());
            meta.getPersistentDataContainer().set(cosmeticKey, PersistentDataType.STRING, cosmetic.id());
        });
        return item;
    }

    public static Component prefix() {
        return Component.text("Cosmetics » ", NamedTextColor.LIGHT_PURPLE);
    }
}
