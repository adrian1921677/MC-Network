package dev.nexus.cosmetics.render.pet;

import org.bukkit.Particle;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Kätzchen: sitzt auf der Schulter des Besitzers und schwingt den Schwanz.
 * Besonderheiten:
 * - Schleicht der Besitzer, schnurrt es: kleine Herzchen steigen auf.
 * - Steht der Besitzer länger still, rollt es sich ein und schläft (eigenes Modell).
 */
final class KittenAnimator extends PetAnimator {

    private static final float SCALE = 0.42f;
    /** Pfoten liegen bei y = 4 im Modell. */
    private static final float BOTTOM = 4 / 16f;
    private static final float SIT_OFFSET = (8 - 4) / 16f * SCALE;
    private static final int FALL_ASLEEP_TICKS = 160;

    // Schwanz-Gelenk hinten unten am Körper (Modell-Einheiten)
    private static final float TAIL_Y = -3 / 16f;
    private static final float TAIL_Z = -3 / 16f;

    @Override
    public Mode mode() {
        return Mode.SHOULDER;
    }

    @Override
    public List<String> parts() {
        return List.of("body", "tail");
    }

    private boolean sleeping(PetState state) {
        return state.ownerIdleTicks > FALL_ASLEEP_TICKS && !state.ownerSneaking;
    }

    @Override
    public String model(PetState state, int part) {
        if (part == 0 && sleeping(state)) {
            return "sleep";
        }
        return parts().get(part);
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        boolean sleeping = sleeping(state);
        // Atmen: im Schlaf langsamer und tiefer
        double breath = sleeping ? Math.sin(state.age * 0.06) * 0.04 : Math.sin(state.age * 0.1) * 0.02;
        // Schnurren: kleines Kuscheln hin und her
        double purr = state.ownerSneaking ? Math.sin(state.age * 0.4) * 5 : 0;

        Matrix4f body = new Matrix4f(base)
                .translate(0, SIT_OFFSET, 0)
                .rotateZ(rad(purr))
                .scale(SCALE)
                .translate(0, -BOTTOM, 0)
                .scale(1, (float) (1 + breath), 1)
                .translate(0, BOTTOM, 0);
        out[0] = body;

        if (sleeping) {
            // Das Schlaf-Modell hat den Schwanz schon eingerollt
            out[1] = new Matrix4f(body).scale(0.001f);
        } else {
            double swish = Math.sin(state.age * 0.12) * 30;
            double lift = 10 + Math.sin(state.age * 0.07) * 10;
            out[1] = new Matrix4f(body)
                    .translate(0, TAIL_Y, TAIL_Z)
                    .rotateY(rad(swish))
                    .rotateX(rad(-lift));
        }
    }

    @Override
    public void effects(PetState state, PetEffects effects) {
        if (state.ownerSneaking && state.age % 12 == 0) {
            effects.particle(Particle.HEART, 0, 0.3, 0, 1, 0.08, 0);
        }
    }
}
