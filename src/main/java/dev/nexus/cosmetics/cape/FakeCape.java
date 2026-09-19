package dev.nexus.cosmetics.cape;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Eine Cape, die nur als Netzwerk-Paket existiert. Der Server selbst kennt sie nicht als echtes Entity.
 * Dadurch stört sie keine anderen Plugins (Teleports, Welt-Wechsel, Minigames ...).
 *
 * Die Cape besteht aus mehreren Segmenten (oben, Mitte, unten). Jedes Segment ist ein eigenes
 * Item-Display und hängt am Ende des vorherigen. Jedes Segment schwingt mit einer eigenen Feder,
 * dadurch biegt sich die Cape wie Stoff und schwingt nach.
 */
final class FakeCape {

    // --- Aufbau (Modell-Einheiten: 16 = ganze Cape-Höhe) ---
    private static final int[] SEGMENT_HEIGHTS = {5, 5, 6};
    private static final float SCALE = 0.9f;

    // --- Position am Körper (in Blöcken, relativ zum Passagier-Punkt über dem Kopf) ---
    private static final float SHOULDER_OFFSET = -0.45f;
    private static final float SNEAK_SHOULDER_OFFSET = -0.35f;
    private static final float BACK_OFFSET = -0.17f;

    // --- Feder-Physik pro Segment: Steifigkeit und Dämpfung (klein = weicher, schwingt länger) ---
    private static final float[] STIFFNESS = {0.22f, 0.16f, 0.12f};
    private static final float[] DAMPING = {0.30f, 0.26f, 0.22f};

    // --- Ab diesem Blickwinkel nach unten wird die eigene Cape ausgeblendet ---
    private static final float HIDE_OWN_CAPE_PITCH = 50f;

    private final Player wearer;
    /** true = diese Instanz sieht nur der Träger selbst. */
    private final boolean ownView;
    private final Display.ItemDisplay[] segments;
    private final ItemDisplay[] segmentViews;
    private final Set<UUID> viewers = new HashSet<>();

    // Physik-Zustand (Winkel in Grad, von der Senkrechten nach hinten gemessen)
    private final float[] angle = new float[SEGMENT_HEIGHTS.length];
    private final float[] velocity = new float[SEGMENT_HEIGHTS.length];
    private float roll;
    private float rollVelocity;

    private Location lastLocation;
    private float lastBodyYaw;
    private int age;

    FakeCape(Player wearer, Cosmetic cosmetic, boolean ownView) {
        this.wearer = wearer;
        this.ownView = ownView;
        this.segments = new Display.ItemDisplay[SEGMENT_HEIGHTS.length];
        this.segmentViews = new ItemDisplay[SEGMENT_HEIGHTS.length];

        for (int i = 0; i < segments.length; i++) {
            Display.ItemDisplay entity = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,
                    ((CraftWorld) wearer.getWorld()).getHandle());
            entity.setItemStack(CraftItemStack.asNMSCopy(segmentItem(cosmetic, i)));

            // Über die Bukkit-Ansicht lassen sich die Display-Werte bequem setzen
            ItemDisplay view = (ItemDisplay) entity.getBukkitEntity();
            view.setInterpolationDuration(2);
            if (cosmetic.glowing()) {
                view.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            }
            segments[i] = entity;
            segmentViews[i] = view;
        }

