package dev.nexus.cosmetics.crate;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.Rarity;
import dev.nexus.cosmetics.emote.Emote;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;

/** Ein möglicher Gewinn aus einer Truhe: ein Cosmetic oder ein Emote. */
public record CrateReward(String ownershipKey, Component displayName, Rarity rarity, NamespacedKey model, String permission) {

    public static CrateReward of(Cosmetic cosmetic) {
        return new CrateReward(cosmetic.ownershipKey(), cosmetic.displayName(), cosmetic.rarity(), cosmetic.model(),
                cosmetic.permission());
    }

    /** Posen-Emotes haben kein eigenes Modell, sie werden mit dem Party-Emoji dargestellt. */
    public static CrateReward of(Emote emote, NamespacedKey fallbackModel) {
        return new CrateReward(emote.ownershipKey(), emote.displayName(), emote.rarity(),
                emote.type().isEmoji() ? emote.model() : fallbackModel, emote.permission());
    }
}
