package dev.nexus.cosmetics.render;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Ein echter Spieler als Träger eines Cosmetics. Reicht alles unverändert an Bukkit weiter. */
public record PlayerCarrier(Player player) implements CosmeticCarrier {

    @Override
    public UUID uniqueId() {
        return player.getUniqueId();
    }

    @Override
    public int entityId() {
        return player.getEntityId();
    }

    @Override
    public World world() {
        return player.getWorld();
    }

    @Override
    public Location location() {
        return player.getLocation();
    }

    @Override
    public float bodyYaw() {
        return player.getBodyYaw();
    }

    @Override
    public double height() {
        return player.getHeight();
    }

    @Override
    public boolean sneaking() {
        return player.isSneaking();
    }

    @Override
    public boolean sprinting() {
        return player.isSprinting();
    }

    @Override
    public boolean gliding() {
        return player.isGliding();
    }

    @Override
    @SuppressWarnings("deprecation") // Die Alternative kennt nur der Client; für Haustiere reicht das hier
    public boolean onGround() {
        return player.isOnGround();
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count,
                              double spreadX, double spreadY, double spreadZ, double speed) {
        player.getWorld().spawnParticle(particle, location, count, spreadX, spreadY, spreadZ, speed);
    }
}
