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
        register("top_hat", Component.text("Zylinder", NamedTextColor.GRAY), CosmeticSlot.HEAD, false, CosmeticAnimation.NONE);
        register("crown", Component.text("Krone", NamedTextColor.GOLD), CosmeticSlot.HEAD, false, CosmeticAnimation.NONE);
        register("talking_hat", Component.text("Sprechender Hut", TextColor.color(0xB08050)), CosmeticSlot.HEAD, false, CosmeticAnimation.TALKING);
        register("halo", Component.text("Heiligenschein", TextColor.color(0xFFE27A)), CosmeticSlot.HEAD, true, CosmeticAnimation.HALO);

        // Capes
        register("royal_cape", Component.text("Königsumhang", NamedTextColor.RED), CosmeticSlot.BACK, false, CosmeticAnimation.NONE);
        register("galaxy_cape", Component.text("Galaxie-Cape", TextColor.color(0xA070FF)), CosmeticSlot.BACK, true, CosmeticAnimation.NONE);
        register("wizard_robe_red", Component.text("Zauberumhang (Rot)", TextColor.color(0xD8413A)), CosmeticSlot.BACK, false, CosmeticAnimation.ROBE);
        register("wizard_robe_green", Component.text("Zauberumhang (Grün)", TextColor.color(0x3FAF5A)), CosmeticSlot.BACK, false, CosmeticAnimation.ROBE);
        register("wizard_robe_blue", Component.text("Zauberumhang (Blau)", TextColor.color(0x4C7FE0)), CosmeticSlot.BACK, false, CosmeticAnimation.ROBE);
        register("wizard_robe_yellow", Component.text("Zauberumhang (Gelb)", TextColor.color(0xF0C23A)), CosmeticSlot.BACK, false, CosmeticAnimation.ROBE);

        // Haustiere
        register("mini_dragon", Component.text("Mini-Drache", TextColor.color(0xE0533A)), CosmeticSlot.PET, false, CosmeticAnimation.DRAGON);
        register("ghost", Component.text("Geist", NamedTextColor.WHITE), CosmeticSlot.PET, true, CosmeticAnimation.GHOST);
        register("penguin", Component.text("Pinguin", TextColor.color(0x9FC7FF)), CosmeticSlot.PET, false, CosmeticAnimation.PENGUIN);
        register("kitten", Component.text("Kätzchen", TextColor.color(0xFFA64D)), CosmeticSlot.PET, false, CosmeticAnimation.KITTEN);
        register("bee", Component.text("Bienchen", TextColor.color(0xFFD43B)), CosmeticSlot.PET, false, CosmeticAnimation.BEE);
        register("mushroom", Component.text("Pilzchen", TextColor.color(0xFF6B6B)), CosmeticSlot.PET, false, CosmeticAnimation.MUSHROOM);
        register("owl", Component.text("Eule", TextColor.color(0xC8A070)), CosmeticSlot.PET, false, CosmeticAnimation.OWL);
    }

    private void register(String id, Component name, CosmeticSlot slot, boolean glowing, CosmeticAnimation animation) {
        cosmetics.put(id, new Cosmetic(id, name, slot, new NamespacedKey(NAMESPACE, id), glowing, animation));
    }

    public Cosmetic get(String id) {
        return cosmetics.get(id);
    }

    public Collection<Cosmetic> all() {
        return cosmetics.values();
    }
}
