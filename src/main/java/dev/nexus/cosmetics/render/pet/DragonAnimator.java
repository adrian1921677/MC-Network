package dev.nexus.cosmetics.render.pet;

import org.bukkit.Particle;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Mini-Drache: fliegt mit schlagenden Flügeln, beim Fliegen schneller.
 * Besonderheit: pustet ab und zu ein kleines Flämmchen.
 */
final class DragonAnimator extends PetAnimator {

    private static final float SCALE = 0.55f;
    private static final int FLAME_EVERY = 140;
    private static final int FLAME_DURATION = 14;

    // Flügel-Gelenke an den Schultern (Modell-Einheiten)
    private static final float JOINT_X = 3 / 16f;
    private static final float JOINT_Y = 2 / 16f;
    private static final float JOINT_Z = -0.5f / 16f;

    private double flapPhase;

    @Override
    public Mode mode() {
        return Mode.FLY;
    }

    @Override
    public List<String> parts() {
        return List.of("body", "wing_a", "wing_b");
    }

    private boolean breathingFire(PetState state) {
        return state.age % FLAME_EVERY < FLAME_DURATION;
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        flapPhase += 0.32 + Math.min(state.speed * 3, 0.35); // schneller schlagen beim Fliegen
        double flap = 15 + Math.sin(flapPhase) * 38;
        double bob = Math.cos(flapPhase) * 0.04; // Körper hebt sich beim Flügelschlag
        double tilt = Math.min(state.speed * 140, 20);
        if (breathingFire(state)) {
            tilt -= 12; // Kopf hoch beim Pusten
        }

        Matrix4f body = new Matrix4f(base)
                .translate(0, (float) bob, 0)
                .rotateX(rad(tilt))
                .scale(SCALE);
        out[0] = body;
        out[1] = new Matrix4f(body).translate(-JOINT_X, JOINT_Y, JOINT_Z).rotateZ(rad(-flap));
        out[2] = new Matrix4f(body).translate(JOINT_X, JOINT_Y, JOINT_Z).rotateZ(rad(flap));
    }

    @Override
    public void effects(PetState state, PetEffects effects) {
        if (breathingFire(state) && state.age % 2 == 0) {
            effects.particle(Particle.SMALL_FLAME, 0, 0.05, 0.32, 2, 0.02, 0.01);
        }
    }
}
