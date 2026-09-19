package dev.nexus.cosmetics.render.pet;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.render.FakeCosmetic;
import dev.nexus.cosmetics.render.Packets;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Ein Haustier, das nur als Netzwerk-Paket existiert.
 *
 * FakePet kümmert sich um das Technische: Position, Folgen, Pakete, Sichtbarkeit.
 * Wie sich das Tier bewegt und aussieht, entscheidet sein PetAnimator.
 */
public final class FakePet implements FakeCosmetic {

    // Platz beim Fliegen: schräg hinter der rechten Schulter
    private static final double FLY_SIDE = 0.85;
    private static final double FLY_BEHIND = 0.35;
    private static final double FLY_HEIGHT = 1.75;
    // Platz am Boden: neben dem rechten Fuß
    private static final double GROUND_SIDE = 0.95;
    private static final double GROUND_BEHIND = 0.15;
    // Platz auf der Schulter (relativ zum Passagier-Punkt über dem Kopf; +x = links)
    private static final float SHOULDER_X = -0.34f;
    private static final float SHOULDER_Y = -0.45f;
    private static final float SNEAK_SHOULDER_Y = -0.33f;

    private static final double FOLLOW_SPEED = 0.16;
    private static final double TELEPORT_DISTANCE = 10;

    private static final class Part {
        final Display.ItemDisplay entity;
        final ItemDisplay view;
        String model;

        Part(Display.ItemDisplay entity, ItemDisplay view, String model) {
            this.entity = entity;
            this.view = view;
            this.model = model;
        }
    }

    private final Player owner;
    private final Cosmetic cosmetic;
    private final PetAnimator animator;
    private final PetState state = new PetState();
    private final List<Part> parts = new ArrayList<>();
    private final Set<UUID> viewers = new HashSet<>();

    private double x;
    private double y;
    private double z;
    private float yaw;
    private double groundY;
    private int ownerAirTicks;
    private Location lastOwnerLocation;

    public FakePet(Player owner, Cosmetic cosmetic) {
        this.owner = owner;
        this.cosmetic = cosmetic;
        this.animator = PetAnimators.create(cosmetic.animation());

        List<String> suffixes = animator.parts();
        for (int i = 0; i < suffixes.size(); i++) {
            String suffix = suffixes.get(i);
            Display.ItemDisplay entity = Packets.createItemDisplay(owner.getWorld(), modelKey(suffix));
            ItemDisplay view = (ItemDisplay) entity.getBukkitEntity();
            view.setInterpolationDuration(animator.interpolation(i));
            view.setTeleportDuration(3);
            if (cosmetic.glowing()) {
                view.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            }
            parts.add(new Part(entity, view, suffix));
        }

        lastOwnerLocation = owner.getLocation();
        groundY = lastOwnerLocation.getY();
        Vec3 start = targetPosition();
        x = start.x;
        y = start.y;
        z = start.z;
        yaw = owner.getBodyYaw();
        applyPose();
    }

    private NamespacedKey modelKey(String suffix) {
        return suffix == null ? cosmetic.model() : cosmetic.model(suffix);
    }

    private boolean onShoulder() {
        return animator.mode() == PetAnimator.Mode.SHOULDER;
    }

    // ------------------------------------------------------------------ Sichtbarkeit

