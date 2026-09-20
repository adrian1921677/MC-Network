package dev.nexus.cosmetics.menu;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.config.Messages;
import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import dev.nexus.cosmetics.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gespeicherte Kombinationen: Hut, Rücken und Haustier zusammen unter einem Namen.
 *
 * Linksklick zieht ein Outfit an, Rechtsklick löscht es. Was gerade getragen wird, lässt sich
 * über einen Knopf als neues Outfit ablegen.
 */
public final class OutfitMenu implements ClickableMenu {

    private static final int SIZE = 45;
    private static final int[] OUTFIT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25};
    private static final int BACK_SLOT = 36;
    private static final int SAVE_SLOT = 40;

    private final NexusCosmetics plugin;
    private final CosmeticManager manager;
    private final Messages messages;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, String> outfitSlots = new HashMap<>();

    public OutfitMenu(NexusCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.manager = plugin.cosmetics();
        this.messages = plugin.messages();
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, messages.get("menu.outfits-title"));
        render();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // ------------------------------------------------------------------ Aufbau

    private void render() {
        inventory.clear();
        outfitSlots.clear();

        Map<String, Map<CosmeticSlot, String>> outfits = manager.profile(player).outfits();
        int index = 0;
        for (Map.Entry<String, Map<CosmeticSlot, String>> entry : outfits.entrySet()) {
            if (index >= OUTFIT_SLOTS.length) {
                break;
            }
            int slot = OUTFIT_SLOTS[index++];
            inventory.setItem(slot, outfitIcon(entry.getKey(), entry.getValue()));
            outfitSlots.put(slot, entry.getKey());
        }

        if (outfits.isEmpty()) {
            inventory.setItem(OUTFIT_SLOTS[3], button(Material.GLASS_PANE, "menu.outfits-empty",
                    messages.item("menu.outfits-empty-lore")));
        }

        inventory.setItem(BACK_SLOT, button(Material.ARROW, "menu.back"));
        boolean full = outfits.size() >= PlayerProfile.MAX_OUTFITS;
        inventory.setItem(SAVE_SLOT, button(full ? Material.BARRIER : Material.WRITABLE_BOOK, "menu.outfit-save",
                messages.item(full ? "menu.outfit-limit" : "menu.outfit-save-lore",
                        Placeholder.unparsed("max", String.valueOf(PlayerProfile.MAX_OUTFITS)))));
    }

    /** Als Symbol dient der Hut des Outfits, damit man es auf einen Blick erkennt. */
    private ItemStack outfitIcon(String name, Map<CosmeticSlot, String> pieces) {
        Cosmetic head = resolve(pieces.get(CosmeticSlot.HEAD));
        List<Component> lore = new ArrayList<>();
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            Cosmetic piece = resolve(pieces.get(slot));
            lore.add(messages.item("menu.outfit-piece",
                    Placeholder.component("category", messages.get("menu.categories." + slot.name())),
                    Placeholder.component("name", piece != null ? piece.displayName()
                            : messages.get("menu.outfit-piece-empty"))));
        }
        lore.add(messages.item("menu.outfit-apply-hint"));
        lore.add(messages.item("menu.outfit-delete-hint"));

        ItemStack icon = head != null ? manager.createItem(head, lore) : ItemStack.of(Material.ARMOR_STAND);
        icon.editMeta(meta -> {
            meta.itemName(messages.item("menu.outfit-name", Placeholder.unparsed("name", name)));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
        });
        return icon;
    }

    private Cosmetic resolve(String id) {
        return id == null ? null : manager.registry().get(id);
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
        if (slot == BACK_SLOT) {
            clickSound();
            new CosmeticMenu(plugin, player).open();
            return;
        }
        if (slot == SAVE_SLOT) {
            clickSound();
            askForName();
            return;
        }

        String name = outfitSlots.get(slot);
        if (name == null) {
            return;
        }
        if (click.isRightClick()) {
            delete(name);
        } else {
            apply(name);
        }
    }

    /**
     * Zieht das Outfit an. Teile, die es nicht mehr gibt oder für die das Recht fehlt, werden
     * übersprungen statt das ganze Outfit scheitern zu lassen.
     */
    private void apply(String name) {
        Map<CosmeticSlot, String> pieces = manager.profile(player).outfits().get(name);
        if (pieces == null) {
            return;
        }
        int applied = 0;
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            Cosmetic cosmetic = resolve(pieces.get(slot));
            if (cosmetic == null) {
                manager.unequip(player, slot);
                continue;
            }
            if (manager.equip(player, cosmetic) == CosmeticManager.EquipResult.EQUIPPED) {
                applied++;
            }
        }
        plugin.preview().refresh(player);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1.2f);
        player.sendMessage(messages.prefixed("menu.outfit-applied",
                Placeholder.unparsed("name", name),
                Placeholder.unparsed("count", String.valueOf(applied))));
        render();
    }

    private void delete(String name) {
        manager.profile(player).outfits().remove(name);
        manager.save(player);
        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.4f);
        player.sendMessage(messages.prefixed("menu.outfit-deleted", Placeholder.unparsed("name", name)));
        render();
    }

    private void askForName() {
        PlayerProfile profile = manager.profile(player);
        if (profile.outfits().size() >= PlayerProfile.MAX_OUTFITS) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(messages.prefixed("menu.outfit-limit-reached",
                    Placeholder.unparsed("max", String.valueOf(PlayerProfile.MAX_OUTFITS))));
            return;
        }
        plugin.chatInput().ask(player, "menu.outfit-name-prompt", this::save,
                () -> new OutfitMenu(plugin, player).open());
    }

    private void save(String rawName) {
        String name = cleanName(rawName);
        if (name.isEmpty()) {
            player.sendMessage(messages.prefixed("menu.outfit-bad-name"));
            new OutfitMenu(plugin, player).open();
            return;
        }
        Map<CosmeticSlot, String> pieces = new EnumMap<>(CosmeticSlot.class);
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            Cosmetic worn = manager.equipped(player, slot);
            if (worn != null) {
                pieces.put(slot, worn.id());
            }
        }
        if (pieces.isEmpty()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(messages.prefixed("menu.outfit-nothing-worn"));
            new OutfitMenu(plugin, player).open();
            return;
        }
        if (manager.profile(player).saveOutfit(name, pieces)) {
            manager.save(player);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
            player.sendMessage(messages.prefixed("menu.outfit-saved", Placeholder.unparsed("name", name)));
        } else {
            player.sendMessage(messages.prefixed("menu.outfit-limit-reached",
                    Placeholder.unparsed("max", String.valueOf(PlayerProfile.MAX_OUTFITS))));
        }
        new OutfitMenu(plugin, player).open();
    }

    /**
     * Punkte würden den Pfad in der Spielerdatei zerlegen (outfits.<name>.<slot>) und das Outfit
     * unlesbar machen; das Paragraphenzeichen liesse Farbcodes durch. Beides fliegt raus.
     */
    private String cleanName(String rawName) {
        String name = rawName.replace('.', ' ').replace('§', ' ').trim();
        return name.length() > 24 ? name.substring(0, 24).trim() : name;
    }

    private void clickSound() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }
}
