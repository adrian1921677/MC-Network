package dev.nexus.cosmetics.cosmetic;

import dev.nexus.cosmetics.render.FakeCosmeticRenderer;
import dev.nexus.cosmetics.render.cape.CapeCosmetic;
import dev.nexus.cosmetics.render.hat.FloatingHalo;
import dev.nexus.cosmetics.render.hat.TalkingHat;
import dev.nexus.cosmetics.render.pet.FakePet;
import dev.nexus.cosmetics.storage.CosmeticStorage;
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
 * - Capes (BACK) und Haustiere (PET) existieren nur als Netzwerk-Pakete (siehe FakeCosmeticRenderer).
 */
public final class CosmeticManager {

    public enum EquipResult { EQUIPPED, NO_PERMISSION, HEAD_OCCUPIED }

    private final Plugin plugin;
    private final CosmeticRegistry registry;
    private final FakeCosmeticRenderer renderer;
    private final CosmeticStorage storage;
    /** Markierung, an der wir unsere Cosmetic-Items erkennen. */
    private final NamespacedKey cosmeticKey;
    private final Map<UUID, Map<CosmeticSlot, Cosmetic>> equipped = new HashMap<>();

    public CosmeticManager(Plugin plugin, CosmeticRegistry registry, FakeCosmeticRenderer renderer, CosmeticStorage storage) {
        this.plugin = plugin;
        this.registry = registry;
        this.renderer = renderer;
        this.storage = storage;
        this.cosmeticKey = new NamespacedKey(plugin, "cosmetic");
    }

    public CosmeticRegistry registry() {
        return registry;
    }

    public Cosmetic equipped(Player player, CosmeticSlot slot) {
        Map<CosmeticSlot, Cosmetic> slots = equipped.get(player.getUniqueId());
        return slots == null ? null : slots.get(slot);
    }

    // ------------------------------------------------------------------ Aktionen des Spielers (werden gespeichert)

    public EquipResult equip(Player player, Cosmetic cosmetic) {
        EquipResult result = apply(player, cosmetic);
        if (result == EquipResult.EQUIPPED) {
            save(player);
        }
        return result;
    }

    public void unequip(Player player, CosmeticSlot slot) {
        remove(player, slot);
        save(player);
    }

    public void unequipAll(Player player) {
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            remove(player, slot);
        }
        save(player);
    }

    // ------------------------------------------------------------------ Einloggen / Ausloggen

    /** Lädt beim Einloggen die gespeicherten Cosmetics und legt sie wieder an. */
    public void handleJoin(Player player) {
        // Aufräumen, falls nach einem Absturz noch alte Cosmetic-Items im Inventar liegen
        removeCosmeticItems(player);

        storage.load(player.getUniqueId()).thenAccept(saved ->
                // Zurück auf den Server-Thread: Die Spielwelt darf nur von dort verändert werden
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    saved.forEach((slot, id) -> {
                        Cosmetic cosmetic = registry.get(id);
                        // Gelöschte Cosmetics oder entzogene Rechte werden still übersprungen
                        if (cosmetic != null && cosmetic.slot() == slot) {
                            apply(player, cosmetic);
                        }
                    });
                }));
    }

    /** Beim Ausloggen nur die Anzeige entfernen, die Auswahl bleibt gespeichert. */
    public void handleQuit(Player player) {
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            remove(player, slot);
        }
        equipped.remove(player.getUniqueId());
    }

    /** Beim Server-Stopp: Anzeige bei allen entfernen, ohne die Auswahl zu löschen. */
    public void shutdown() {
        for (UUID uuid : List.copyOf(equipped.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                handleQuit(player);
            }
        }
        equipped.clear();
    }

    /** Setzt den Hut nach einem Respawn erneut auf, falls er verloren ging. */
    public void restoreHat(Player player) {
        Cosmetic hat = equipped(player, CosmeticSlot.HEAD);
        ItemStack helmet = player.getInventory().getHelmet();
        if (hat != null && hat.animation() != CosmeticAnimation.HALO && (helmet == null || helmet.isEmpty())) {
            player.getInventory().setHelmet(createItem(hat, List.of()));
        }
    }

    // ------------------------------------------------------------------ Intern

    /** Legt ein Cosmetic an, ohne zu speichern. */
    private EquipResult apply(Player player, Cosmetic cosmetic) {
        if (!player.hasPermission(cosmetic.permission())) {
            return EquipResult.NO_PERMISSION;
        }
        switch (cosmetic.slot()) {
            case HEAD -> {
                if (cosmetic.animation() == CosmeticAnimation.HALO) {
                    // Der Heiligenschein schwebt als Paket-Entity, der Helm-Slot bleibt frei
                    removeCosmeticItems(player);
                    renderer.show(player, CosmeticSlot.HEAD, new FloatingHalo(player, cosmetic));
                } else {
                    ItemStack current = player.getInventory().getHelmet();
                    if (current != null && !current.isEmpty() && !isCosmeticItem(current)) {
                        return EquipResult.HEAD_OCCUPIED;
                    }
                    player.getInventory().setHelmet(createItem(cosmetic, List.of()));
                    if (cosmetic.animation() == CosmeticAnimation.TALKING) {
                        renderer.show(player, CosmeticSlot.HEAD, new TalkingHat(player, cosmetic, this::isCosmeticItem));
                    } else {
                        renderer.hide(player, CosmeticSlot.HEAD);
                    }
                }
            }
            case BACK -> renderer.show(player, CosmeticSlot.BACK, new CapeCosmetic(player, cosmetic));
            case PET -> renderer.show(player, CosmeticSlot.PET, new FakePet(player, cosmetic));
        }
        equipped.computeIfAbsent(player.getUniqueId(), uuid -> new EnumMap<>(CosmeticSlot.class))
                .put(cosmetic.slot(), cosmetic);
        return EquipResult.EQUIPPED;
    }

    /** Nimmt ein Cosmetic ab, ohne zu speichern. */
    private void remove(Player player, CosmeticSlot slot) {
        Map<CosmeticSlot, Cosmetic> slots = equipped.get(player.getUniqueId());
        if (slots != null) {
            slots.remove(slot);
        }
        switch (slot) {
            case HEAD -> {
                removeCosmeticItems(player);
                renderer.hide(player, slot);
            }
            case BACK, PET -> renderer.hide(player, slot);
        }
    }

    private void save(Player player) {
        Map<CosmeticSlot, String> ids = new EnumMap<>(CosmeticSlot.class);
        Map<CosmeticSlot, Cosmetic> slots = equipped.get(player.getUniqueId());
        if (slots != null) {
            slots.forEach((slot, cosmetic) -> ids.put(slot, cosmetic.id()));
        }
        storage.save(player.getUniqueId(), ids);
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