    @Override
    public void show(Player viewer) {
        viewers.add(viewer.getUniqueId());
        Location ownerLocation = owner.getLocation();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        for (Part part : parts) {
            packets.add(onShoulder()
                    ? Packets.spawn(part.entity, ownerLocation.getX(), ownerLocation.getY() + owner.getHeight(), ownerLocation.getZ())
                    : Packets.spawn(part.entity, x, y, z));
            List<SynchedEntityData.DataValue<?>> data = part.entity.getEntityData().getNonDefaultValues();
            if (data != null) {
                packets.add(new ClientboundSetEntityDataPacket(part.entity.getId(), data));
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
        sendToViewers(removePacket());
        viewers.clear();
    }

    @Override
    public int[] passengerIds(Player viewer) {
        // Schulter-Haustiere "reiten" auf dem Besitzer und bewegen sich so ruckelfrei mit
        return onShoulder() ? parts.stream().mapToInt(part -> part.entity.getId()).toArray() : new int[0];
    }

    // ------------------------------------------------------------------ Bewegung

    @Override
    public void tick(int serverTick) {
        updateState();

        if (!onShoulder()) {
            follow();
        }
        applyPose();

        if (!onShoulder()) {
            PositionMoveRotation position = new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, 0f, 0f);
            for (Part part : parts) {
                sendToViewers(ClientboundTeleportEntityPacket.teleport(part.entity.getId(), position, Set.of(), false));
            }
        }
        for (Part part : parts) {
            List<SynchedEntityData.DataValue<?>> dirty = part.entity.getEntityData().packDirty();
            if (dirty != null) {
                sendToViewers(new ClientboundSetEntityDataPacket(part.entity.getId(), dirty));
            }
        }

        animator.effects(state, this::spawnParticle);
    }

    /** Beobachtet den Besitzer: bewegt er sich, springt er, schleicht er ...? */
    private void updateState() {
        state.age++;
        Location now = owner.getLocation();
        double dx = now.getX() - lastOwnerLocation.getX();
        double dz = now.getZ() - lastOwnerLocation.getZ();
        double dy = now.getY() - lastOwnerLocation.getY();
        lastOwnerLocation = now;

        state.ownerSpeed = Math.hypot(dx, dz);
        state.ownerVerticalSpeed = dy;
        state.ownerSneaking = owner.isSneaking();
        state.ownerSprinting = owner.isSprinting();
        boolean still = state.ownerSpeed < 0.01 && Math.abs(dy) < 0.01;
        state.ownerIdleTicks = still ? state.ownerIdleTicks + 1 : 0;

        // Bodenhöhe merken: Beim Springen soll ein Boden-Haustier nicht mit in die Luft
        @SuppressWarnings("deprecation")
        boolean onGround = owner.isOnGround();
        ownerAirTicks = onGround ? 0 : ownerAirTicks + 1;
        if (onGround || ownerAirTicks > 30) {
            groundY = now.getY();
        }
    }

    /** Weich zum Zielpunkt gleiten und sich in Laufrichtung drehen. */
    private void follow() {
        Vec3 target = targetPosition();
        double dx = target.x - x;
        double dy = target.y - y;
        double dz = target.z - z;
        if (dx * dx + dy * dy + dz * dz > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            // Besitzer hat sich teleportiert: direkt hinterher
            x = target.x;
            y = target.y;
            z = target.z;
            state.speed = 0;
            return;
        }
        double stepX = dx * FOLLOW_SPEED;
        double stepZ = dz * FOLLOW_SPEED;
        x += stepX;
        z += stepZ;
        y += dy * (animator.mode() == PetAnimator.Mode.GROUND ? 0.35 : FOLLOW_SPEED);
        state.speed = Math.hypot(stepX, stepZ);

        // Beim Laufen in Laufrichtung schauen, im Stand dorthin, wo der Besitzer hinschaut
        float desiredYaw = state.speed > 0.04
                ? (float) Math.toDegrees(Math.atan2(-stepX, stepZ))
                : owner.getLocation().getYaw();
        yaw += wrapDegrees(desiredYaw - yaw) * 0.15f;
    }

    /** Der Platz neben dem Besitzer, plus der Versatz der Animation (z. B. Kreise fliegen). */
    private Vec3 targetPosition() {
        Location location = owner.getLocation();
        double bodyYaw = Math.toRadians(owner.getBodyYaw());
        double forwardX = -Math.sin(bodyYaw);
        double forwardZ = Math.cos(bodyYaw);
        double rightX = -Math.cos(bodyYaw);
        double rightZ = -Math.sin(bodyYaw);

        boolean ground = animator.mode() == PetAnimator.Mode.GROUND;
        double side = ground ? GROUND_SIDE : FLY_SIDE;
        double behind = ground ? GROUND_BEHIND : FLY_BEHIND;
        double baseY = ground ? groundY : location.getY() + FLY_HEIGHT;

        Vector3d offset = animator.offset(state);
        double right = side + offset.x;
        double forward = -behind + offset.z;
        return new Vec3(
                location.getX() + rightX * right + forwardX * forward,
                baseY + offset.y,
                location.getZ() + rightZ * right + forwardZ * forward);
    }

    /** Lässt den Animator die Teile positionieren und tauscht bei Bedarf Modelle. */
    private void applyPose() {
        Matrix4f base;
        if (onShoulder()) {
            base = new Matrix4f()
                    .rotateY((float) Math.toRadians(-owner.getBodyYaw()))
                    .translate(SHOULDER_X, owner.isSneaking() ? SNEAK_SHOULDER_Y : SHOULDER_Y, 0.02f);
        } else {
            base = new Matrix4f().rotateY((float) Math.toRadians(-yaw));
        }

        Matrix4f[] poses = new Matrix4f[parts.size()];
        animator.pose(state, base, poses);

        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            String model = animator.model(state, i);
            if (!Objects.equals(model, part.model)) {
                part.model = model;
                Packets.setModel(part.entity, modelKey(model));
            }
            part.view.setTransformationMatrix(poses[i]);
            part.view.setInterpolationDelay(0);
        }
    }

    // ------------------------------------------------------------------ Hilfsmethoden

    private void spawnParticle(Particle particle, double right, double up, double forward, int count, double spread, double speed) {
        double baseX;
        double baseY;
        double baseZ;
        double facing;
        if (onShoulder()) {
            Location location = owner.getLocation();
            facing = Math.toRadians(owner.getBodyYaw());
            baseX = location.getX() - Math.cos(facing) * 0.34;
            baseY = location.getY() + 1.45;
            baseZ = location.getZ() - Math.sin(facing) * 0.34;
        } else {
            facing = Math.toRadians(yaw);
            baseX = x;
            baseY = y;
            baseZ = z;
        }
        double px = baseX - Math.cos(facing) * right - Math.sin(facing) * forward;
        double pz = baseZ - Math.sin(facing) * right + Math.cos(facing) * forward;
        for (UUID uuid : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer != null) {
                viewer.spawnParticle(particle, px, baseY + up, pz, count, spread, spread, spread, speed);
            }
        }
    }

    private ClientboundRemoveEntitiesPacket removePacket() {
        return new ClientboundRemoveEntitiesPacket(parts.stream().mapToInt(part -> part.entity.getId()).toArray());
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
