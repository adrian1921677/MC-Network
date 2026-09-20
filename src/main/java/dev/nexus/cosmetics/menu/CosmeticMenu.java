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
import dev.nexus.cosmetics.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Das Cosmetics-Menü.
 *
 * - Hauptmenü: Kategorien (Hüte, Umhänge, Haustiere), Outfits, Emotes, Truhen und "Alles ablegen".
 * - Kategorie-Seite: bis zu 28 Cosmetics pro Seite (als 3D-Modell), dazu Suche, Sortierung und
 *   ein Favoriten-Filter. Bei 174 Cosmetics wäre reines Blättern sonst mühsam.
 *
 * Linksklick legt an, Rechtsklick setzt oder entfernt einen Favoriten.
 */
public final class CosmeticMenu implements ClickableMenu {

    /** Reihenfolge, in der die Cosmetics einer Kategorie angezeigt werden. */
    public enum Sort {
        STANDARD, RARITY, NAME, UNLOCKED;

        Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Was der Spieler auf der Kategorie-Seite gerade eingestellt hat. */
    public record View(String search, Sort sort, boolean favoritesOnly) {

        public static final View DEFAULT = new View("", Sort.STANDARD, false);

        View withSearch(String term) {
            return new View(term, sort, favoritesOnly);
        }

        View withSort(Sort newSort) {
            return new View(search, newSort, favoritesOnly);
        }

        View toggleFavorites() {
            return new View(search, sort, !favoritesOnly);
        }

        boolean searching() {
            return !search.isBlank();
        }
    }

    // Hauptmenü
    private static final int MAIN_SIZE = 27;
    private static final Map<CosmeticSlot, Integer> CATEGORY_SLOTS = Map.of(
            CosmeticSlot.HEAD, 10,
            CosmeticSlot.BACK, 11,
            CosmeticSlot.PET, 12);
    private static final int EMOTES_SLOT = 14;
    private static final int OUTFITS_SLOT = 15;
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
    private static final int SEARCH_SLOT = 46;
    private static final int SORT_SLOT = 47;
    private static final int PREVIOUS_SLOT = 48;
    private static final int UNEQUIP_CATEGORY_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int FAVORITES_SLOT = 51;
    private static final int RESET_SLOT = 52;

    private final NexusCosmetics plugin;
    private final CosmeticManager manager;
    private final EmoteService emotes;
    private final Messages messages;
    private final Player player;
    /** null = Hauptmenü */
    private final CosmeticSlot category;
    private final int page;
    private final View view;
    private final Inventory inventory;
    private final Map<Integer, Cosmetic> cosmeticSlots = new HashMap<>();
    private final List<Cosmetic> visible;

    public CosmeticMenu(NexusCosmetics plugin, Player player) {
        this(plugin, player, null, 0, View.DEFAULT);
    }

