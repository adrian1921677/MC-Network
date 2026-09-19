package dev.nexus.cosmetics.crate;

import dev.nexus.cosmetics.config.Messages;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import dev.nexus.cosmetics.cosmetic.Rarity;
import dev.nexus.cosmetics.menu.ClickableMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Übersicht aller Truhen mit Schlüssel-Anzahl und Gewinnchancen. Klick öffnet eine Truhe. */
public final class CrateMenu implements ClickableMenu {

    private static final int SIZE = 27;
    private static final int[] SLOTS = {11, 13, 15, 10, 12, 14, 16};

    private final CrateService service;
    private final CosmeticManager cosmetics;
    private final Messages messages;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, Crate> slots = new HashMap<>();

    public CrateMenu(CrateService service, CosmeticManager cosmetics, Messages messages, Player player) {
        this.service = service;
        this.cosmetics = cosmetics;
        this.messages = messages;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, messages.get("crates.menu-title"));
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
        int index = 0;
        for (Crate crate : service.registry().all()) {
            if (index >= SLOTS.length) {
                break;
            }
            inventory.setItem(SLOTS[index], icon(crate));
            slots.put(SLOTS[index], crate);
            index++;
        }
    }

    private ItemStack icon(Crate crate) {
        int keys = cosmetics.profile(player).keys(crate.id());
        ItemStack item = ItemStack.of(Material.PAPER);
        item.editMeta(meta -> {
            meta.setItemModel(new NamespacedKey(CosmeticRegistry.NAMESPACE, "crate"));
            meta.itemName(crate.displayName());
            List<Component> lore = new ArrayList<>();
            lore.add(messages.item("crates.keys", Placeholder.unparsed("count", String.valueOf(keys))));
            lore.add(Component.empty());
            int total = crate.chances().values().stream().mapToInt(Integer::intValue).sum();
            for (Rarity rarity : Rarity.values()) {
                int chance = crate.chances().getOrDefault(rarity, 0);
                if (chance > 0 && total > 0) {
                    lore.add(messages.item("crates.chance-line",
                            Placeholder.component("rarity", messages.get("rarities." + rarity.name())),
                            Placeholder.unparsed("percent", String.format("%.1f", chance * 100.0 / total))));
                }
            }
            lore.add(Component.empty());
            lore.add(messages.item(keys > 0 ? "crates.click-open" : "crates.no-keys-lore"));
            meta.lore(lore);
        });
        return item;
    }

    @Override
    public void handleClick(int slot) {
        Crate crate = slots.get(slot);
        if (crate == null) {
            return;
        }
        switch (service.open(player, crate)) {
            case OPENED -> player.closeInventory();
            case NO_KEY -> {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(messages.prefixed("crates.no-keys"));
            }
            case ALL_OWNED -> {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
                player.sendMessage(messages.prefixed("crates.all-owned"));
            }
            case BUSY -> player.sendMessage(messages.prefixed("crates.already-opening"));
        }
    }
}
