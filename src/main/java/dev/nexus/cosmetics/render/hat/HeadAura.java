package dev.nexus.cosmetics.render.hat;

import dev.nexus.cosmetics.render.FakeCosmetic;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Partikel-Aura für normale Hüte (die im Helm-Slot sitzen und selbst nichts ausstoßen können),
 * z. B. Schneeflocken um das Eisdiadem. Besteht nur aus Partikeln, ohne eigene Entities.
 */
public final class HeadAura implements FakeCosmetic {

    private final Player wearer;
    private final Particle particle;
    private int age;

    public HeadAura(Player wearer, Particle particle) {
        this.wearer = wearer;
        this.particle = particle;
    }

    @Override
    public void tick(int serverTick) {
        age++;
        if (age % 4 == 0) {
            wearer.getWorld().spawnParticle(particle, wearer.getLocation().add(0, wearer.getHeight() + 0.15, 0),
                    1, 0.3, 0.15, 0.3, 0.005);
        }
    }

    @Override
    public void show(Player viewer) {
    }

    @Override
    public void hide(Player viewer) {
    }

    @Override
    public void forget(UUID viewer) {
    }

    @Override
    public void destroy() {
    }
}
