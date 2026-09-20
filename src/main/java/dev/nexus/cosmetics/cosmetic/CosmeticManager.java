package dev.nexus.cosmetics.cosmetic;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.render.FakeCosmeticRenderer;
import dev.nexus.cosmetics.render.cape.BackCosmetic;
import dev.nexus.cosmetics.render.hat.FloatingHalo;
import dev.nexus.cosmetics.render.hat.HeadAura;
import dev.nexus.cosmetics.render.hat.TalkingHat;
import dev.nexus.cosmetics.render.pet.FakePet;
import dev.nexus.cosmetics.storage.CosmeticStorage;
import dev.nexus.cosmetics.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Verwaltet, welcher Spieler welches Cosmetic trägt und besitzt.
 *
 * - Hüte (HEAD) liegen im Helm-Slot und sind dort geschützt. Ideal für Lobbys ohne Rüstung.
 * - Capes (BACK) und Haustiere (PET) existieren nur als Netzwerk-Pakete (siehe FakeCosmeticRenderer).
 * - Pro Spieler gibt es ein Profil (getragen, Besitz, Truhen-Schlüssel), das gespeichert wird.
 */
public final class CosmeticManager {

    public enum EquipResult { EQUIPPED, NO_PERMISSION, HEAD_OCCUPIED }

    private final NexusCosmetics plugin;
    private final CosmeticRegistry registry;
    private final FakeCosmeticRenderer renderer;
    private final CosmeticStorage storage;
    /** Markierung, an der wir unsere Cosmetic-Items erkennen. */
    private final NamespacedKey cosmeticKey;
    private final Map<UUID, Map<CosmeticSlot, Cosmetic>> equipped = new HashMap<>();
    /** Nur fertig geladene Profile. Solange ein Profil lädt, steht hier nichts. */
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    /** Änderungen, die ankommen, während das Profil noch lädt. Sie werden danach angewendet. */
    private final Map<UUID, List<Consumer<PlayerProfile>>> pendingChanges = new HashMap<>();

    public CosmeticManager(NexusCosmetics plugin, CosmeticRegistry registry, FakeCosmeticRenderer renderer, CosmeticStorage storage) {
        this.plugin = plugin;
        this.registry = registry;
        this.renderer = renderer;
        this.storage = storage;
        this.cosmeticKey = new NamespacedKey(plugin, "cosmetic");
        // Im Netzwerk: Hat ein anderer Server etwas am Profil geändert, holen wir es nach.
        storage.onExternalChange(this::reloadFromOtherServer);
    }

    public CosmeticRegistry registry() {
        return registry;
    }

    public Cosmetic equipped(Player player, CosmeticSlot slot) {
        Map<CosmeticSlot, Cosmetic> slots = equipped.get(player.getUniqueId());
        return slots == null ? null : slots.get(slot);
    }

    // ------------------------------------------------------------------ Besitz & Schlüssel

