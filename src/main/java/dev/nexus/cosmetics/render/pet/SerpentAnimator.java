package dev.nexus.cosmetics.render.pet;

import org.bukkit.Particle;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Schlangen-Wesen (Donnerdrache, Seeschlange, Lavawurm ...): ein Kopf, mehrere Körperglieder und
 * eine Schwanzspitze, die sich wellenförmig durch die Luft winden. Die Welle läuft vom Kopf zum
 * Schwanz, beim Fliegen schneller. Jedes Glied dreht sich in Richtung seiner Bewegung.
 */
final class SerpentAnimator extends PetAnimator {

    private static final int BODY_SEGMENTS = 5;
    private static final float SCALE = 0.55f;
    /** Abstand zwischen zwei Gliedern (Modell-Einheiten) */
    private static final float SPACING = 5.2f / 16f;

    private double phase;

    @Override
    public Mode mode() {
        return Mode.FLY;
    }

    @Override
    public List<String> parts() {
        List<String> parts = new ArrayList<>();
        parts.add("head");
        for (int i = 0; i < BODY_SEGMENTS; i++) {
            parts.add("body");
        }
        parts.add("tail");
        return parts;
    }

    @Override
    public void pose(PetState state, Matrix4f base, Matrix4f[] out) {
        phase += 0.12 + Math.min(state.speed * 2.5, 0.25);
        double amplitude = 1.6 / 16 * (1 + Math.min(state.speed * 4, 0.8));
        Matrix4f origin = new Matrix4f(base).translate(0, (float) (Math.sin(phase * 0.5) * 0.05), 0).scale(SCALE);

        for (int i = 0; i < out.length; i++) {
            double z = -i * SPACING;
            double wave = phase - i * 0.9;
            double x = Math.sin(wave) * amplitude * Math.min(1, i * 0.6);
            double y = Math.cos(wave * 0.7) * amplitude * 0.6 * Math.min(1, i * 0.5);
            // Richtung: Steigung der Welle zum nächsten Glied
            double nextX = Math.sin(wave - 0.9) * amplitude * Math.min(1, (i + 1) * 0.6);
            double yaw = Math.atan2(x - nextX, SPACING);
            double shrink = 1 - i * 0.06;
            out[i] = new Matrix4f(origin)
                    .translate((float) x, (float) y, (float) z)
                    .rotateY((float) yaw)
                    .scale((float) shrink);
        }
    }

    @Override
    public void effects(PetState state, PetEffects effects) {
        if (state.age % 30 == 0) {
            effects.particle(Particle.END_ROD, 0, 0, 0.1, 1, 0.1, 0.01);
        }
    }
}
