package dev.nexus.cosmetics.cosmetic;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Liste aller verfügbaren Cosmetics, gelesen aus cosmetics.yml.
 */
public final class CosmeticRegistry {

    /** Namespace des mitgelieferten Resource Packs (Ordner assets/nexus/...). */
    public static final String NAMESPACE = "nexus";

    private final Map<String, Cosmetic> cosmetics = new LinkedHashMap<>();

    /** Liest alle Cosmetics neu ein. Fehlerhafte Einträge werden mit Warnung übersprungen. */
    public void load(YamlConfiguration config, Logger logger) {
        cosmetics.clear();
        ConfigurationSection section = config.getConfigurationSection("cosmetics");
        if (section == null) {
            logger.warning("cosmetics.yml enthält keinen Abschnitt 'cosmetics:'.");
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null || !entry.getBoolean("enabled", true)) {
                continue;
            }
            try {
                CosmeticSlot slot = CosmeticSlot.valueOf(entry.getString("slot", "HEAD").toUpperCase(Locale.ROOT));
                CosmeticAnimation animation = CosmeticAnimation.valueOf(entry.getString("type", "NONE").toUpperCase(Locale.ROOT));
                NamespacedKey model = NamespacedKey.fromString(entry.getString("model", NAMESPACE + ":" + id));
                if (model == null) {
                    throw new IllegalArgumentException("ungültiges Modell '" + entry.getString("model") + "'");
                }
                if (!fits(slot, animation)) {
                    logger.warning("Cosmetic '" + id + "': Typ " + animation + " passt nicht zu Slot " + slot + ".");
                }
                Particle aura = null;
                String particleName = entry.getString("particle", "");
                if (!particleName.isBlank()) {
                    aura = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
                    if (aura.getDataType() != Void.class) {
                        logger.warning("Cosmetic '" + id + "': Partikel " + aura + " braucht Zusatzdaten und wird ignoriert.");
                        aura = null;
                    }
                }
                cosmetics.put(id, new Cosmetic(id,
                        MiniMessage.miniMessage().deserialize(entry.getString("name", id)),
                        slot, model,
                        entry.getBoolean("glowing", false),
                        animation,
                        entry.getBoolean("unlocked-by-default", false),
                        Rarity.valueOf(entry.getString("rarity", "COMMON").toUpperCase(Locale.ROOT)),
                        Math.clamp(entry.getDouble("scale", 1.0), 0.2, 3.0),
                        aura));
            } catch (IllegalArgumentException exception) {
                logger.warning("Cosmetic '" + id + "' wird übersprungen: " + exception.getMessage());
            }
        }
    }

    /** Prüft, ob Typ und Slot zusammenpassen (z. B. kein Pinguin als Hut). */
    private static boolean fits(CosmeticSlot slot, CosmeticAnimation animation) {
        return switch (slot) {
            case HEAD -> List.of(CosmeticAnimation.NONE, CosmeticAnimation.TALKING, CosmeticAnimation.HALO).contains(animation);
            case BACK -> List.of(CosmeticAnimation.NONE, CosmeticAnimation.ROBE, CosmeticAnimation.WINGS,
                    CosmeticAnimation.BACKPACK).contains(animation);
            case PET -> !List.of(CosmeticAnimation.NONE, CosmeticAnimation.ROBE, CosmeticAnimation.WINGS, CosmeticAnimation.BACKPACK,
                    CosmeticAnimation.TALKING, CosmeticAnimation.HALO).contains(animation);
        };
    }

    public Cosmetic get(String id) {
        return cosmetics.get(id);
    }

    public Collection<Cosmetic> all() {
        return cosmetics.values();
    }

    public List<Cosmetic> inSlot(CosmeticSlot slot) {
        return cosmetics.values().stream().filter(cosmetic -> cosmetic.slot() == slot).toList();
    }
}
