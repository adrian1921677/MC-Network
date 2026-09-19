package dev.nexus.cosmetics.render.pet;

import dev.nexus.cosmetics.cosmetic.CosmeticAnimation;

/** Erstellt für jedes Haustier seine passende Animation. */
final class PetAnimators {

    private PetAnimators() {
    }

    static PetAnimator create(CosmeticAnimation animation) {
        return switch (animation) {
            case DRAGON -> new DragonAnimator();
            case PENGUIN -> new PenguinAnimator();
            case KITTEN -> new KittenAnimator();
            case BEE -> new BeeAnimator();
            case MUSHROOM -> new MushroomAnimator();
            case OWL -> new OwlAnimator();
            default -> new GhostAnimator();
        };
    }
}
