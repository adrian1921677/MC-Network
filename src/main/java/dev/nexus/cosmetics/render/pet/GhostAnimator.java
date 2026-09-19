package dev.nexus.cosmetics.render.pet;

import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

/**
 * Geist: schwebt auf und ab, wackelt sanft hin und her.
 * Besonderheit: dreht ab und zu eine fröhliche Pirouette.
 */
final class GhostAnimator extends PetAnimator {

    private static final float SCALE = 0.55f;
    private static final int SPIN_EVERY = 180;
    private static final int SPIN_DURATION = 24;

    @Override
    public Mode mode() {
        return Mode.FLY;
    }

    @Override
    public List<String> parts() {
        return Collections.singletonList(null);
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        double bob = Math.sin(state.age * 0.08) * 0.08;
        double roll = Math.sin(state.age * 0.05) * 8;
        double tilt = Math.min(state.speed * 140, 15);

        int spinTick = state.age % SPIN_EVERY;
        double spin = spinTick < SPIN_DURATION ? smooth(spinTick / (double) SPIN_DURATION) * 360 : 0;

        out[0] = new Matrix4f(base)
                .translate(0, (float) bob, 0)
                .rotateY(rad(spin))
                .rotateX(rad(tilt))
                .rotateZ(rad(roll))
                .scale(SCALE);
    }
}
