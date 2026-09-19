package dev.nexus.cosmetics.render.pet;

import org.bukkit.Particle;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

/**
 * Pilzchen: hüpft neben dem Besitzer her, staucht sich vor dem Absprung und streckt sich in der Luft.
 * Besonderheiten:
 * - Beim Landen pufft eine kleine Sporenwolke.
 * - Steht der Besitzer still, dreht es sich bei jedem dritten Hüpfer vor Freude im Kreis.
 */
final class MushroomAnimator extends PetAnimator {

    private static final float SCALE = 0.5f;
    /** Füße liegen bei y = 1 im Modell. */
    private static final float BOTTOM = 7 / 16f;
    private static final float GROUND_OFFSET = BOTTOM * SCALE;
    private static final double SQUASH_PART = 0.18;

    private double hopPhase;
    private int lastHop;
    private boolean landed;

    @Override
    public Mode mode() {
        return Mode.GROUND;
    }

    @Override
    public List<String> parts() {
        return Collections.singletonList(null);
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        boolean moving = state.speed > 0.02;
        hopPhase += moving ? 0.085 : 0.03;

        int hop = (int) hopPhase;
        landed = hop != lastHop;
        lastHop = hop;
        boolean happySpin = state.ownerIdleTicks > 100 && hop % 3 == 0;

        double t = hopPhase - hop;
        double height = 0;
        double stretch;
        double spin = 0;
        if (t < SQUASH_PART) {
            // Vor dem Absprung zusammenstauchen
            stretch = -0.28 * Math.sin(Math.PI * t / SQUASH_PART);
        } else {
            double air = (t - SQUASH_PART) / (1 - SQUASH_PART);
            height = Math.sin(Math.PI * air) * (moving ? 0.35 : 0.2);
            stretch = 0.12 * Math.sin(Math.PI * air);
            if (happySpin) {
                spin = smooth(air) * 360;
            }
        }
        double tilt = Math.min(state.speed * 120, 12);

        out[0] = new Matrix4f(base)
                .translate(0, (float) (GROUND_OFFSET + height), 0)
                .rotateY(rad(spin))
                .rotateX(rad(tilt))
                .scale(SCALE)
                // Stauchen/Strecken von den Füßen aus
                .translate(0, -BOTTOM, 0)
                .scale((float) (1 - stretch * 0.5), (float) (1 + stretch), (float) (1 - stretch * 0.5))
                .translate(0, BOTTOM, 0);
    }

    @Override
    public void effects(PetState state, PetEffects effects) {
        if (landed) {
            effects.particle(Particle.CRIMSON_SPORE, 0, 0.05, 0, 5, 0.12, 0.01);
        }
    }
}