    private CosmeticMenu(NexusCosmetics plugin, Player player, CosmeticSlot category, int page, View view) {
        this.plugin = plugin;
        this.manager = plugin.cosmetics();
        this.emotes = plugin.emotes();
        this.messages = plugin.messages();
        this.player = player;
        this.category = category;
        this.view = view;
        this.visible = category == null ? List.of() : filterAndSort();
        this.page = Math.max(0, Math.min(page, pageCount() - 1));
        if (category == null) {
            this.inventory = Bukkit.createInventory(this, MAIN_SIZE, messages.get("menu.main-title"));
        } else {
            this.inventory = Bukkit.createInventory(this, PAGE_SIZE, messages.get("menu.category-title",
                    Placeholder.component("category", messages.get("menu.categories." + category.name())),
                    Placeholder.unparsed("page", String.valueOf(this.page + 1)),
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

    // ------------------------------------------------------------------ Filtern und Sortieren

    /**
     * Wendet Suche, Favoriten-Filter und Sortierung an.
     *
     * Favoriten stehen immer ganz vorne - wer sich etwas angeheftet hat, will es sofort sehen,
     * egal welche Sortierung eingestellt ist.
     */
    private List<Cosmetic> filterAndSort() {
        PlayerProfile profile = manager.profile(player);
        List<Cosmetic> all = manager.registry().inSlot(category);
        String needle = view.search().toLowerCase(Locale.ROOT);
        List<Cosmetic> result = new ArrayList<>();
        for (Cosmetic cosmetic : all) {
            if (view.favoritesOnly() && !profile.isFavorite(cosmetic.id())) {
                continue;
            }
            if (!needle.isEmpty() && !matches(cosmetic, needle)) {
                continue;
            }
            result.add(cosmetic);
        }

        Comparator<Cosmetic> order = switch (view.sort()) {
            case STANDARD -> Comparator.comparingInt(all::indexOf);
            // Seltenstes zuerst
            case RARITY -> Comparator.comparingInt((Cosmetic c) -> -c.rarity().ordinal()).thenComparing(this::plainName);
            case NAME -> Comparator.comparing(this::plainName);
            // Freigeschaltetes zuerst
            case UNLOCKED -> Comparator.comparingInt((Cosmetic c) -> manager.canUse(player, c) ? 0 : 1)
                    .thenComparing(this::plainName);
        };
        result.sort(Comparator.comparingInt((Cosmetic c) -> profile.isFavorite(c.id()) ? 0 : 1).thenComparing(order));
        return result;
    }

    private boolean matches(Cosmetic cosmetic, String needle) {
        return cosmetic.id().toLowerCase(Locale.ROOT).contains(needle)
                || plainName(cosmetic).toLowerCase(Locale.ROOT).contains(needle);
    }

    private String plainName(Cosmetic cosmetic) {
        return PlainTextComponentSerializer.plainText().serialize(cosmetic.displayName());
    }

    private int pageCount() {
        return Math.max(1, (visible.size() + ITEM_SLOTS.length - 1) / ITEM_SLOTS.length);
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
        int outfitCount = manager.profile(player).outfits().size();
        inventory.setItem(OUTFITS_SLOT, button(Material.ARMOR_STAND, "menu.outfits-button",
                messages.item("menu.outfits-button-lore",
                        Placeholder.unparsed("count", String.valueOf(outfitCount)),
                        Placeholder.unparsed("max", String.valueOf(PlayerProfile.MAX_OUTFITS)))));
        int keys = manager.profile(player).allKeys().values().stream().mapToInt(Integer::intValue).sum();
        ItemStack crates = button(Material.PAPER, "crates.button", messages.item("crates.button-lore"),
                messages.item("crates.keys", Placeholder.unparsed("count", String.valueOf(keys))));
        crates.editMeta(meta -> meta.setItemModel(new NamespacedKey(CosmeticRegistry.NAMESPACE, "crate")));
        inventory.setItem(CRATES_SLOT, crates);
        inventory.setItem(UNEQUIP_ALL_SLOT, button(Material.BARRIER, "menu.unequip-all"));
    }

    private void renderCategory() {
        PlayerProfile profile = manager.profile(player);
        Cosmetic equipped = manager.equipped(player, category);
        int first = page * ITEM_SLOTS.length;
        for (int i = 0; i < ITEM_SLOTS.length && first + i < visible.size(); i++) {
            Cosmetic cosmetic = visible.get(first + i);
            List<Component> lore = new ArrayList<>();
            lore.add(messages.item("rarities." + cosmetic.rarity().name()));
            if (profile.isFavorite(cosmetic.id())) {
                lore.add(messages.item("menu.favorite-marked"));
            }
            if (cosmetic.equals(equipped)) {
                lore.add(messages.item("menu.equipped"));
                lore.add(messages.item("menu.click-unequip"));
            } else if (manager.canUse(player, cosmetic)) {
                lore.add(messages.item("menu.click-equip"));
            } else {
                lore.add(messages.item("menu.locked"));
            }
            lore.add(messages.item("menu.favorite-hint"));
            inventory.setItem(ITEM_SLOTS[i], manager.createItem(cosmetic, lore));
            cosmeticSlots.put(ITEM_SLOTS[i], cosmetic);
        }

        if (visible.isEmpty()) {
            inventory.setItem(ITEM_SLOTS[10], button(Material.GLASS_PANE, "menu.no-results",
                    messages.item("menu.no-results-lore")));
        }

        inventory.setItem(BACK_SLOT, button(Material.ARROW, "menu.back"));
        inventory.setItem(SEARCH_SLOT, button(Material.COMPASS, "menu.search",
                view.searching()
                        ? messages.item("menu.search-active", Placeholder.unparsed("term", view.search()))
                        : messages.item("menu.search-lore")));
        inventory.setItem(SORT_SLOT, button(Material.HOPPER, "menu.sort",
                messages.item("menu.sort-active",
                        Placeholder.component("mode", messages.get("menu.sort-modes." + view.sort().name()))),
                messages.item("menu.sort-lore")));
        inventory.setItem(FAVORITES_SLOT, button(view.favoritesOnly() ? Material.NETHER_STAR : Material.FIREWORK_STAR,
                "menu.favorites-filter",
                messages.item(view.favoritesOnly() ? "menu.favorites-filter-on" : "menu.favorites-filter-off")));
        if (view.searching() || view.favoritesOnly() || view.sort() != Sort.STANDARD) {
            inventory.setItem(RESET_SLOT, button(Material.STRUCTURE_VOID, "menu.reset-filters"));
        }
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
        handleClick(slot, ClickType.LEFT);
    }

    @Override
    public void handleClick(int slot, ClickType click) {
        if (category == null) {
            handleMainClick(slot);
        } else {
            handleCategoryClick(slot, click);
        }
    }

    private void handleMainClick(int slot) {
        for (Map.Entry<CosmeticSlot, Integer> entry : CATEGORY_SLOTS.entrySet()) {
            if (entry.getValue() == slot) {
                clickSound();
                openPage(entry.getKey(), 0, View.DEFAULT);
                return;
            }
        }
        if (slot == EMOTES_SLOT) {
            clickSound();
            new EmoteMenu(emotes, messages, player).open();
        } else if (slot == OUTFITS_SLOT) {
            clickSound();
            new OutfitMenu(plugin, player).open();
        } else if (slot == CRATES_SLOT) {
            clickSound();
            new CrateMenu(plugin.crates(), manager, messages, player).open();
        } else if (slot == UNEQUIP_ALL_SLOT) {
            manager.unequipAll(player);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
            player.sendMessage(messages.prefixed("cosmetics.unequipped-all"));
        }
    }

    private void handleCategoryClick(int slot, ClickType click) {
        switch (slot) {
            case BACK_SLOT -> {
                clickSound();
                new CosmeticMenu(plugin, player).open();
                return;
            }
            case PREVIOUS_SLOT -> {
                if (page > 0) {
                    clickSound();
                    openPage(category, page - 1, view);
                }
                return;
            }
            case NEXT_SLOT -> {
                if (page + 1 < pageCount()) {
                    clickSound();
                    openPage(category, page + 1, view);
                }
                return;
            }
            case SEARCH_SLOT -> {
                clickSound();
                askForSearch();
                return;
            }
            case SORT_SLOT -> {
                clickSound();
                openPage(category, 0, view.withSort(view.sort().next()));
                return;
            }
            case FAVORITES_SLOT -> {
                clickSound();
                openPage(category, 0, view.toggleFavorites());
                return;
            }
            case RESET_SLOT -> {
                clickSound();
                openPage(category, 0, View.DEFAULT);
                return;
            }
            case UNEQUIP_CATEGORY_SLOT -> {
                manager.unequip(player, category);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
                reopen();
                return;
            }
            default -> {
            }
        }

        Cosmetic cosmetic = cosmeticSlots.get(slot);
        if (cosmetic == null) {
            return;
        }

        if (click.isRightClick()) {
            toggleFavorite(cosmetic);
            return;
        }

        // Nochmal auf ein getragenes Cosmetic klicken = ablegen
        if (cosmetic.equals(manager.equipped(player, cosmetic.slot()))) {
            manager.unequip(player, cosmetic.slot());
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
            reopen();
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
        reopen();
    }

    private void toggleFavorite(Cosmetic cosmetic) {
        boolean nowFavorite = manager.profile(player).toggleFavorite(cosmetic.id());
        manager.save(player);
        player.playSound(player.getLocation(), nowFavorite ? Sound.ENTITY_PLAYER_LEVELUP : Sound.UI_BUTTON_CLICK,
                0.6f, nowFavorite ? 1.8f : 1.0f);
        player.sendMessage(messages.prefixed(nowFavorite ? "menu.favorite-added" : "menu.favorite-removed",
                Placeholder.component("name", cosmetic.displayName())));
        reopen();
    }

    private void askForSearch() {
        plugin.chatInput().ask(player, "menu.search-prompt",
                term -> openPage(category, 0, view.withSearch(term)),
                () -> openPage(category, page, view));
    }

    /** Baut die Seite neu auf, damit sich geänderte Favoriten sofort in der Sortierung zeigen. */
    private void reopen() {
        openPage(category, page, view);
    }

    private void openPage(CosmeticSlot slot, int newPage, View newView) {
        new CosmeticMenu(plugin, player, slot, newPage, newView).open();
    }

    private void clickSound() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }
}
