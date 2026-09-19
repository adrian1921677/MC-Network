package dev.nexus.cosmetics.render.pet;

import org.bukkit.Particle;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Eule: sitzt auf der Schulter, schaut sich neugierig um und legt den Kopf schief.
 * Besonderheiten:
 * - Dreht ab und zu den Kopf komplett nach hinten (wie echte Eulen!).
 * - Fällt oder rennt der Besitzer, breitet sie die Flügel aus, um die Balance zu halten.
 * - Nachts, wenn der Besitzer still steht, schuhut sie (Noten-Partikel).
 * - Blinzelt (animierte Textur).
 */
final class OwlAnimator extends PetAnimator {

    private static final float SCALE = 0.42f;
    /** Füße liegen bei y = 3.5 im Modell. */
    private static final float BOTTOM = 4.5f / 16f;
    private static final float SIT_OFFSET = BOTTOM * SCALE;

    // Gelenke (Modell-Einheiten)
    private static final float NECK_Y = 2 / 16f;
    private static final float NECK_Z = -0.25f / 16f;
    private static final float WING_X = 3 / 16f;
    private static final float WING_Y = 1.5f / 16f;
    private static final float WING_Z = -0.5f / 16f;

    private static final int HEAD_SPIN_EVERY = 360;
    private static final int HEAD_SPIN_DURATION = 50;

    private double headYaw;
    private double lookTarget;
    private double headTilt;
    private double tiltTarget;
    private double spread;

    @Override
    public Mode mode() {
        return Mode.SHOULDER;
    }

    @Override
    public List<String> parts() {
        return List.of("body", "head", "wing_a", "wing_b");
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        // Umschauen: ab und zu einen neuen Blickwinkel wählen
        if (state.age % 70 == 0) {
            lookTarget = (state.random.nextDouble() - 0.5) * 100;
            tiltTarget = state.random.nextDouble() < 0.35 ? (state.random.nextBoolean() ? 18 : -18) : 0;
        }

        // Besonderheit: Kopf einmal ganz nach hinten drehen und wieder zurück
        int spinTick = state.age % HEAD_SPIN_EVERY;
        double target = lookTarget;
        if (spinTick < HEAD_SPIN_DURATION) {
            target = spinTick < HEAD_SPIN_DURATION * 0.7 ? 180 : lookTarget;
        }
        headYaw += (target - headYaw) * 0.18;
        headTilt += (tiltTarget - headTilt) * 0.12;

        // Flügel ausbreiten beim Fallen oder Rennen
        double spreadTarget = state.ownerVerticalSpeed < -0.25 || state.ownerSprinting ? 1 : 0;
        spread += (spreadTarget - spread) * 0.2;
        double wing = spread * (55 + Math.sin(state.age * 0.9) * 25);

        // Leichtes Mitwippen, wenn der Besitzer läuft
        double bob = Math.abs(Math.sin(state.age * 0.35)) * 0.012 * Math.min(state.ownerSpeed * 6, 1);

        Matrix4f body = new Matrix4f(base)
                .translate(0, (float) (SIT_OFFSET + bob), 0)
                .scale(SCALE);
        out[0] = body;
        out[1] = new Matrix4f(body)
                .translate(0, NECK_Y, NECK_Z)
                .rotateY(rad(headYaw))
                .rotateZ(rad(headTilt));
        out[2] = new Matrix4f(body).translate(-WING_X, WING_Y, WING_Z).rotateZ(rad(-wing));
        out[3] = new Matrix4f(body).translate(WING_X, WING_Y, WING_Z).rotateZ(rad(wing));
    }

    @Override
    public void effects(PetState state, PetEffects effects) {
        if (state.night && state.ownerIdleTicks > 100 && state.age % 90 == 0) {
            effects.particle(Particle.NOTE, 0, 0.35, 0, 1, 0, 0);
        }
    }
}
