package dev.nexus.cosmetics.preview;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.CosmeticAnimation;
import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.cosmetic.CosmeticSlot;
import dev.nexus.cosmetics.render.CosmeticCarrier;
import dev.nexus.cosmetics.render.FakeCosmetic;
import dev.nexus.cosmetics.render.Packets;
import dev.nexus.cosmetics.render.cape.BackCosmetic;
import dev.nexus.cosmetics.render.hat.FloatingHalo;
import dev.nexus.cosmetics.render.pet.FakePet;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.decoration.Mannequin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Eine Schaufensterpuppe, die nur ein einziger Spieler sieht.
 *
 * Für die Vorschau schliesst sich das Menü, dann steht sie frei vor ihm, trägt seine eigene
 * Haut und dreht sich langsam wie ein Podest im Schaufenster. Weil sie ein CosmeticCarrier ist,
 * laufen an ihr exakt dieselben Animationen wie am Spieler selbst: dieselbe Cape-Physik,
 * derselbe Flügelschlag, dasselbe Haustier.
 *
 * Die Puppe existiert nur als Netzwerk-Paket. Der Server kennt sie nicht, andere Spieler
 * sehen sie nicht, und sie steht niemandem im Weg.
 */
public final class PreviewStand implements CosmeticCarrier {

    /** Näher heran darf die Puppe nicht, wenn eine Wand im Weg steht. */
    private static final double MIN_DISTANCE = 1.4;
    /** Ein Spieler ist 1,8 Blöcke hoch. Die Puppe ist es auch. */
    private static final double HEIGHT = 1.8;
    /** Vanilla schickt Passagier-Listen manchmal neu. Dann senden wir unsere erneut. */
    private static final int PASSENGER_RESEND_TICKS = 40;
    /** Ab dieser Entfernung passt die Puppe nicht mehr zum Spieler und wird neu gestellt. */
    private static final double TOO_FAR = 8;

    /**
     * Wo die Puppe steht und wie schnell sie sich dreht.
     *
     * Das hängt vom Bildschirm des Spielers ab — Sichtfeld, Fenstergrösse, GUI-Grösse —,
     * deshalb steht es in der config.yml und nicht als feste Zahl im Code.
     *
     * @param sideAngle    Grad seitlich, 0 = genau vor dem Spieler, positiv nach links
     * @param distance     Abstand in Blöcken
     * @param heightOffset Höhenversatz in Blöcken
     * @param turnPerTick  Grad pro Tick, 0 = keine Drehung
     */
    public record Placement(float sideAngle, double distance, double heightOffset, float turnPerTick) {
    }

    private final Player viewer;
    private final Placement placement;
    private final Mannequin entity;
    /** Fester Standplatz. Der Yaw darin ist die Blickrichtung zum Spieler. */
    private final Location stand;
    private final List<FakeCosmetic> attached = new ArrayList<>();

    private float yaw;
    private int age;
    /** Worauf es bei dieser Vorschau ankommt. Bestimmt, welche Seite der Puppe zuerst zu sehen ist. */
    private CosmeticSlot focus;

    public PreviewStand(Player viewer, Placement placement) {
        this.viewer = viewer;
        this.placement = placement;
        this.stand = findSpot(viewer, placement);
        this.yaw = stand.getYaw();
        this.entity = Packets.createMannequin(stand.getWorld(), viewer.getPlayerProfile());
        spawn();
    }

    // ------------------------------------------------------------------ Standplatz

    /**
     * Sucht einen freien Platz vor dem Spieler. Steht dort eine Wand, rückt die Puppe näher
     * heran, statt in den Blöcken zu verschwinden.
     */
    private static Location findSpot(Player viewer, Placement placement) {
        World world = viewer.getWorld();
        Location eye = viewer.getLocation();
        float direction = eye.getYaw() - placement.sideAngle();
        double dx = -Math.sin(Math.toRadians(direction));
        double dz = Math.cos(Math.toRadians(direction));
        double y = eye.getY() + placement.heightOffset();
        Location closest = null;
        for (double distance = placement.distance(); distance >= MIN_DISTANCE; distance -= 0.2) {
            closest = new Location(world, eye.getX() + dx * distance, y, eye.getZ() + dz * distance,
                    direction + 180f, 0f);
            if (free(closest)) {
                return closest;
            }
        }
        return closest;
    }

    /** Frei heißt: zwei Blöcke hoch nichts im Weg, sonst steckt die Puppe in der Wand. */
    private static boolean free(Location spot) {
        return spot.getBlock().isPassable() && spot.clone().add(0, 1, 0).getBlock().isPassable();
    }

    /** Passt die Puppe noch zum Spieler, oder hat er sich inzwischen wegbewegt? */
    public boolean stillFits(Player player) {
        return player.getWorld().equals(stand.getWorld())
                && player.getLocation().distanceSquared(stand) < TOO_FAR * TOO_FAR;
    }

    // ------------------------------------------------------------------ Anzeigen

