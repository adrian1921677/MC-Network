package dev.nexus.cosmetics.cosmetic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liste aller verfügbaren Cosmetics. Neue Cosmetics werden hier eingetragen.
 */
public final class CosmeticRegistry {

    /** Namespace im Resource Pack (Ordner assets/nexus/...). */
    public static final String NAMESPACE = "nexus";

    private final Map<String, Cosmetic> cosmetics = new LinkedHashMap<>();

    public CosmeticRegistry() {
        // Hüte
        register("top_hat", Component.text("Zylinder", NamedTextColor.GRAY), CosmeticSlot.HEAD, false);
        register("crown", Component.text("Krone", NamedTextColor.GOLD), CosmeticSlot.HEAD, false);

        // Capes
        register("royal_cape", Component.text("Königsumhang", NamedTextColor.RED), CosmeticSlot.BACK, false);
        register("galaxy_cape", Component.text("Galaxie-Cape", TextColor.color(0xA070FF)), CosmeticSlot.BACK, true);
    }

    private void register(String id, Component name, CosmeticSlot slot, boolean glowing) {
        cosmetics.put(id, new Cosmetic(id, name, slot, new NamespacedKey(NAMESPACE, id), glowing));
    }

    public Cosmetic get(String id) {
        return cosmetics.get(id);
    }

    public Collection<Cosmetic> all() {
        return cosmetics.values();
    }
}
