package dev.nexus.cosmetics.render.pet;

import org.joml.Matrix4f;

import java.util.List;

/**
 * Quallen-Wesen: Der Schirm zieht sich rhythmisch zusammen und stößt die Qualle dabei ein Stück
 * nach oben, die Tentakel wehen hinterher und strecken sich beim Stoß.
 */
final class JellyAnimator extends PetAnimator {

    private static final float SCALE = 0.55f;
    /** Tentakel hängen an der Unterseite des Schirms (Modell-Einheiten) */
    private static final float TENTACLE_JOINT = -2 / 16f;

    @Override
    public Mode mode() {
        return Mode.FLY;
    }

    @Override
    public List<String> parts() {
        return List.of("body", "tentacles");
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        double beat = state.age * 0.16;
        double squeeze = Math.max(0, Math.sin(beat));                 // Zusammenziehen
        double rise = -Math.cos(beat) * 0.06;                          // Stoß nach oben
        double tilt = Math.min(state.speed * 120, 18);

        Matrix4f body = new Matrix4f(base)
                .translate(0, (float) rise, 0)
                .rotateX((float) Math.toRadians(tilt))
                .scale(SCALE);
        out[0] = new Matrix4f(body).scale((float) (1 - squeeze * 0.12), (float) (1 + squeeze * 0.12), (float) (1 - squeeze * 0.12));
        out[1] = new Matrix4f(body)
                .translate(0, TENTACLE_JOINT, 0)
                .rotateX(rad(Math.sin(beat - 1.2) * 12 + tilt * 0.8))
                .rotateZ(rad(Math.sin(beat * 0.7) * 8))
                .scale(1, (float) (1 + squeeze * 0.2), 1);
    }
}
