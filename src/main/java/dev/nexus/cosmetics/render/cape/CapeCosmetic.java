package dev.nexus.cosmetics.render.cape;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.render.FakeCosmetic;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Eine getragene Cape. Besteht aus zwei Kopien: eine für alle anderen und eine nur für den Träger.
 * So kann die eigene Cape ausgeblendet werden, wenn man nach unten schaut, ohne dass sie
 * für andere verschwindet.
 */
public final class CapeCosmetic implements FakeCosmetic {

    private final Player wearer;
    private final FakeCape forOthers;
    private final FakeCape forSelf;

    public CapeCosmetic(Player wearer, Cosmetic cosmetic) {
        this.wearer = wearer;
        this.forOthers = new FakeCape(wearer, cosmetic, false);
        this.forSelf = new FakeCape(wearer, cosmetic, true);
    }

    private FakeCape copyFor(Player viewer) {
        return viewer.getUniqueId().equals(wearer.getUniqueId()) ? forSelf : forOthers;
    }

    @Override
    public void show(Player viewer) {
        copyFor(viewer).show(viewer);
    }

    @Override
    public void hide(Player viewer) {
        copyFor(viewer).hide(viewer);
    }

    @Override
    public void forget(UUID viewer) {
        forOthers.forget(viewer);
        forSelf.forget(viewer);
    }

    @Override
    public void tick(int serverTick) {
        forOthers.tick();
        forSelf.tick();
    }

    @Override
    public int[] passengerIds(Player viewer) {
        return copyFor(viewer).entityIds();
    }

    @Override
    public void destroy() {
        forOthers.destroy();
        forSelf.destroy();
    }
}
