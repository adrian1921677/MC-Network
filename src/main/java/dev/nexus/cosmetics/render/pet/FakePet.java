package dev.nexus.cosmetics.render.pet;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.cosmetic.CosmeticAnimation;
import dev.nexus.cosmetics.render.FakeCosmetic;
import dev.nexus.cosmetics.render.Packets;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ein Haustier, das neben dem Besitzer schwebt. Existiert nur als Netzwerk-Paket.
 *
 * Es folgt einem Punkt schräg hinter der rechten Schulter, weich verzögert, und dreht sich
 * in Laufrichtung. Drachen bestehen aus drei Teilen (Körper + zwei Flügel), damit die Flügel
 * einzeln schlagen können.
 */
public final class FakePet implements FakeCosmetic {

    private static final float SCALE = 0.55f;

    // Wo das Haustier relativ zum Besitzer schwebt (in Blöcken)
    private static final double SIDE = 0.85;
    private static final double BEHIND = 0.35;
    private static final double HEIGHT = 1.75;

    private static final double FOLLOW_SPEED = 0.16;
    private static final double TELEPORT_DISTANCE = 10;

    // Flügel-Gelenke (Modell-Einheiten, 16 = 1 Block vor der Skalierung)
    private static final float WING_JOINT_X = 3 / 16f;
    private static final float WING_JOINT_Y = 2 / 16f;
    private static final float WING_JOINT_Z = -0.5f / 16f;

    private record Part(Display.ItemDisplay entity, ItemDisplay view) {
    }

    private final Player owner;
    private final CosmeticAnimation animation;
    private final List<Part> parts = new ArrayList<>();
    private final Set<UUID> viewers = new HashSet<>();

    private double x;
    private double y;
    private double z;
    private float yaw;
    private double speed;
    private double flapPhase;
    private int age;

    public FakePet(Player owner, Cosmetic cosmetic) {
        this.owner = owner;
        this.animation = cosmetic.animation();

        if (animation == CosmeticAnimation.WINGS) {
            addPart(cosmetic, "body");
            addPart(cosmetic, "wing_a");
            addPart(cosmetic, "wing_b");
        } else {
            addPart(cosmetic, null);
        }

        Vec3 start = targetPosition();
        x = start.x;
        y = start.y;
        z = start.z;
        yaw = owner.getBodyYaw();
        applyTransformations();
    }

    private void addPart(Cosmetic cosmetic, String suffix) {
        Display.ItemDisplay entity = Packets.createItemDisplay(owner.getWorld(),
                suffix == null ? cosmetic.model() : cosmetic.model(suffix));
        ItemDisplay view = (ItemDisplay) entity.getBukkitEntity();
        view.setInterpolationDuration(3);
        view.setTeleportDuration(3);
        if (cosmetic.glowing()) {
            view.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        }
        parts.add(new Part(entity, view));
    }

    // ------------------------------------------------------------------ Sichtbarkeit

    @Override
    public void show(Player viewer) {
        viewers.add(viewer.getUniqueId());
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        for (Part part : parts) {
            packets.add(new ClientboundAddEntityPacket(part.entity().getId(), part.entity().getUUID(),
                    x, y, z, 0f, 0f, EntityTypes.ITEM_DISPLAY, 0, Vec3.ZERO, 0d));
            List<SynchedEntityData.DataValue<?>> data = part.entity().getEntityData().getNonDefaultValues();
            if (data != null) {
                packets.add(new ClientboundSetEntityDataPacket(part.entity().getId(), data));
            }
        }
        Packets.send(viewer, new ClientboundBundlePacket(packets));
    }

    @Override
    public void hide(Player viewer) {
        if (viewers.remove(viewer.getUniqueId())) {
            Packets.send(viewer, removePacket());
        }
    }

    @Override
    public void forget(UUID viewer) {
        viewers.remove(viewer);
    }

    @Override
    public void destroy() {
        ClientboundRemoveEntitiesPacket packet = removePacket();
        sendToViewers(packet);
        viewers.clear();
    }

    // ------------------------------------------------------------------ Bewegung

