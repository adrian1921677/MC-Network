package dev.nexus.cosmetics.render.pet;

import org.bukkit.Particle;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Pinguin: watschelt am Boden neben dem Besitzer und wackelt dabei hin und her.
 * Besonderheiten:
 * - Springt der Besitzer, hüpft der Pinguin mit und flattert mit den Flossen.
 * - Rennt der Besitzer, macht er einen Bauchrutscher (mit Schneeflocken).
 * - Steht der Besitzer still, winkt er ab und zu mit einer Flosse.
 */
final class PenguinAnimator extends PetAnimator {

    private static final float SCALE = 0.55f;
    /** Füße liegen bei y = 1 im Modell: so weit muss das Modell angehoben werden. */
    private static final float GROUND_OFFSET = 7 / 16f * SCALE;
    private static final int HOP_DURATION = 10;

    // Flossen-Gelenke an den Schultern (Modell-Einheiten)
    private static final float JOINT_X = 3.5f / 16f;
    private static final float JOINT_Y = 0f;
    private static final float JOINT_Z = -0.5f / 16f;

    private double waddlePhase;
    private double slide;
    private int hopTick;

    @Override
    public Mode mode() {
        return Mode.GROUND;
    }

    @Override
    public List<String> parts() {
        return List.of("body", "flipper_a", "flipper_b");
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        boolean moving = state.speed > 0.02;
        double walkAmount = Math.min(state.speed * 10, 1);
        if (moving) {
            waddlePhase += 0.3 + state.speed * 2;
        }

        // Bauchrutscher beim Rennen
        double slideTarget = state.ownerSprinting && state.speed > 0.12 ? 1 : 0;
        slide += (slideTarget - slide) * 0.15;

        // Mithüpfen, wenn der Besitzer springt
        if (hopTick == 0 && state.ownerVerticalSpeed > 0.3 && slide < 0.3) {
            hopTick = 1;
        }
        double hop = 0;
        if (hopTick > 0) {
            hop = Math.sin(Math.PI * hopTick / HOP_DURATION);
            hopTick = hopTick >= HOP_DURATION ? 0 : hopTick + 1;
        }

        double roll = Math.sin(waddlePhase) * 14 * walkAmount * (1 - slide);
        double bob = Math.abs(Math.sin(waddlePhase)) * 0.025 * walkAmount;

        Matrix4f body = new Matrix4f(base)
                .translate(0, (float) (GROUND_OFFSET + bob + hop * 0.3 - slide * 0.12), 0)
                .rotateX(rad(slide * 80)) // auf den Bauch legen
                .rotateZ(rad(roll))
                .scale(SCALE);

        // Flossen: leicht abgespreizt, beim Hüpfen flattern, beim Winken eine Flosse hoch
        double flapA = 8 + hop * 55;
        double flapB = flapA;
        if (state.ownerIdleTicks > 60 && state.age % 110 < 26) {
            flapB = 110 + Math.sin(state.age * 0.9) * 25;
        }
        if (slide > 0.5) {
            flapA = flapB = 2; // beim Rutschen eng anlegen
        }

        out[0] = body;
        out[1] = new Matrix4f(body).translate(-JOINT_X, JOINT_Y, JOINT_Z).rotateZ(rad(-flapA));
        out[2] = new Matrix4f(body).translate(JOINT_X, JOINT_Y, JOINT_Z).rotateZ(rad(flapB));
    }

    @Override
    public void effects(PetState state, PetEffects effects) {
        if (slide > 0.6 && state.speed > 0.1) {
            effects.particle(Particle.SNOWFLAKE, 0, 0.05, -0.25, 2, 0.05, 0.01);
        }
    }
}
