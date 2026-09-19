package dev.nexus.cosmetics.crate;

import dev.nexus.cosmetics.cosmetic.Rarity;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

/**
 * Eine Truhen-Art aus crates.yml.
 *
 * @param chances Gewichtung je Seltenheit (z. B. COMMON 60, LEGENDARY 3)
 * @param rewards alle möglichen Gewinne
 */
public record Crate(String id, Component displayName, Map<Rarity, Integer> chances, List<CrateReward> rewards) {
}
