package dev.nexus.cosmetics.render;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Hilfsmethoden für Paket-Cosmetics. Hier liegt der Zugriff auf Paper-Interna gebündelt,
 * damit bei Minecraft-Updates möglichst wenig Code angepasst werden muss.
 */
public final class Packets {

    private Packets() {
    }

    /** Erstellt ein Item-Display, das nie in die Welt gesetzt, sondern nur per Paket verschickt wird. */
    public static Display.ItemDisplay createItemDisplay(World world, NamespacedKey model) {
        Display.ItemDisplay entity = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, ((CraftWorld) world).getHandle());
        setModel(entity, model);
        return entity;
    }

    /** Tauscht das angezeigte Modell (z. B. schlafendes Kätzchen). */
    public static void setModel(Display.ItemDisplay entity, NamespacedKey model) {
        ItemStack item = ItemStack.of(Material.PAPER);
        item.editMeta(meta -> meta.setItemModel(model));
        entity.setItemStack(CraftItemStack.asNMSCopy(item));
    }

    /** Erstellt eine Text-Anzeige (z. B. Sprechblase), die nur per Paket verschickt wird. */
    public static Display.TextDisplay createTextDisplay(World world) {
        return new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, ((CraftWorld) world).getHandle());
    }

    /**
     * Erstellt eine Schaufensterpuppe (seit Minecraft 1.21.9 ein eigenes Entity), die nur per
     * Paket verschickt wird. Sie trägt die Haut des Spielers, dem sie gezeigt wird.
     */
    public static net.minecraft.world.entity.decoration.Mannequin createMannequin(World world,
                                                                                  com.destroystokyo.paper.profile.PlayerProfile skin) {
        net.minecraft.world.entity.decoration.Mannequin entity =
                new net.minecraft.world.entity.decoration.Mannequin(EntityTypes.MANNEQUIN, ((CraftWorld) world).getHandle());
        org.bukkit.entity.Mannequin view = (org.bukkit.entity.Mannequin) entity.getBukkitEntity();
        view.setProfile(io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile(skin));
        view.setImmovable(true);
        // Sonst schwebt das Wort "Schaufensterpuppe" über ihr wie ein Namensschild
        entity.setHideDescription(true);
        return entity;
    }

    public static ClientboundAddEntityPacket spawn(Entity entity, double x, double y, double z) {
        return spawn(entity, x, y, z, 0f, 0f);
    }

    public static ClientboundAddEntityPacket spawn(Entity entity, double x, double y, double z, float yaw, float pitch) {
        return new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(), x, y, z, pitch, yaw,
                entity.getType(), 0, Vec3.ZERO, yaw);
    }

    /** Dreht ein Entity. Der Client gleitet in die neue Richtung, statt zu springen. */
    public static ClientboundMoveEntityPacket.Rot rotate(int entityId, float yaw) {
        return new ClientboundMoveEntityPacket.Rot(entityId, angleToByte(yaw), (byte) 0, true);
    }

    /** Dreht den Kopf mit. Ohne das bliebe er stur in die Startrichtung schauen. */
    public static ClientboundRotateHeadPacket rotateHead(Entity entity, float yaw) {
        return new ClientboundRotateHeadPacket(entity, angleToByte(yaw));
    }

    /** Setzt einem Entity einen Hut auf, nur für den Empfänger sichtbar. null = wieder abnehmen. */
    public static ClientboundSetEquipmentPacket helmet(int entityId, ItemStack item) {
        return new ClientboundSetEquipmentPacket(entityId,
                List.of(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(item))));
    }

    /**
     * Setzt Fake-Entities als Passagiere auf einen Spieler. Echte Passagiere bleiben erhalten.
     * Der Client ersetzt bei jedem Paket die ganze Liste, deshalb müssen alle Fake-Passagiere
     * eines Spielers immer gemeinsam verschickt werden.
     */
    public static ClientboundSetPassengersPacket passengers(Player vehicle, int[] fakeIds) {
        ServerPlayer handle = ((CraftPlayer) vehicle).getHandle();
        List<Entity> realPassengers = handle.getPassengers();
        int[] ids = new int[realPassengers.size() + fakeIds.length];
        for (int i = 0; i < realPassengers.size(); i++) {
            ids[i] = realPassengers.get(i).getId();
        }
        System.arraycopy(fakeIds, 0, ids, realPassengers.size(), fakeIds.length);
        return passengers(handle.getId(), ids);
    }

    /**
     * Dieselbe Liste für ein Entity, das kein Spieler ist (die Vorschau-Puppe). Echte Passagiere
     * kann es dort nicht geben — die Puppe existiert nur als Paket.
     */
    public static ClientboundSetPassengersPacket passengers(int vehicleId, int[] ids) {
        // Das Paket hat keinen passenden Konstruktor, deshalb bauen wir es aus Rohdaten
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(vehicleId);
            buffer.writeVarIntArray(ids);
            return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    public static void send(Player player, Packet<?> packet) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        if (handle.connection != null) {
            handle.connection.send(packet);
        }
    }

    /** Minecraft überträgt Winkel als ein Byte: 256 Schritte für die volle Drehung. */
    private static byte angleToByte(float degrees) {
        return (byte) Math.round(degrees * 256f / 360f);
    }
}
