package dev.nexus.cosmetics.render;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Das, woran ein Cosmetic hängt.
 *
 * Im Normalfall ist das ein echter Spieler. Für die Vorschau im Menü ist es eine
 * Schaufensterpuppe, die nur ein einziger Spieler sieht. Cape, Flügel, Heiligenschein und
 * Haustiere arbeiten deshalb nicht direkt mit einem Player, sondern mit dieser Schnittstelle —
 * so laufen an der Puppe exakt dieselben Animationen wie am Spieler selbst.
 */
public interface CosmeticCarrier {

    /** Der Normalfall: ein echter Spieler trägt das Cosmetic. */
    static CosmeticCarrier of(Player player) {
        return new PlayerCarrier(player);
    }

    UUID uniqueId();

    /** Entity-ID, auf der Cape und Schulter-Haustiere als Passagiere sitzen. */
    int entityId();

    World world();

    /** Position der Füße, inklusive Blickrichtung (Yaw und Pitch). */
    Location location();

    /** Drehung des Oberkörpers. Kann beim Spieler vom Blick abweichen. */
    float bodyYaw();

    /** Höhe in Blöcken. Capes und Heiligenschein hängen an der Oberkante. */
    double height();

    boolean sneaking();

    boolean sprinting();

    boolean gliding();

    boolean onGround();

    /**
     * Partikel des Cosmetics. Am echten Spieler sehen sie alle in der Nähe, an der Vorschau-Puppe
     * nur der Spieler, für den die Puppe überhaupt da ist.
     */
    void spawnParticle(Particle particle, Location location, int count,
                       double spreadX, double spreadY, double spreadZ, double speed);
}
