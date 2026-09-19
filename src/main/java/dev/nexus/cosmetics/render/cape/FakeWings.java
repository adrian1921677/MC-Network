package dev.nexus.cosmetics.render.cape;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.render.Packets;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
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
 * Zwei Flügel auf dem Rücken, die nur als Netzwerk-Pakete existieren.
 *
 * Verhalten:
 * - Im Stand: gemächlicher Flügelschlag, halb geöffnet.
 * - Beim Laufen: etwas kräftiger. Beim Rennen: nach hinten angelegt.
 * - Beim Fallen oder Gleiten mit Elytra: weit ausgebreitet, kräftiger Schlag.
 * - Beim Schleichen: eng zusammengefaltet.
 */
final class FakeWings implements BackPiece {

    // Position am Rücken (in Blöcken, relativ zum Passagier-Punkt über dem Kopf; +x = links)
    private static final float SHOULDER_OFFSET = -0.5f;
    private static final float SNEAK_SHOULDER_OFFSET = -0.38f;
    private static final float BACK_OFFSET = -0.2f;
    private static final float SPINE_GAP = 0.05f;
    private static final float SCALE = 0.8f;
    private static final float HIDE_OWN_WINGS_PITCH = 50f;

    private final Player wearer;
    private final boolean ownView;
    private final Display.ItemDisplay wingA;
    private final Display.ItemDisplay wingB;
    private final ItemDisplay viewA;
    private final ItemDisplay viewB;
    private final Set<UUID> viewers = new HashSet<>();

    // Zustand der Animation (Grad)
    private double sweep = 45;       // 0 = seitlich ausgebreitet, 90 = ganz nach hinten gefaltet
    private double amplitude = 8;
    private double flapSpeed = 0.12;
    private double phase;
    private Location lastLocation;

    FakeWings(Player wearer, Cosmetic cosmetic, boolean ownView) {
        this.wearer = wearer;
        this.ownView = ownView;
        this.wingA = Packets.createItemDisplay(wearer.getWorld(), cosmetic.model("wing_a"));
        this.wingB = Packets.createItemDisplay(wearer.getWorld(), cosmetic.model("wing_b"));
        this.viewA = (ItemDisplay) wingA.getBukkitEntity();
        this.viewB = (ItemDisplay) wingB.getBukkitEntity();
        for (ItemDisplay view : List.of(viewA, viewB)) {
            view.setInterpolationDuration(2);
            if (cosmetic.glowing()) {
                view.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            }
        }
        this.lastLocation = wearer.getLocation();
        applyPose();
    }

    @Override
    public void show(Player viewer) {
        viewers.add(viewer.getUniqueId());
        Location location = wearer.getLocation();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        for (Display.ItemDisplay wing : List.of(wingA, wingB)) {
            packets.add(Packets.spawn(wing, location.getX(), location.getY() + wearer.getHeight(), location.getZ()));
            List<SynchedEntityData.DataValue<?>> data = wing.getEntityData().getNonDefaultValues();
            if (data != null) {
                packets.add(new ClientboundSetEntityDataPacket(wing.getId(), data));
            }
        }
        Packets.send(viewer, new ClientboundBundlePacket(packets));
    }

    @Override
    public void hide(Player viewer) {
        if (viewers.remove(viewer.getUniqueId())) {
            Packets.send(viewer, new ClientboundRemoveEntitiesPacket(entityIds()));
        }
    }

    @Override
    public void forget(UUID viewer) {
        viewers.remove(viewer);
    }

    @Override
    public void destroy() {
        sendToViewers(new ClientboundRemoveEntitiesPacket(entityIds()));
        viewers.clear();
    }

    @Override
    public int[] entityIds() {
        return new int[]{wingA.getId(), wingB.getId()};
    }

    @Override
    public void tick() {
        Location now = wearer.getLocation();
        double dx = now.getX() - lastLocation.getX();
        double dy = now.getY() - lastLocation.getY();
        double dz = now.getZ() - lastLocation.getZ();
        lastLocation = now;
        double speed = Math.hypot(dx, dz);
        if (speed > 2) {
            speed = 0; // Teleport
        }

        // Ziel-Haltung je nach Situation
        double targetSweep;
        double targetAmplitude;
        double targetSpeed;
        if (wearer.isSneaking()) {
            targetSweep = 80;
            targetAmplitude = 3;
            targetSpeed = 0.08;
        } else if (wearer.isGliding() || dy < -0.3) {
            targetSweep = 5;
            targetAmplitude = 22;
            targetSpeed = 0.45;
        } else if (wearer.isSprinting()) {
            targetSweep = 62;
            targetAmplitude = 10;
            targetSpeed = 0.32;
        } else if (speed > 0.05) {
            targetSweep = 42;
            targetAmplitude = 15;
            targetSpeed = 0.24;
        } else {
            targetSweep = 48;
            targetAmplitude = 8;
            targetSpeed = 0.12;
        }
        sweep += (targetSweep - sweep) * 0.15;
        amplitude += (targetAmplitude - amplitude) * 0.15;
        flapSpeed += (targetSpeed - flapSpeed) * 0.15;
        phase += flapSpeed;

        applyPose();
        for (Display.ItemDisplay wing : List.of(wingA, wingB)) {
            List<SynchedEntityData.DataValue<?>> dirty = wing.getEntityData().packDirty();
            if (dirty != null) {
                sendToViewers(new ClientboundSetEntityDataPacket(wing.getId(), dirty));
            }
        }
    }

    private void applyPose() {
        boolean hidden = ownView && wearer.getLocation().getPitch() > HIDE_OWN_WINGS_PITCH;
        float shoulder = wearer.isSneaking() ? SNEAK_SHOULDER_OFFSET : SHOULDER_OFFSET;
        double flap = Math.sin(phase) * amplitude;          // vor und zurück
        double lift = 12 + Math.sin(phase) * amplitude * 0.7; // Spitzen hoch und runter
        float scale = hidden ? 0.001f : SCALE;
        float bodyYaw = (float) Math.toRadians(-wearer.getBodyYaw());

        // Flügel A zeigt nach rechts (-x), Flügel B nach links (+x)
        viewA.setTransformationMatrix(new Matrix4f()
                .rotateY(bodyYaw)
                .translate(-SPINE_GAP, shoulder, BACK_OFFSET)
                .rotateY((float) Math.toRadians(-(sweep + flap)))
                .rotateZ((float) Math.toRadians(-lift))
                .scale(scale));
        viewB.setTransformationMatrix(new Matrix4f()
                .rotateY(bodyYaw)
                .translate(SPINE_GAP, shoulder, BACK_OFFSET)
                .rotateY((float) Math.toRadians(sweep + flap))
                .rotateZ((float) Math.toRadians(lift))
                .scale(scale));
        viewA.setInterpolationDelay(0);
        viewB.setInterpolationDelay(0);
    }

    private void sendToViewers(Packet<?> packet) {
        for (UUID uuid : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer != null) {
                Packets.send(viewer, packet);
            }
        }
    }
}
