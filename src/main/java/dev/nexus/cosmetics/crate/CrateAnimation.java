package dev.nexus.cosmetics.crate;

import dev.nexus.cosmetics.cosmetic.CosmeticRegistry;
import dev.nexus.cosmetics.cosmetic.Rarity;
import dev.nexus.cosmetics.render.FakeCosmetic;
import dev.nexus.cosmetics.render.Packets;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Die Öffnungs-Animation einer Truhe, vor dem Spieler, für alle in der Nähe sichtbar:
 *
 *  1. Aufsteigen:  Die Truhe wächst drehend aus dem Boden.
 *  2. Glücksrad:   Über der Truhe wechseln die möglichen Gewinne, immer langsamer. Die Truhe bebt.
 *  3. Öffnen:      Der Deckel springt auf, der Gewinn steigt heraus, Effekte je nach Seltenheit.
 *  4. Zeigen:      Der Gewinn dreht sich, darüber stehen Seltenheit und Name.
 *  5. Verschwinden
 */
final class CrateAnimation implements FakeCosmetic {

    private static final int RISE_END = 16;
    private static final int OPEN_TICK = 76;
    private static final int SHOW_END = 140;
    private static final int END = 150;

    private static final double DISTANCE = 2.3;
    private static final float ITEM_HEIGHT = 1.2f;

    private final World world;
    private final Location origin;
    private final float yaw;
    private final CrateReward reward;
    private final List<CrateReward> pool;
    private final Runnable onReveal;
    private final Random random = new Random();

    private final Display.ItemDisplay body;
    private final Display.ItemDisplay lid;
    private final Display.ItemDisplay prize;
    private final Display.TextDisplay label;
    private final ItemDisplay bodyView;
    private final ItemDisplay lidView;
    private final ItemDisplay prizeView;
    private final TextDisplay labelView;

    private final List<Integer> rollTicks = new ArrayList<>();
    private final Set<UUID> viewers = new HashSet<>();
    private int age;

