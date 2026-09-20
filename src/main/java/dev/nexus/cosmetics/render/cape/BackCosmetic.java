package dev.nexus.cosmetics.render.cape;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.render.CosmeticCarrier;
import dev.nexus.cosmetics.render.FakeCosmetic;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Etwas, das auf dem Rücken getragen wird (Cape, Umhang, Flügel). Besteht aus zwei Kopien:
 * eine für alle anderen und eine nur für den Träger. So kann die eigene Kopie ausgeblendet werden,
 * wenn man nach unten schaut, ohne dass sie für andere verschwindet.
 */
public final class BackCosmetic implements FakeCosmetic {

    private final CosmeticCarrier wearer;
    private final BackPiece forOthers;
    private final BackPiece forSelf;

    private BackCosmetic(CosmeticCarrier wearer, BackPiece forOthers, BackPiece forSelf) {
        this.wearer = wearer;
        this.forOthers = forOthers;
        this.forSelf = forSelf;
    }

    /** Cape oder langer Umhang. */
    public static BackCosmetic cape(CosmeticCarrier wearer, Cosmetic cosmetic) {
        return new BackCosmetic(wearer, new FakeCape(wearer, cosmetic, false), new FakeCape(wearer, cosmetic, true));
    }

    /** Festes Teil auf dem Rücken (Rucksack, Jetpack ...). */
    public static BackCosmetic backItem(CosmeticCarrier wearer, Cosmetic cosmetic) {
        return new BackCosmetic(wearer, new FakeBackItem(wearer, cosmetic, false), new FakeBackItem(wearer, cosmetic, true));
    }

    /** Schlagende Flügel. */
    public static BackCosmetic wings(CosmeticCarrier wearer, Cosmetic cosmetic) {
        return new BackCosmetic(wearer, new FakeWings(wearer, cosmetic, false), new FakeWings(wearer, cosmetic, true));
    }

    private BackPiece copyFor(Player viewer) {
        return viewer.getUniqueId().equals(wearer.uniqueId()) ? forSelf : forOthers;
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
