package dev.nexus.cosmetics.cosmetic;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;

/**
 * Beschreibt ein einzelnes Cosmetic.
 *
 * @param id          eindeutiger Name, z. B. "top_hat"
 * @param displayName Name, den Spieler sehen
 * @param slot        wo das Cosmetic getragen wird
 * @param model       das 3D-Modell im Resource Pack, z. B. nexus:top_hat
 * @param glowing     leuchtet auch im Dunkeln (Capes und Haustiere)
 * @param animation   Bewegungsart (nur für Haustiere)
 */
public record Cosmetic(String id, Component displayName, CosmeticSlot slot, NamespacedKey model,
                       boolean glowing, CosmeticAnimation animation) {

    /** Permission, die ein Spieler braucht, um dieses Cosmetic zu benutzen. */
    public String permission() {
        return "nexuscosmetics.cosmetic." + id;
    }

    /** Ein zusätzliches Modell mit Endung, z. B. nexus:mini_dragon_wing_a */
    public NamespacedKey model(String suffix) {
        return new NamespacedKey(model.getNamespace(), model.getKey() + "_" + suffix);
    }
}
