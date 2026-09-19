package dev.nexus.cosmetics.render;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
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

    public static ClientboundAddEntityPacket spawn(Display.ItemDisplay entity, double x, double y, double z) {
        return new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(), x, y, z, 0f, 0f,
                EntityTypes.ITEM_DISPLAY, 0, Vec3.ZERO, 0d);
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

        // Das Paket hat keinen passenden Konstruktor, deshalb bauen wir es aus Rohdaten
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(handle.getId());
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
}