    CrateAnimation(Player player, CrateReward reward, List<CrateReward> pool, Component rarityName, Runnable onReveal) {
        this.world = player.getWorld();
        this.reward = reward;
        this.pool = pool;
        this.onReveal = onReveal;

        // Truhe steht vor dem Spieler auf dem Boden und schaut ihn an
        Location eye = player.getLocation();
        double yawRad = Math.toRadians(eye.getYaw());
        this.origin = eye.clone().add(-Math.sin(yawRad) * DISTANCE, 0, Math.cos(yawRad) * DISTANCE);
        this.yaw = eye.getYaw() + 180;

        body = Packets.createItemDisplay(world, new NamespacedKey(CosmeticRegistry.NAMESPACE, "crate_body"));
        lid = Packets.createItemDisplay(world, new NamespacedKey(CosmeticRegistry.NAMESPACE, "crate_lid"));
        prize = Packets.createItemDisplay(world, pool.getFirst().model());
        label = Packets.createTextDisplay(world);
        bodyView = (ItemDisplay) body.getBukkitEntity();
        lidView = (ItemDisplay) lid.getBukkitEntity();
        prizeView = (ItemDisplay) prize.getBukkitEntity();
        labelView = (TextDisplay) label.getBukkitEntity();

        for (ItemDisplay view : List.of(bodyView, lidView, prizeView)) {
            view.setInterpolationDuration(2);
            view.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        }
        prizeView.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
        prizeView.setBillboard(org.bukkit.entity.Display.Billboard.VERTICAL);
        labelView.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        labelView.setBackgroundColor(Color.fromARGB(150, 15, 10, 25));
        labelView.text(rarityName.append(Component.newline()).append(reward.displayName()));
        labelView.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

        // Glücksrad: Wechsel werden immer langsamer, der letzte zeigt den echten Gewinn
        int tick = RISE_END;
        double interval = 2;
        while (tick < OPEN_TICK - 4) {
            rollTicks.add(tick);
            tick += (int) Math.round(interval);
            interval += 0.35;
        }
        rollTicks.add(OPEN_TICK - 4);

        applyPose();
        world.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.6f);
    }

    // ------------------------------------------------------------------ Sichtbarkeit

    private List<Display> entities() {
        return List.of(body, lid, prize, label);
    }

    @Override
    public void show(Player viewer) {
        viewers.add(viewer.getUniqueId());
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        for (Display entity : entities()) {
            packets.add(Packets.spawn(entity, origin.getX(), origin.getY(), origin.getZ()));
            List<SynchedEntityData.DataValue<?>> data = entity.getEntityData().getNonDefaultValues();
            if (data != null) {
                packets.add(new ClientboundSetEntityDataPacket(entity.getId(), data));
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
        forEachViewer(viewer -> Packets.send(viewer, packet));
        viewers.clear();
    }

    @Override
    public boolean finished() {
        return age >= END;
    }

    // ------------------------------------------------------------------ Ablauf

    @Override
    public void tick(int serverTick) {
        age++;

        int rollIndex = rollTicks.indexOf(age);
        if (rollIndex >= 0) {
            boolean last = rollIndex == rollTicks.size() - 1;
            CrateReward shown = last ? reward : pool.get(random.nextInt(pool.size()));
            Packets.setModel(prize, shown.model());
            float pitch = 0.8f + rollIndex / (float) rollTicks.size();
            world.playSound(origin, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, pitch);
        }

        if (age > OPEN_TICK - 16 && age < OPEN_TICK && age % 3 == 0) {
            // Kleiner Vorgeschmack: Funken in der Farbe der Seltenheit (bei ULTRA in Regenbogenfarben)
            int rgb = reward.rarity() == Rarity.ULTRA
                    ? java.awt.Color.HSBtoRGB(age / 12f, 0.8f, 1f) & 0xFFFFFF
                    : reward.rarity().color();
            world.spawnParticle(Particle.DUST, origin.clone().add(0, 0.5, 0), 3, 0.35, 0.2, 0.35, 0,
                    new Particle.DustOptions(Color.fromRGB(rgb), 1f));
        }
        if (age == OPEN_TICK) {
            reveal();
        }
        if (age > OPEN_TICK && age < SHOW_END && age % 6 == 0) {
            world.spawnParticle(Particle.END_ROD, origin.clone().add(0, ITEM_HEIGHT + 0.3, 0), 1, 0.3, 0.3, 0.3, 0.01);
        }
        if (age == SHOW_END) {
            world.playSound(origin, Sound.BLOCK_CHEST_CLOSE, 0.6f, 1.4f);
        }

        applyPose();
        for (Display entity : entities()) {
            List<SynchedEntityData.DataValue<?>> dirty = entity.getEntityData().packDirty();
            if (dirty != null) {
                ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(entity.getId(), dirty);
                forEachViewer(viewer -> Packets.send(viewer, packet));
            }
        }
    }

    /** Der große Moment: Deckel auf, Effekte je nach Seltenheit. */
    private void reveal() {
        Location center = origin.clone().add(0, 0.8, 0);
        Rarity rarity = reward.rarity();
        Color color = Color.fromRGB(rarity.color());

        world.playSound(origin, Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);
        world.spawnParticle(Particle.DUST, center, 30, 0.4, 0.4, 0.4, 0, new Particle.DustOptions(color, 1.5f));
        world.playSound(origin, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        if (rarity.ordinal() >= Rarity.RARE.ordinal()) {
            world.spawnParticle(Particle.FIREWORK, center, 25, 0.2, 0.3, 0.2, 0.15);
            world.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.1f);
        }
        if (rarity.ordinal() >= Rarity.EPIC.ordinal()) {
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2 * i / 24;
                world.spawnParticle(Particle.END_ROD, center.clone().add(Math.cos(angle) * 0.9, 0, Math.sin(angle) * 0.9),
                        1, 0, 0.05, 0, 0.02);
            }
            world.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
        }
        if (rarity.ordinal() >= Rarity.LEGENDARY.ordinal()) {
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 80, 0.3, 0.5, 0.3, 0.5);
            world.playSound(origin, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        if (rarity == Rarity.ULTRA) {
            // Die große Show: Blitz (ohne Schaden), Lichtsäule und Donnergrollen
            world.strikeLightningEffect(origin.clone().add(0, 0, 0));
            for (double y = 0; y < 8; y += 0.25) {
                world.spawnParticle(Particle.END_ROD, origin.clone().add(0, y, 0), 2, 0.08, 0.05, 0.08, 0.01);
            }
            for (int i = 0; i < 40; i++) {
                double angle = Math.PI * 2 * i / 40;
                world.spawnParticle(Particle.DUST, center.clone().add(Math.cos(angle) * 1.4, 0, Math.sin(angle) * 1.4), 1,
                        0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(java.awt.Color.HSBtoRGB(i / 40f, 0.8f, 1f) & 0xFFFFFF), 1.6f));
            }
            world.playSound(origin, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
            world.playSound(origin, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.2f);
        }

        prizeView.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
        onReveal.run();
    }

    private void applyPose() {
        // 1. Aufsteigen mit Drehung
        double rise = Math.min(1, age / (double) RISE_END);
        double riseEase = 1 - Math.pow(1 - rise, 3);
        double y = -0.8 * (1 - riseEase);
        double spin = (1 - riseEase) * 360;

        // 2. Beben, immer stärker bis zum Öffnen
        double shake = 0;
        if (age > RISE_END && age < OPEN_TICK) {
            double strength = (age - RISE_END) / (double) (OPEN_TICK - RISE_END);
            shake = Math.sin(age * 1.7) * 6 * strength * strength;
        }

        // 5. Verschwinden
        double scale = Math.max(0.001, Math.min(riseEase, age > SHOW_END ? 1 - (age - SHOW_END) / (double) (END - SHOW_END) : 1));

        Matrix4f base = new Matrix4f()
                .rotateY((float) Math.toRadians(-yaw))
                .translate(0, (float) y, 0)
                .rotateY((float) Math.toRadians(spin))
                .rotateZ((float) Math.toRadians(shake))
                .scale((float) scale);
        Matrix4f chest = new Matrix4f(base).translate(0, 0.5f, 0);
        bodyView.setTransformationMatrix(chest);

        // 3. Deckel springt auf (Scharnier hinten oben)
        double open = age < OPEN_TICK ? 0 : Math.min(1, (age - OPEN_TICK) / 5.0);
        double lidAngle = 110 * (1 - Math.pow(1 - open, 2));
        if (age >= SHOW_END) {
            lidAngle *= Math.max(0, 1 - (age - SHOW_END) / 6.0);
        }
        lidView.setTransformationMatrix(new Matrix4f(chest).translate(0, 0, -5 / 16f).rotateX((float) Math.toRadians(-lidAngle)));

        // 2./4. Glücksrad über der Truhe, nach dem Öffnen größer, höher und drehend
        double lift = age < OPEN_TICK ? 0 : Math.min(1, (age - OPEN_TICK) / 10.0);
        double prizeScale = (age < RISE_END ? 0.001 : 0.75 + 0.35 * lift) * scale;
        Matrix4f prizeMatrix = new Matrix4f()
                .translate(0, (float) (ITEM_HEIGHT + lift * 0.35 + Math.sin(age * 0.15) * 0.04), 0);
        if (age >= OPEN_TICK) {
            prizeMatrix.rotateY((float) Math.toRadians((age - OPEN_TICK) * 6));
        }
        prizeView.setTransformationMatrix(prizeMatrix.scale((float) Math.max(0.001, prizeScale)));

        // 4. Name und Seltenheit
        double labelScale = age < OPEN_TICK + 4 ? 0.001 : Math.min(1, (age - OPEN_TICK - 4) / 5.0) * scale;
        labelView.setTransformationMatrix(new Matrix4f().translate(0, ITEM_HEIGHT + 0.95f, 0)
                .scale((float) Math.max(0.001, labelScale)));

        for (Display entity : entities()) {
            ((org.bukkit.entity.Display) entity.getBukkitEntity()).setInterpolationDelay(0);
        }
    }

    // ------------------------------------------------------------------ Hilfsmethoden

    private ClientboundRemoveEntitiesPacket removePacket() {
        return new ClientboundRemoveEntitiesPacket(entities().stream().mapToInt(Display::getId).toArray());
    }

    private void forEachViewer(Consumer<Player> action) {
        for (UUID uuid : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer != null) {
                action.accept(viewer);
            }
        }
    }
}
