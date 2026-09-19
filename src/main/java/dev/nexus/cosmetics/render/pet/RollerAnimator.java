package dev.nexus.cosmetics.render.pet;

import org.bukkit.Particle;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Kugel-Droide: Der Kugelkörper rollt am Boden (dreht sich passend zur Strecke), der Kopf bleibt
 * obenauf, neigt sich beim Fahren nach vorne und schaut sich im Stand um.
 * Besonderheit: piepst ab und zu fröhlich (Noten-Partikel) und wirbelt beim Anfahren Staub auf.
 */
final class RollerAnimator extends PetAnimator {

    private static final float SCALE = 0.5f;
    /** Unterkante des Körpers liegt bei y = 1 im Modell, die Kugelmitte bei y = 5. */
    private static final float GROUND_OFFSET = 7 / 16f * SCALE;
    private static final float BODY_CENTER = -3 / 16f;
    private static final float HEAD_JOINT = 1 / 16f;
    private static final double RADIUS_BLOCKS = 4 / 16.0 * SCALE;

    private double roll;
    private double headYaw;
    private double headTarget;
    private double headTilt;

    @Override
    public Mode mode() {
        return Mode.GROUND;
    }

    @Override
    public List<String> parts() {
        return List.of("body", "head");
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        // Rollen: Winkel = Strecke / Radius
        roll += Math.toDegrees(state.speed / RADIUS_BLOCKS);

        if (state.age % 50 == 0) {
            headTarget = state.speed > 0.03 ? 0 : (state.random.nextDouble() - 0.5) * 120;
        }
        headYaw += (headTarget - headYaw) * 0.12;
        double tiltTarget = Math.min(state.speed * 160, 18);
        headTilt += (tiltTarget - headTilt) * 0.2;
        double bob = Math.abs(Math.sin(state.age * 0.3)) * 0.01;

        Matrix4f origin = new Matrix4f(base).translate(0, (float) (GROUND_OFFSET + bob), 0).scale(SCALE);
        out[0] = new Matrix4f(origin)
                .translate(0, BODY_CENTER, 0)
                .rotateX(rad(roll))
                .translate(0, -BODY_CENTER, 0);
        out[1] = new Matrix4f(origin)
                .translate(0, HEAD_JOINT, 0)
                .rotateY(rad(headYaw))
                .rotateX(rad(headTilt));
    }

    @Override
    public void effects(PetState state, PetEffects effects) {
        if (state.ownerIdleTicks > 40 && state.age % 70 == 0) {
            effects.particle(Particle.NOTE, 0, 0.45, 0, 1, 0, 1);
        }
        if (state.speed > 0.12 && state.age % 4 == 0) {
            effects.particle(Particle.CLOUD, 0, 0.02, -0.2, 1, 0.05, 0.005);
        }
    }
}