        this.lastLocation = wearer.getLocation();
        this.lastBodyYaw = wearer.getBodyYaw();
        applyTransformations();
    }

    private static ItemStack segmentItem(Cosmetic cosmetic, int index) {
        ItemStack item = ItemStack.of(Material.PAPER);
        NamespacedKey model = new NamespacedKey(cosmetic.model().getNamespace(), cosmetic.model().getKey() + "_" + index);
        item.editMeta(meta -> meta.setItemModel(model));
        return item;
    }

    // ------------------------------------------------------------------ Sichtbarkeit

    void show(Player viewer) {
        viewers.add(viewer.getUniqueId());
        Location loc = wearer.getLocation();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        for (Display.ItemDisplay entity : segments) {
            packets.add(new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(),
                    loc.getX(), loc.getY() + wearer.getHeight(), loc.getZ(), 0f, 0f,
                    EntityTypes.ITEM_DISPLAY, 0, Vec3.ZERO, 0d));
            List<SynchedEntityData.DataValue<?>> data = entity.getEntityData().getNonDefaultValues();
            if (data != null) {
                packets.add(new ClientboundSetEntityDataPacket(entity.getId(), data));
            }
        }
        packets.add(passengerPacket());
        send(viewer, new ClientboundBundlePacket(packets));
    }

    void hide(Player viewer) {
        if (viewers.remove(viewer.getUniqueId())) {
            send(viewer, removePacket());
        }
    }

    /** Entfernt einen Zuschauer, ohne ein Paket zu senden (z. B. weil er offline ist). */
    void forget(UUID viewer) {
        viewers.remove(viewer);
    }

    void destroy() {
        ClientboundRemoveEntitiesPacket packet = removePacket();
        forEachViewer(viewer -> send(viewer, packet));
        viewers.clear();
    }

    /** Nach einem Respawn oder Weltwechsel neu anzeigen. */
    void respawnFor(Player viewer) {
        hide(viewer);
        show(viewer);
    }

    /**
     * Vanilla schickt die Passagier-Liste des Trägers manchmal neu (z. B. wenn etwas anderes aufsteigt).
     * Dann wäre unsere Cape "abgestiegen". Deshalb senden wir sie regelmäßig erneut.
     */
    void resendPassengers() {
        ClientboundSetPassengersPacket packet = passengerPacket();
        forEachViewer(viewer -> send(viewer, packet));
    }

    // ------------------------------------------------------------------ Physik

    void tick() {
        age++;
        Location now = wearer.getLocation();
        double dx = now.getX() - lastLocation.getX();
        double dy = now.getY() - lastLocation.getY();
        double dz = now.getZ() - lastLocation.getZ();
        lastLocation = now;

        // Teleport erkannt: diese Bewegung ignorieren statt die Cape wild herumzuschleudern
        if (dx * dx + dy * dy + dz * dz > 4) {
            dx = dy = dz = 0;
        }

        float bodyYaw = wearer.getBodyYaw();
        float turnSpeed = wrapDegrees(bodyYaw - lastBodyYaw);
        lastBodyYaw = bodyYaw;

        // Bewegung aufteilen in "nach vorne" und "zur Seite"
        double yaw = Math.toRadians(bodyYaw);
        double forward = -Math.sin(yaw) * dx + Math.cos(yaw) * dz;
        double right = -Math.cos(yaw) * dx - Math.sin(yaw) * dz;
        double speed = Math.max(0, forward);

        // Oberes Segment: Grundwinkel aus Laufgeschwindigkeit, Fallen und Schleichen
        double target = 4 + Math.min(speed * 260, 80);
        if (dy < -0.08) {
            target += Math.min(-dy * 90, 35);
        }
        if (wearer.isSneaking()) {
            target += 22;
        }
        target += Math.sin(age * 0.09) * 1.5;

        for (int i = 0; i < angle.length; i++) {
            if (i > 0) {
                // Untere Segmente hängen dem oberen hinterher und flattern zusätzlich im Fahrtwind
                double flutter = Math.min(speed * 45, 12) * Math.sin(age * 0.85 - i * 1.4);
                double idle = Math.sin(age * 0.11 - i * 0.9) * 2.5;
                target = angle[i - 1] + Math.min(speed * 70, 22) + flutter + idle;
            }
            velocity[i] += (float) ((target - angle[i]) * STIFFNESS[i]) - velocity[i] * DAMPING[i];
            angle[i] = Math.clamp(angle[i] + velocity[i], 0f, 115f); // nie in den Körper hinein
        }

        // Seitliches Schwingen beim Drehen und Seitwärtslaufen
        double rollTarget = Math.clamp(turnSpeed * 1.6 + right * 90, -35, 35);
        rollVelocity += (float) ((rollTarget - roll) * 0.15) - rollVelocity * 0.3f;
        roll += rollVelocity;

        applyTransformations();

        for (Display.ItemDisplay entity : segments) {
            List<SynchedEntityData.DataValue<?>> dirty = entity.getEntityData().packDirty();
            if (dirty != null && !viewers.isEmpty()) {
                ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(entity.getId(), dirty);
                forEachViewer(viewer -> send(viewer, packet));
            }
        }
    }

    /**
     * Rechnet für jedes Segment aus, wo es hängt:
     * mit dem Körper drehen → zu den Schultern → seitlich kippen → Segment für Segment nach hinten schwingen.
     */
    private void applyTransformations() {
        boolean hidden = ownView && wearer.getLocation().getPitch() > HIDE_OWN_CAPE_PITCH;
        float shoulder = wearer.isSneaking() ? SNEAK_SHOULDER_OFFSET : SHOULDER_OFFSET;

        Matrix4f joint = new Matrix4f()
                .rotateY((float) Math.toRadians(-wearer.getBodyYaw()))
                .translate(0, shoulder, BACK_OFFSET)
                .rotateZ((float) Math.toRadians(roll));

        float previousAngle = 0;
        for (int i = 0; i < segments.length; i++) {
            joint.rotateX((float) Math.toRadians(angle[i] - previousAngle));
            previousAngle = angle[i];

            Matrix4f segmentMatrix = new Matrix4f(joint)
                    .scale(hidden ? 0.001f : SCALE)
                    .translate(0, -0.5f, 0);
            segmentViews[i].setTransformationMatrix(segmentMatrix);
            segmentViews[i].setInterpolationDelay(0);

            // Gelenk ans untere Ende dieses Segments verschieben
            joint.translate(0, -SEGMENT_HEIGHTS[i] / 16f * SCALE, 0);
        }
    }

    // ------------------------------------------------------------------ Hilfsmethoden

    private ClientboundSetPassengersPacket passengerPacket() {
        ServerPlayer handle = ((CraftPlayer) wearer).getHandle();
        List<Entity> realPassengers = handle.getPassengers();
        int[] ids = new int[realPassengers.size() + segments.length];
        for (int i = 0; i < realPassengers.size(); i++) {
            ids[i] = realPassengers.get(i).getId();
        }
        for (int i = 0; i < segments.length; i++) {
            ids[realPassengers.size() + i] = segments[i].getId();
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(handle.getId());
            buffer.writeVarIntArray(ids);
            return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private ClientboundRemoveEntitiesPacket removePacket() {
        int[] ids = new int[segments.length];
        for (int i = 0; i < segments.length; i++) {
            ids[i] = segments[i].getId();
        }
        return new ClientboundRemoveEntitiesPacket(ids);
    }

    private void forEachViewer(Consumer<Player> action) {
        for (UUID uuid : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer != null) {
                action.accept(viewer);
            }
        }
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360f;
        if (degrees >= 180f) degrees -= 360f;
        if (degrees < -180f) degrees += 360f;
        return degrees;
    }

    private static void send(Player player, Packet<?> packet) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        if (handle.connection != null) {
            handle.connection.send(packet);
        }
    }
}
