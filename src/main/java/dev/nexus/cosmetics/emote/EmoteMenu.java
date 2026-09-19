package dev.nexus.cosmetics.emote;

import dev.nexus.cosmetics.config.Messages;
import dev.nexus.cosmetics.menu.ClickableMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Emote-Menü: oben die Emojis (als 3D-Modell), darunter die Posen.
 * Klick spielt das Emote ab und schließt das Menü.
 */
public final class EmoteMenu implements ClickableMenu {

    private static final int SIZE = 36;
    private static final int EMOJI_ROW = 10;
    private static final int POSE_ROW = 19;

    private final EmoteService service;
    private final Messages messages;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, Emote> slots = new HashMap<>();

    public EmoteMenu(EmoteService service, Messages messages, Player player) {
        this.service = service;
        this.messages = messages;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, messages.get("emotes.menu-title"));
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
        int emojiSlot = EMOJI_ROW;
        int poseSlot = POSE_ROW;
        for (Emote emote : service.registry().all()) {
            int slot = emote.type().isEmoji() ? emojiSlot++ : poseSlot++;
            inventory.setItem(slot, icon(emote));
            slots.put(slot, emote);
        }

        ItemStack hint = ItemStack.of(Material.BOOK);
        hint.editMeta(meta -> {
            meta.itemName(messages.item("emotes.tip-title"));
            meta.lore(messages.itemList("emotes.tip-lines"));
        });
        inventory.setItem(31, hint);
    }

    private ItemStack icon(Emote emote) {
        ItemStack item = ItemStack.of(emote.type().isEmoji() ? Material.PAPER : emote.type().icon());
        boolean unlocked = service.canUse(player, emote);
        item.editMeta(meta -> {
            if (emote.type().isEmoji()) {
                meta.setItemModel(emote.model());
            }
            meta.itemName(emote.displayName());
            meta.lore(List.of(messages.item(unlocked ? "emotes.click-play" : "emotes.locked-lore")));
            meta.addItemFlags(ItemFlag.values());
        });
        return item;
    }

    @Override
    public void handleClick(int slot) {
        Emote emote = slots.get(slot);
        if (emote == null) {
            return;
        }
        player.closeInventory();
        service.play(player, emote);
    }
}