    @Override
    public void tick(int serverTick) {
        age++;

        // Weich zum Zielpunkt neben dem Besitzer gleiten
        Vec3 target = targetPosition();
        double dx = target.x - x;
        double dy = target.y - y;
        double dz = target.z - z;
        if (dx * dx + dy * dy + dz * dz > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            // Besitzer hat sich teleportiert: direkt hinterher
            x = target.x;
            y = target.y;
            z = target.z;
            speed = 0;
        } else {
            double stepX = dx * FOLLOW_SPEED;
            double stepZ = dz * FOLLOW_SPEED;
            x += stepX;
            y += dy * FOLLOW_SPEED;
            z += stepZ;
            speed = Math.hypot(stepX, stepZ);

            // Beim Fliegen in Flugrichtung schauen, im Stand dorthin, wo der Besitzer hinschaut
            float desiredYaw = speed > 0.04
                    ? (float) Math.toDegrees(Math.atan2(-stepX, stepZ))
                    : owner.getLocation().getYaw();
            yaw += wrapDegrees(desiredYaw - yaw) * 0.15f;
        }

        applyTransformations();

        PositionMoveRotation position = new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, 0f, 0f);
        for (Part part : parts) {
            sendToViewers(ClientboundTeleportEntityPacket.teleport(part.entity().getId(), position, Set.of(), false));
            List<SynchedEntityData.DataValue<?>> dirty = part.entity().getEntityData().packDirty();
            if (dirty != null) {
                sendToViewers(new ClientboundSetEntityDataPacket(part.entity().getId(), dirty));
            }
        }
    }

    /** Punkt schräg hinter der rechten Schulter des Besitzers. */
    private Vec3 targetPosition() {
        Location location = owner.getLocation();
        double bodyYaw = Math.toRadians(owner.getBodyYaw());
        double forwardX = -Math.sin(bodyYaw);
        double forwardZ = Math.cos(bodyYaw);
        double rightX = -Math.cos(bodyYaw);
        double rightZ = -Math.sin(bodyYaw);
        return new Vec3(
                location.getX() + rightX * SIDE - forwardX * BEHIND,
                location.getY() + HEIGHT,
                location.getZ() + rightZ * SIDE - forwardZ * BEHIND);
    }

    private void applyTransformations() {
        double bob;
        double roll;
        double flap = 0;
        double tilt = Math.min(speed * 140, 20); // beim Fliegen nach vorne neigen

        if (animation == CosmeticAnimation.WINGS) {
            flapPhase += 0.32 + Math.min(speed * 3, 0.35); // schneller schlagen beim Fliegen
            flap = 15 + Math.sin(flapPhase) * 38;
            bob = Math.cos(flapPhase) * 0.04; // Körper hebt sich beim Flügelschlag
            roll = 0;
        } else {
            bob = Math.sin(age * 0.08) * 0.08;
            roll = Math.sin(age * 0.05) * 8;
        }

        Matrix4f base = new Matrix4f()
                .translate(0, (float) bob, 0)
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(tilt))
                .rotateZ((float) Math.toRadians(roll))
                .scale(SCALE);

        parts.get(0).view().setTransformationMatrix(base);
        if (animation == CosmeticAnimation.WINGS) {
            // Die Flügel drehen sich um ihr Gelenk an der Schulter des Drachen
            parts.get(1).view().setTransformationMatrix(new Matrix4f(base)
                    .translate(-WING_JOINT_X, WING_JOINT_Y, WING_JOINT_Z)
                    .rotateZ((float) Math.toRadians(-flap)));
            parts.get(2).view().setTransformationMatrix(new Matrix4f(base)
                    .translate(WING_JOINT_X, WING_JOINT_Y, WING_JOINT_Z)
                    .rotateZ((float) Math.toRadians(flap)));
        }
        for (Part part : parts) {
            part.view().setInterpolationDelay(0);
        }
    }

    // ------------------------------------------------------------------ Hilfsmethoden

    private ClientboundRemoveEntitiesPacket removePacket() {
        return new ClientboundRemoveEntitiesPacket(parts.stream().mapToInt(part -> part.entity().getId()).toArray());
    }

    private void sendToViewers(Packet<?> packet) {
        for (UUID uuid : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer != null) {
                Packets.send(viewer, packet);
            }
        }
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360f;
        if (degrees >= 180f) degrees -= 360f;
        if (degrees < -180f) degrees += 360f;
        return degrees;
    }
}
