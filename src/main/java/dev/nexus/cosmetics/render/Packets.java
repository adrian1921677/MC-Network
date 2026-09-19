package dev.nexus.cosmetics.render;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        ItemStack item = ItemStack.of(Material.PAPER);
        item.editMeta(meta -> meta.setItemModel(model));
        entity.setItemStack(CraftItemStack.asNMSCopy(item));
        return entity;
    }

    public static void send(Player player, Packet<?> packet) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        if (handle.connection != null) {
            handle.connection.send(packet);
        }
    }
}
