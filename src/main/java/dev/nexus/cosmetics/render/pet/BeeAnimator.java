package dev.nexus.cosmetics.render.pet;

import org.bukkit.Particle;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.List;

/**
 * Bienchen: summt mit flirrenden Flügeln und fliegt kleine Achten.
 * Besonderheiten:
 * - Steht der Besitzer still, kreist es um seinen Kopf.
 * - Tropft ab und zu Nektar.
 */
final class BeeAnimator extends PetAnimator {

    private static final float SCALE = 0.42f;
    private static final int CIRCLE_AFTER_IDLE_TICKS = 60;

    // Flügel-Gelenke oben auf dem Rücken (Modell-Einheiten)
    private static final float JOINT_X = 2 / 16f;
    private static final float JOINT_Y = 2 / 16f;
    private static final float JOINT_Z = -0.5f / 16f;

    @Override
    public Mode mode() {
        return Mode.FLY;
    }

    @Override
    public List<String> parts() {
        return List.of("body", "wing_a", "wing_b");
    }

    @Override
    public int interpolation(int part) {
        // Flügel ohne Glättung: sie springen jeden Tick hin und her und wirken dadurch wie ein Summ-Flirren
        return part == 0 ? 3 : 1;
    }

    @Override
    public Vector3d offset(PetState state) {
        if (state.ownerIdleTicks > CIRCLE_AFTER_IDLE_TICKS) {
            // Kreis um den Kopf. Der normale Platz liegt 0.85 rechts und 0.35 hinter dem Besitzer.
            double angle = state.age * 0.09;
            return new Vector3d(-0.85 + Math.cos(angle) * 0.75, -0.05 + Math.sin(state.age * 0.2) * 0.05,
                    0.35 + Math.sin(angle) * 0.75);
        }
        // Kleine liegende Acht
        double t = state.age * 0.08;
        return new Vector3d(Math.sin(t) * 0.3, Math.cos(2 * t) * 0.08, Math.sin(2 * t) * 0.2);
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        double bob = Math.sin(state.age * 0.3) * 0.02;
        double wiggle = Math.sin(state.age * 0.5) * 5;
        double tilt = Math.min(state.speed * 140, 20);
        double flap = state.age % 2 == 0 ? 55 : 5;

        Matrix4f body = new Matrix4f(base)
                .translate(0, (float) bob, 0)
                .rotateX(rad(tilt))
                .rotateZ(rad(wiggle))
                .scale(SCALE);
        out[0] = body;
        out[1] = new Matrix4f(body).translate(-JOINT_X, JOINT_Y, JOINT_Z).rotateZ(rad(-flap));
        out[2] = new Matrix4f(body).translate(JOINT_X, JOINT_Y, JOINT_Z).rotateZ(rad(flap));
    }

    @Override
    public void effects(PetState state, PetEffects effects) {
        if (state.age % 50 == 0) {
            effects.particle(Particle.FALLING_NECTAR, 0, -0.1, 0, 1, 0.02, 0);
        }
    }
}