    /** Das Profil eines Online-Spielers (leer, solange es noch lädt). */
    public PlayerProfile profile(Player player) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        return profile != null ? profile : new PlayerProfile();
    }

    /** Darf der Spieler das Cosmetic benutzen? (Permission oder gewonnen/geschenkt) */
    public boolean canUse(Player player, Cosmetic cosmetic) {
        return player.hasPermission(cosmetic.permission()) || owns(player, cosmetic.ownershipKey());
    }

    public boolean owns(Player player, String ownershipKey) {
        return profile(player).owned().contains(ownershipKey);
    }

    /**
     * Ändert das Profil eines Spielers und speichert es. Funktioniert auch, wenn der Spieler
     * offline ist (z. B. wenn ein Shop Schlüssel vergibt).
     */
    public void modifyProfile(UUID uuid, Consumer<PlayerProfile> change) {
        Player online = Bukkit.getPlayer(uuid);
        if (online == null) {
            storage.modify(uuid, change);
        } else if (profiles.containsKey(uuid)) {
            change.accept(profiles.get(uuid));
            save(online);
        } else {
            // Profil lädt gerade noch: merken und nach dem Laden anwenden (sonst ginge die Änderung verloren)
            pendingChanges.computeIfAbsent(uuid, key -> new ArrayList<>()).add(change);
        }
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

    /** Lädt beim Einloggen das Profil und legt die gespeicherten Cosmetics wieder an. */
    public void handleJoin(Player player) {
        // Aufräumen, falls nach einem Absturz noch alte Cosmetic-Items im Inventar liegen
        removeCosmeticItems(player);

        storage.load(player.getUniqueId()).thenAccept(saved ->
                // Zurück auf den Server-Thread: Die Spielwelt darf nur von dort verändert werden
                Bukkit.getScheduler().runTask(plugin, () -> {
                    List<Consumer<PlayerProfile>> pending = pendingChanges.remove(player.getUniqueId());
                    if (!player.isOnline()) {
                        if (pending != null) {
                            pending.forEach(change -> storage.modify(player.getUniqueId(), change));
                        }
                        return;
                    }
                    profiles.put(player.getUniqueId(), saved);
                    if (pending != null) {
                        pending.forEach(change -> change.accept(saved));
                        storage.save(player.getUniqueId(), saved);
                    }
                    saved.equipped().forEach((slot, id) -> {
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
        profiles.remove(player.getUniqueId());
        // Im Netzwerk das Profil freigeben, damit der nächste Server es sofort übernehmen kann
        storage.release(player.getUniqueId());
    }

    /**
     * Ein anderer Server im Netzwerk hat das Profil verändert (z. B. ein Shop hat Schlüssel
     * vergeben). Besitz und Schlüssel werden übernommen.
     *
     * Was der Spieler hier gerade trägt, bleibt unangetastet — dafür ist dieser Server zuständig,
     * und ein Überschreiben würde ihm das Cosmetic mitten im Spiel vom Kopf nehmen.
     */
    private void reloadFromOtherServer(UUID uuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!profiles.containsKey(uuid)) {
                return;
            }
            storage.load(uuid).thenAccept(fresh -> Bukkit.getScheduler().runTask(plugin, () -> {
                PlayerProfile current = profiles.get(uuid);
                if (current == null) {
                    return;
                }
                current.owned().clear();
                current.owned().addAll(fresh.owned());
                current.allKeys().clear();
                current.allKeys().putAll(fresh.allKeys());
            }));
        });
    }

    /** Beim Server-Stopp oder Reload: Anzeige bei allen entfernen, ohne die Auswahl zu löschen. */
    public void shutdown() {
        for (UUID uuid : List.copyOf(profiles.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                handleQuit(player);
            }
        }
        equipped.clear();
        profiles.clear();
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
        if (!canUse(player, cosmetic)) {
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
                        renderer.show(player, CosmeticSlot.HEAD,
                                new TalkingHat(player, cosmetic, this::isCosmeticItem, plugin.messages(), plugin.settings()));
                    } else if (cosmetic.aura() != null) {
                        renderer.show(player, CosmeticSlot.HEAD, new HeadAura(player, cosmetic.aura()));
                    } else {
                        renderer.hide(player, CosmeticSlot.HEAD);
                    }
                }
            }
            case BACK -> renderer.show(player, CosmeticSlot.BACK, switch (cosmetic.animation()) {
                case WINGS -> BackCosmetic.wings(player, cosmetic);
                case BACKPACK -> BackCosmetic.backItem(player, cosmetic);
                default -> BackCosmetic.cape(player, cosmetic);
            });
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

    /** Speichert das Profil (inklusive der gerade getragenen Cosmetics). */
    public void save(Player player) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) {
            return; // Noch nicht geladen: niemals ein leeres Profil über das echte schreiben
        }
        profile.equipped().clear();
        Map<CosmeticSlot, Cosmetic> slots = equipped.get(player.getUniqueId());
        if (slots != null) {
            slots.forEach((slot, cosmetic) -> profile.equipped().put(slot, cosmetic.id()));
        }
        storage.save(player.getUniqueId(), profile);
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
}
