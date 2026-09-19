package dev.nexus.cosmetics.cosmetic;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;

/**
 * Beschreibt ein einzelnes Cosmetic (aus cosmetics.yml).
 *
 * @param id                eindeutiger Name, z. B. "top_hat"
 * @param displayName       Name, den Spieler sehen
 * @param slot              wo das Cosmetic getragen wird
 * @param model             das 3D-Modell im Resource Pack, z. B. nexus:top_hat
 * @param glowing           leuchtet auch im Dunkeln (Capes, Haustiere, Heiligenschein)
 * @param animation         besonderes Verhalten (Haustier-Art, Umhang, sprechender Hut ...)
 * @param unlockedByDefault true = jeder darf es benutzen, sonst nur mit Permission
 */
public record Cosmetic(String id, Component displayName, CosmeticSlot slot, NamespacedKey model,
                       boolean glowing, CosmeticAnimation animation, boolean unlockedByDefault) {

    /** Permission, die ein Spieler braucht, um dieses Cosmetic zu benutzen. */
    public String permission() {
        return "nexuscosmetics.cosmetic." + id;
    }

    /** Ein zusätzliches Modell mit Endung, z. B. nexus:mini_dragon_wing_a */
    public NamespacedKey model(String suffix) {
        return new NamespacedKey(model.getNamespace(), model.getKey() + "_" + suffix);
    }
}
