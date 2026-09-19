package dev.nexus.cosmetics.render.pet;

import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.List;

/**
 * Die Persönlichkeit eines Haustiers: aus welchen Teilen es besteht, wo es sich aufhält
 * und wie es sich bewegt. Jedes Haustier hat eine eigene Unterklasse.
 *
 * Koordinaten-Hinweis für pose(): Die Matrix "base" steht bereits an der Position des Haustiers
 * und zeigt in seine Blickrichtung. Einheiten sind Blöcke: x = links, y = oben, z = vorne.
 * Nach scale(...) rechnet man in Modell-Einheiten (1/16 Block).
 */
public abstract class PetAnimator {

    public enum Mode {
        /** Schwebt neben dem Besitzer */
        FLY,
        /** Läuft neben dem Besitzer am Boden */
        GROUND,
        /** Sitzt auf der rechten Schulter des Besitzers */
        SHOULDER
    }

    public abstract Mode mode();

    /** Modell-Endungen der Teile, z. B. "body", "wing_a". null = das Hauptmodell. Das erste Teil ist der Körper. */
    public abstract List<String> parts();

    /** Berechnet die Matrix jedes Teils. */
    public abstract void pose(PetState state, Matrix4f base, Matrix4f[] out);

    /** Über wie viele Ticks Bewegungen eines Teils geglättet werden. */
    public int interpolation(int part) {
        return 3;
    }

    /**
     * Zusätzlicher Versatz zum normalen Platz neben dem Besitzer (in Blöcken, im Körperraum des
     * Besitzers: x = rechts, y = oben, z = vorne). Für Flugmanöver wie Kreise oder Achten.
     */
    public Vector3d offset(PetState state) {
        return new Vector3d();
    }

    /** Welches Modell ein Teil gerade zeigen soll (z. B. schlafendes Kätzchen). */
    public String model(PetState state, int part) {
        return parts().get(part);
    }

    /** Partikel und andere Effekte. */
    public void effects(PetState state, PetEffects effects) {
    }

    // ------------------------------------------------------------------ Hilfen für Unterklassen

    protected static float rad(double degrees) {
        return (float) Math.toRadians(degrees);
    }

    /** Weiche Kurve von 0 nach 1 (langsam anfangen, langsam aufhören). */
    protected static double smooth(double t) {
        t = Math.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