    private void spawn() {
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        packets.add(Packets.spawn(entity, stand.getX(), stand.getY(), stand.getZ(), yaw, 0f));
        List<SynchedEntityData.DataValue<?>> data = entity.getEntityData().getNonDefaultValues();
        if (data != null) {
            packets.add(new ClientboundSetEntityDataPacket(entity.getId(), data));
        }
        Packets.send(viewer, new ClientboundBundlePacket(packets));
        Packets.send(viewer, Packets.rotateHead(entity, yaw));
    }

    /**
     * Zieht der Puppe ein ganzes Outfit an: was der Spieler gerade trägt, plus das Stück,
     * das er sich im Menü ansieht. So sieht er nicht ein einzelnes Teil, sondern den Gesamteindruck.
     *
     * @param focus der Platz, auf den es gerade ankommt — danach richtet sich die Startdrehung
     */
    public void wear(CosmeticManager manager, Map<CosmeticSlot, Cosmetic> outfit, CosmeticSlot focus) {
        clearAttached();
        ItemStack helmet = ItemStack.empty();
        for (Map.Entry<CosmeticSlot, Cosmetic> entry : outfit.entrySet()) {
            Cosmetic cosmetic = entry.getValue();
            switch (entry.getKey()) {
                case HEAD -> {
                    if (cosmetic.animation() == CosmeticAnimation.HALO) {
                        attach(new FloatingHalo(this, cosmetic));
                    } else {
                        helmet = manager.createItem(cosmetic, List.of());
                    }
                }
                case BACK -> attach(switch (cosmetic.animation()) {
                    case WINGS -> BackCosmetic.wings(this, cosmetic);
                    case BACKPACK -> BackCosmetic.backItem(this, cosmetic);
                    default -> BackCosmetic.cape(this, cosmetic);
                });
                case PET -> attach(new FakePet(this, cosmetic));
            }
        }
        Packets.send(viewer, Packets.helmet(entity.getId(), helmet));

        // Was auf dem Rücken sitzt, sieht man nur von hinten. Also dreht sich die Puppe passend —
        // aber nur bei einem Wechsel, sonst würde sie bei jedem Klick in die Startstellung springen.
        if (focus != this.focus) {
            this.focus = focus;
            turnTo(focus == CosmeticSlot.BACK ? stand.getYaw() + 180f : stand.getYaw());
        }
        sendPassengers();
    }

    /** Worauf sich die Vorschau gerade richtet. */
    public CosmeticSlot focus() {
        return focus;
    }

    private void attach(FakeCosmetic cosmetic) {
        attached.add(cosmetic);
        cosmetic.show(viewer);
    }

    private void clearAttached() {
        attached.forEach(FakeCosmetic::destroy);
        attached.clear();
    }

    public void tick(int serverTick) {
        age++;
        if (placement.turnPerTick() != 0f) {
            turnTo(yaw + placement.turnPerTick());
        }
        for (FakeCosmetic cosmetic : List.copyOf(attached)) {
            cosmetic.tick(serverTick);
        }
        if (age % PASSENGER_RESEND_TICKS == 0) {
            sendPassengers();
        }
    }

    private void turnTo(float newYaw) {
        yaw = newYaw % 360f;
        Packets.send(viewer, Packets.rotate(entity.getId(), yaw));
        Packets.send(viewer, Packets.rotateHead(entity, yaw));
    }

    private void sendPassengers() {
        int[] ids = attached.stream()
                .flatMapToInt(cosmetic -> IntStream.of(cosmetic.passengerIds(viewer)))
                .toArray();
        Packets.send(viewer, Packets.passengers(entity.getId(), ids));
    }

    public void destroy() {
        clearAttached();
        Packets.send(viewer, new ClientboundRemoveEntitiesPacket(entity.getId()));
    }

    // ------------------------------------------------------------------ Als Träger von Cosmetics

    @Override
    public UUID uniqueId() {
        return entity.getUUID();
    }

    @Override
    public int entityId() {
        return entity.getId();
    }

    @Override
    public World world() {
        return stand.getWorld();
    }

    @Override
    public Location location() {
        Location copy = stand.clone();
        copy.setYaw(yaw);
        return copy;
    }

    @Override
    public float bodyYaw() {
        return yaw;
    }

    @Override
    public double height() {
        return HEIGHT;
    }

    @Override
    public boolean sneaking() {
        return false;
    }

    @Override
    public boolean sprinting() {
        return false;
    }

    @Override
    public boolean gliding() {
        return false;
    }

    @Override
    public boolean onGround() {
        return true;
    }

    /** Partikel der Vorschau sieht nur der eine Spieler — für alle anderen gibt es die Puppe nicht. */
    @Override
    public void spawnParticle(Particle particle, Location location, int count,
                              double spreadX, double spreadY, double spreadZ, double speed) {
        viewer.spawnParticle(particle, location, count, spreadX, spreadY, spreadZ, speed);
    }
}
