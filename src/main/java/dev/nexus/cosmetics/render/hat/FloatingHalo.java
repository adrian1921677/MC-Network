package dev.nexus.cosmetics.render.hat;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.render.CosmeticCarrier;
import dev.nexus.cosmetics.render.FakeCosmetic;
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
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Ein leuchtender Heiligenschein, der über dem Kopf schwebt, sich langsam dreht und funkelt.
 * Sitzt als Passagier auf dem Spieler und folgt ihm dadurch ruckelfrei.
 */
public final class FloatingHalo implements FakeCosmetic {

    /** Abstand über dem Passagier-Punkt (= Oberkante des Kopfes) in Blöcken */
    private static final float HEIGHT = 0.28f;
    private static final float BASE_SCALE = 0.55f;

    private final CosmeticCarrier wearer;
    private final Display.ItemDisplay entity;
    private final ItemDisplay view;
    private final Set<UUID> viewers = new HashSet<>();
    private final Random random = new Random();
    private final float scale;
    private final Particle aura;
    private int age;

    public FloatingHalo(CosmeticCarrier wearer, Cosmetic cosmetic) {
        this.wearer = wearer;
        this.scale = (float) (BASE_SCALE * cosmetic.scale());
        this.aura = cosmetic.aura();
        this.entity = Packets.createItemDisplay(wearer.world(), cosmetic.model());
        this.view = (ItemDisplay) entity.getBukkitEntity();
        view.setInterpolationDuration(3);
        view.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        applyPose();
    }

    @Override
    public void show(Player viewer) {
        viewers.add(viewer.getUniqueId());
        Location location = wearer.location();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        packets.add(Packets.spawn(entity, location.getX(), location.getY() + wearer.height(), location.getZ()));
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
    public int[] passengerIds(Player viewer) {
        return new int[]{entity.getId()};
    }

    @Override
    public void tick(int serverTick) {
        age++;
        applyPose();
        List<SynchedEntityData.DataValue<?>> dirty = entity.getEntityData().packDirty();
        if (dirty != null) {
            sendToViewers(new ClientboundSetEntityDataPacket(entity.getId(), dirty));
        }

        if (aura != null && age % 3 == 0) {
            wearer.spawnParticle(aura, wearer.location().add(0, wearer.height() + HEIGHT, 0),
                    2, 0.25 * scale / BASE_SCALE, 0.1, 0.25 * scale / BASE_SCALE, 0.01);
        }

        // Ab und zu ein Funkeln am Ring
        if (age % 15 == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            Location location = wearer.location().add(Math.cos(angle) * 0.22, wearer.height() + HEIGHT, Math.sin(angle) * 0.22);
            for (UUID uuid : List.copyOf(viewers)) {
                Player viewer = Bukkit.getPlayer(uuid);
                if (viewer != null) {
                    viewer.spawnParticle(Particle.END_ROD, location, 1, 0.02, 0.02, 0.02, 0.005);
                }
            }
        }
    }

    private void applyPose() {
        double bob = Math.sin(age * 0.07) * 0.04;
        view.setTransformationMatrix(new Matrix4f()
                .rotateY((float) Math.toRadians(-wearer.bodyYaw()))
                .translate(0, (float) (HEIGHT + bob), -0.04f)
                .rotateX((float) Math.toRadians(-12))   // leicht nach hinten gekippt
                .rotateY((float) Math.toRadians(age * 2.5))
                .scale(scale));
        view.setInterpolationDelay(0);
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
