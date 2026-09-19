package dev.nexus.cosmetics.menu;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.config.Messages;
import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import dev.nexus.cosmetics.crate.CrateMenu;
import dev.nexus.cosmetics.emote.EmoteMenu;
import dev.nexus.cosmetics.emote.EmoteService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Das Cosmetics-Menü.
 *
 * - Hauptmenü: Kategorien (Hüte, Umhänge, Haustiere), Emotes, Truhen und "Alles ablegen".
 * - Kategorie-Seite: bis zu 28 Cosmetics pro Seite (als 3D-Modell), mit Blättern und Zurück.
 */
public final class CosmeticMenu implements ClickableMenu {

    // Hauptmenü
    private static final int MAIN_SIZE = 27;
    private static final Map<CosmeticSlot, Integer> CATEGORY_SLOTS = Map.of(
            CosmeticSlot.HEAD, 10,
            CosmeticSlot.BACK, 11,
            CosmeticSlot.PET, 12);
    private static final int EMOTES_SLOT = 14;
    private static final int CRATES_SLOT = 16;
    private static final int UNEQUIP_ALL_SLOT = 22;

    // Kategorie-Seite
    private static final int PAGE_SIZE = 54;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};
    private static final int BACK_SLOT = 45;
    private static final int PREVIOUS_SLOT = 48;
    private static final int UNEQUIP_CATEGORY_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    private final NexusCosmetics plugin;
    private final CosmeticManager manager;
    private final EmoteService emotes;
    private final Messages messages;
    private final Player player;
    /** null = Hauptmenü */
    private final CosmeticSlot category;
    private final int page;
    private final Inventory inventory;
    private final Map<Integer, Cosmetic> cosmeticSlots = new HashMap<>();

    public CosmeticMenu(NexusCosmetics plugin, Player player) {
        this(plugin, player, null, 0);
    }

    private CosmeticMenu(NexusCosmetics plugin, Player player, CosmeticSlot category, int page) {
        this.plugin = plugin;
        this.manager = plugin.cosmetics();
        this.emotes = plugin.emotes();
        this.messages = plugin.messages();
        this.player = player;
        this.category = category;
        this.page = page;
        if (category == null) {
            this.inventory = Bukkit.createInventory(this, MAIN_SIZE, messages.get("menu.main-title"));
        } else {
            this.inventory = Bukkit.createInventory(this, PAGE_SIZE, messages.get("menu.category-title",
                    Placeholder.component("category", messages.get("menu.categories." + category.name())),
                    Placeholder.unparsed("page", String.valueOf(page + 1)),
                    Placeholder.unparsed("pages", String.valueOf(pageCount()))));
        }
        render();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private List<Cosmetic> categoryCosmetics() {
        return manager.registry().inSlot(category);
    }

    private int pageCount() {
        return Math.max(1, (categoryCosmetics().size() + ITEM_SLOTS.length - 1) / ITEM_SLOTS.length);
    }

    // ------------------------------------------------------------------ Aufbau

    private void render() {
        inventory.clear();
        cosmeticSlots.clear();
        if (category == null) {
            renderMain();
        } else {
            renderCategory();
        }
    }

    private void renderMain() {
        CATEGORY_SLOTS.forEach((slot, index) -> {
            List<Cosmetic> cosmetics = manager.registry().inSlot(slot);
            // Symbol der Kategorie: das erste Cosmetic darin (bzw. eine Truhe, wenn sie leer ist)
            ItemStack icon = cosmetics.isEmpty() ? ItemStack.of(Material.CHEST) : manager.createItem(cosmetics.getFirst(), List.of());
            icon.editMeta(meta -> {
                meta.itemName(messages.item("menu.categories." + slot.name()));
                meta.lore(List.of(
                        messages.item("menu.category-lore", Placeholder.unparsed("count", String.valueOf(cosmetics.size()))),
                        messages.item("menu.open-category")));
            });
            inventory.setItem(index, icon);
        });

        inventory.setItem(EMOTES_SLOT, button(Material.NOTE_BLOCK, "menu.emotes-button", messages.item("menu.emotes-button-lore")));
        int keys = manager.profile(player).allKeys().values().stream().mapToInt(Integer::intValue).sum();
        ItemStack crates = button(Material.PAPER, "crates.button", messages.item("crates.button-lore"),
                messages.item("crates.keys", Placeholder.unparsed("count", String.valueOf(keys))));
        crates.editMeta(meta -> meta.setItemModel(new NamespacedKey(CosmeticRegistry.NAMESPACE, "crate")));
        inventory.setItem(CRATES_SLOT, crates);
        inventory.setItem(UNEQUIP_ALL_SLOT, button(Material.BARRIER, "menu.unequip-all"));
    }

    private void renderCategory() {
        List<Cosmetic> cosmetics = categoryCosmetics();
        Cosmetic equipped = manager.equipped(player, category);
        int first = page * ITEM_SLOTS.length;
        for (int i = 0; i < ITEM_SLOTS.length && first + i < cosmetics.size(); i++) {
            Cosmetic cosmetic = cosmetics.get(first + i);
            List<Component> lore = new ArrayList<>();
            lore.add(messages.item("rarities." + cosmetic.rarity().name()));
            if (cosmetic.equals(equipped)) {
                lore.add(messages.item("menu.equipped"));
                lore.add(messages.item("menu.click-unequip"));
            } else if (manager.canUse(player, cosmetic)) {
                lore.add(messages.item("menu.click-equip"));
            } else {
                lore.add(messages.item("menu.locked"));
            }
            inventory.setItem(ITEM_SLOTS[i], manager.createItem(cosmetic, lore));
            cosmeticSlots.put(ITEM_SLOTS[i], cosmetic);
        }

        inventory.setItem(BACK_SLOT, button(Material.ARROW, "menu.back"));
        inventory.setItem(UNEQUIP_CATEGORY_SLOT, button(Material.BARRIER, "menu.unequip-category"));
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, button(Material.SPECTRAL_ARROW, "menu.previous-page"));
        }
        if (page + 1 < pageCount()) {
            inventory.setItem(NEXT_SLOT, button(Material.SPECTRAL_ARROW, "menu.next-page"));
        }
    }

    private ItemStack button(Material material, String nameKey, Component... lore) {
        ItemStack item = ItemStack.of(material);
        item.editMeta(meta -> {
            meta.itemName(messages.item(nameKey));
            meta.lore(List.of(lore));
            meta.addItemFlags(ItemFlag.values());
        });
        return item;
    }

    // ------------------------------------------------------------------ Klicks

    @Override
    public void handleClick(int slot) {
        if (category == null) {
            handleMainClick(slot);
        } else {
            handleCategoryClick(slot);
        }
    }

    private void handleMainClick(int slot) {
        for (Map.Entry<CosmeticSlot, Integer> entry : CATEGORY_SLOTS.entrySet()) {
            if (entry.getValue() == slot) {
                click();
                openPage(entry.getKey(), 0);
                return;
            }
        }
        if (slot == EMOTES_SLOT) {
            click();
            new EmoteMenu(emotes, messages, player).open();
        } else if (slot == CRATES_SLOT) {
            click();
            new CrateMenu(plugin.crates(), manager, messages, player).open();
        } else if (slot == UNEQUIP_ALL_SLOT) {
            manager.unequipAll(player);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
            player.sendMessage(messages.prefixed("cosmetics.unequipped-all"));
        }
    }

    private void handleCategoryClick(int slot) {
        switch (slot) {
            case BACK_SLOT -> {
                click();
                new CosmeticMenu(plugin, player).open();
                return;
            }
            case PREVIOUS_SLOT -> {
                if (page > 0) {
                    click();
                    openPage(category, page - 1);
                }
                return;
            }
            case NEXT_SLOT -> {
                if (page + 1 < pageCount()) {
                    click();
                    openPage(category, page + 1);
                }
                return;
            }
            case UNEQUIP_CATEGORY_SLOT -> {
                manager.unequip(player, category);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
                render();
                return;
            }
            default -> {
            }
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
                player.sendMessage(messages.prefixed("cosmetics.equipped", Placeholder.component("name", cosmetic.displayName())));
            }
            case NO_PERMISSION -> {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(messages.prefixed("cosmetics.locked"));
            }
            case HEAD_OCCUPIED -> {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(messages.prefixed("cosmetics.head-occupied"));
            }
        }
        render();
    }

    private void openPage(CosmeticSlot slot, int newPage) {
        new CosmeticMenu(plugin, player, slot, newPage).open();
    }

    private void click() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }
}
