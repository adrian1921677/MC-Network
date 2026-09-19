package dev.nexus.cosmetics.render.pet;

import java.util.Random;

/**
 * Alles, was eine Haustier-Animation über die aktuelle Situation wissen muss.
 * Wird von FakePet jeden Tick aktualisiert.
 */
public final class PetState {

    /** Ticks seit das Haustier erschienen ist (20 pro Sekunde). */
    public int age;
    /** Wie schnell sich das Haustier gerade bewegt (Blöcke pro Tick, waagerecht). */
    public double speed;
    /** Wie schnell sich der Besitzer bewegt (Blöcke pro Tick, waagerecht). */
    public double ownerSpeed;
    /** Senkrechte Bewegung des Besitzers (positiv = springt/steigt). */
    public double ownerVerticalSpeed;
    /** Wie lange der Besitzer schon still steht (Ticks). */
    public int ownerIdleTicks;
    public boolean ownerSneaking;
    public boolean ownerSprinting;
    /** Ist es in der Welt des Besitzers gerade Nacht? */
    public boolean night;

    public final Random random = new Random();
}
