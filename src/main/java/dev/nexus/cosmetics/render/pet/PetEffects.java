package dev.nexus.cosmetics.render.pet;

import org.bukkit.Particle;

/** Damit Haustier-Animationen Partikel erzeugen können (Herzchen, Flammen ...). */
public interface PetEffects {

    /**
     * Erzeugt Partikel relativ zum Haustier.
     *
     * @param right   Blöcke nach rechts (aus Sicht des Haustiers)
     * @param up      Blöcke nach oben
     * @param forward Blöcke nach vorne (in Blickrichtung des Haustiers)
     * @param spread  zufällige Streuung in alle Richtungen
     */
    void particle(Particle particle, double right, double up, double forward, int count, double spread, double speed);
}
