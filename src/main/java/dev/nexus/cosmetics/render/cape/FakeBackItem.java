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
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ein festes Teil auf dem Rücken (Rucksack, Jetpack, Klinge ...). Wippt beim Laufen leicht mit
 * und neigt sich beim Rennen. Hat es eine Partikel-Aura, sprüht es beim Springen und Fallen kräftig
 * (z. B. Jetpack-Flammen), sonst nur ab und zu.
 */
final class FakeBackItem implements BackPiece {

    private static final float SHOULDER_OFFSET = -0.62f;
    private static final float SNEAK_SHOULDER_OFFSET = -0.5f;
    private static final float BACK_OFFSET = -0.13f;
    private static final float BASE_SCALE = 0.62f;
    private static final float HIDE_OWN_PITCH = 50f;

    private final Player wearer;
    private final boolean ownView;
    private final float scale;
    private final Particle aura;
    private final Display.ItemDisplay entity;
    private final ItemDisplay view;
    private final Set<UUID> viewers = new HashSet<>();

    private Location lastLocation;
    private double bobPhase;
    private double lean;
    private int age;

    FakeBackItem(Player wearer, Cosmetic cosmetic, boolean ownView) {
        this.wearer = wearer;
        this.ownView = ownView;
        this.scale = (float) (BASE_SCALE * cosmetic.scale());
        this.aura = cosmetic.aura();
        this.entity = Packets.createItemDisplay(wearer.getWorld(), cosmetic.model());
        this.view = (ItemDisplay) entity.getBukkitEntity();
        view.setInterpolationDuration(2);
        if (cosmetic.glowing()) {
            view.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        }
        this.lastLocation = wearer.getLocation();
        applyPose(0);
    }

    @Override
    public void show(Player viewer) {
        viewers.add(viewer.getUniqueId());
        Location location = wearer.getLocation();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        packets.add(Packets.spawn(entity, location.getX(), location.getY() + wearer.getHeight(), location.getZ()));
        List<SynchedEntityData.DataValue<?>> data = entity.getEntityData().getNonDefaultValues();
        if (data != null) {
            packets.add(new ClientboundSetEntityDataPacket(entity.getId(), data));
        }
        Packets.send(viewer, new ClientboundBundlePacket(packets));
    }

    @Override
    public void hide(Player viewer) {
        if (viewers.remove(viewer.getUniqueId())) {
            Packets.send(viewer, new ClientboundRemoveEntitiesPacket(entity.getId()));
        }
    }

    @Override
    public void forget(UUID viewer) {
        viewers.remove(viewer);
    }

    @Override
    public void destroy() {
        sendToViewers(new ClientboundRemoveEntitiesPacket(entity.getId()));
        viewers.clear();
    }

    @Override
    public int[] entityIds() {
        return new int[]{entity.getId()};
    }

    @Override
    public void tick() {
        age++;
        Location now = wearer.getLocation();
        double speed = Math.hypot(now.getX() - lastLocation.getX(), now.getZ() - lastLocation.getZ());
        double dy = now.getY() - lastLocation.getY();
        lastLocation = now;
        if (speed > 2) {
            speed = 0;
        }

        bobPhase += 0.2 + Math.min(speed * 3, 0.5);
        double bob = Math.sin(bobPhase * 2) * Math.min(speed * 0.25, 0.03);
        double targetLean = wearer.isSprinting() ? 12 : speed > 0.05 ? 5 : 0;
        lean += (targetLean - lean) * 0.2;
        applyPose(bob);
        emitAura(dy);

        List<SynchedEntityData.DataValue<?>> dirty = entity.getEntityData().packDirty();
        if (dirty != null) {
            sendToViewers(new ClientboundSetEntityDataPacket(entity.getId(), dirty));
        }
    }

    private void applyPose(double bob) {
        boolean hidden = ownView && wearer.getLocation().getPitch() > HIDE_OWN_PITCH;
        float shoulder = wearer.isSneaking() ? SNEAK_SHOULDER_OFFSET : SHOULDER_OFFSET;
        view.setTransformationMatrix(new Matrix4f()
                .rotateY((float) Math.toRadians(-wearer.getBodyYaw()))
                .translate(0, (float) (shoulder + bob), BACK_OFFSET)
                .rotateX((float) Math.toRadians(lean))
                .scale(hidden ? 0.001f : scale));
        view.setInterpolationDelay(0);
    }

    /** Beim Springen/Fallen kräftig nach unten sprühen (Jetpack), sonst ab und zu ein Funke. */
    private void emitAura(double dy) {
        if (aura == null || ownView) {
            return;
        }
        boolean airborne = Math.abs(dy) > 0.05 || wearer.isGliding();
        if (!airborne && age % 6 != 0) {
            return;
        }
        double yaw = Math.toRadians(wearer.getBodyYaw());
        double back = 0.35 * scale / BASE_SCALE;
        double height = wearer.getHeight() + SHOULDER_OFFSET - 0.35 * scale / BASE_SCALE;
        Vector right = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vector backward = new Vector(Math.sin(yaw), 0, -Math.cos(yaw));
        for (int side : new int[]{-1, 1}) {
            Location nozzle = wearer.getLocation().add(backward.clone().multiply(back))
                    .add(right.clone().multiply(0.12 * side * scale / BASE_SCALE)).add(0, height, 0);
            if (airborne) {
                // count 0 = Richtung statt Streuung: Flammen schießen nach unten
                wearer.getWorld().spawnParticle(aura, nozzle, 0, 0, -1, 0, 0.15);
            } else {
                wearer.getWorld().spawnParticle(aura, nozzle, 1, 0.03, 0.03, 0.03, 0.005);
            }
        }
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
