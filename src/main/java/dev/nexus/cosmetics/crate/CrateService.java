package dev.nexus.cosmetics.crate;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.Rarity;
import dev.nexus.cosmetics.render.FakeCosmeticRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Öffnet Truhen: Schlüssel prüfen, fair einen Gewinn ziehen, sofort freischalten
 * (damit nichts verloren geht, falls der Spieler während der Animation rausgeht) und die Show starten.
 */
public final class CrateService {

    private static final String ANIMATION_PLACE = "CRATE";

    public enum OpenResult { OPENED, NO_KEY, ALL_OWNED, BUSY }

    private final NexusCosmetics plugin;
    private final CrateRegistry registry;
    private final FakeCosmeticRenderer renderer;
    private final Random random = new Random();

    public CrateService(NexusCosmetics plugin, CrateRegistry registry, FakeCosmeticRenderer renderer) {
        this.plugin = plugin;
        this.registry = registry;
        this.renderer = renderer;
    }

    public CrateRegistry registry() {
        return registry;
    }

    public OpenResult open(Player player, Crate crate) {
        CosmeticManager cosmetics = plugin.cosmetics();
        if (renderer.isShowing(player, ANIMATION_PLACE)) {
            return OpenResult.BUSY;
        }
        if (cosmetics.profile(player).keys(crate.id()) <= 0) {
            return OpenResult.NO_KEY;
        }

        // Nur Gewinne, die der Spieler noch nicht hat (gewonnen oder per Rang freigeschaltet).
        // Für OPs zählen Rechte nicht, sonst hätten sie automatisch "alles" und die Truhe wäre leer.
        List<CrateReward> available = crate.rewards().stream()
                .filter(reward -> !cosmetics.owns(player, reward.ownershipKey()))
                .filter(reward -> player.isOp() || !player.hasPermission(reward.permission()))
                .toList();
        if (available.isEmpty()) {
            return OpenResult.ALL_OWNED;
        }

        CrateReward reward = draw(crate.chances(), available);
        cosmetics.modifyProfile(player.getUniqueId(), profile -> {
            profile.addKeys(crate.id(), -1);
            profile.owned().add(reward.ownershipKey());
        });

        Component rarityName = plugin.messages().get("rarities." + reward.rarity().name());
        renderer.show(player, ANIMATION_PLACE, new CrateAnimation(player, reward, crate.rewards(), rarityName,
                () -> announce(player, reward, rarityName)));
        return OpenResult.OPENED;
    }

    /**
     * Zieht einen Gewinn: erst die Seltenheit nach den Chancen der Truhe, dann zufällig innerhalb der
     * Seltenheit. Seltenheiten ohne verfügbare Gewinne fallen weg, die restlichen Chancen bleiben im Verhältnis.
     */
    private CrateReward draw(Map<Rarity, Integer> chances, List<CrateReward> available) {
        int total = 0;
        for (Rarity rarity : Rarity.values()) {
            if (hasRarity(available, rarity)) {
                total += chances.getOrDefault(rarity, 0);
            }
        }
        Rarity picked = null;
        if (total > 0) {
            int roll = random.nextInt(total);
            for (Rarity rarity : Rarity.values()) {
                if (!hasRarity(available, rarity)) {
                    continue;
                }
                roll -= chances.getOrDefault(rarity, 0);
                if (roll < 0) {
                    picked = rarity;
                    break;
                }
            }
        }
        Rarity finalPick = picked;
        List<CrateReward> candidates = finalPick == null
                ? available
                : available.stream().filter(reward -> reward.rarity() == finalPick).toList();
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static boolean hasRarity(List<CrateReward> rewards, Rarity rarity) {
        return rewards.stream().anyMatch(reward -> reward.rarity() == rarity);
    }

    /** Wird im Moment des Aufspringens aufgerufen. */
    private void announce(Player player, CrateReward reward, Component rarityName) {
        if (!player.isOnline()) {
            return;
        }
        player.sendMessage(plugin.messages().prefixed("crates.won",
                Placeholder.component("rarity", rarityName),
                Placeholder.component("name", reward.displayName())));
        if (reward.rarity() == Rarity.ULTRA) {
            // ULTRA: großer Titel für alle
            net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
                    rarityName, plugin.messages().get("crates.ultra-subtitle",
                            Placeholder.unparsed("player", player.getName()),
                            Placeholder.component("name", reward.displayName())));
            plugin.getServer().getOnlinePlayers().forEach(other -> other.showTitle(title));
        }
        if (reward.rarity().ordinal() >= Rarity.LEGENDARY.ordinal()) {
            Component broadcast = plugin.messages().prefixed("crates.broadcast",
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.component("rarity", rarityName),
                    Placeholder.component("name", reward.displayName()));
            plugin.getServer().getOnlinePlayers().forEach(other -> {
                other.sendMessage(broadcast);
                if (!other.equals(player)) {
                    other.playSound(other.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.3f);
                }
            });
        }
    }
}
