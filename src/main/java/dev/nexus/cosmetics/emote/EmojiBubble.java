package dev.nexus.cosmetics.emote;

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
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
 * Ein 3D-Emoji, das über dem Kopf aufploppt, sich passend zum Gefühl bewegt und wieder verschwindet.
 * Sitzt als Passagier auf dem Spieler und dreht sich immer zum Betrachter.
 */
final class EmojiBubble implements FakeCosmetic {

    private static final int DURATION = 60;
    private static final int POP_TICKS = 6;
    private static final float HEIGHT = 0.8f;
    private static final float SIZE = 0.5f;

    private static final Color[] CONFETTI = {
            Color.fromRGB(0xFF4D6D), Color.fromRGB(0xFFD43B), Color.fromRGB(0x4DABF7),
            Color.fromRGB(0x51CF66), Color.fromRGB(0xCC5DE8)};

    private final Player wearer;
    private final EmoteType type;
    private final Display.ItemDisplay entity;
    private final ItemDisplay view;
    private final Set<UUID> viewers = new HashSet<>();
    private final Random random = new Random();
    private int age;
    private double lift;

    EmojiBubble(Player wearer, Emote emote) {
        this.wearer = wearer;
        this.type = emote.type();
        this.entity = Packets.createItemDisplay(wearer.getWorld(), emote.model());
        this.view = (ItemDisplay) entity.getBukkitEntity();
        view.setBillboard(org.bukkit.entity.Display.Billboard.VERTICAL);
        view.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        view.setInterpolationDuration(2);
        applyPose();
        effects();
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
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entity.getId());
        for (UUID uuid : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer != null) {
                Packets.send(viewer, packet);
            }
        }
        viewers.clear();
    }

    @Override
    public int[] passengerIds(Player viewer) {
        return new int[]{entity.getId()};
    }

    @Override
    public boolean finished() {
        return age >= DURATION;
    }

    @Override
    public void tick(int serverTick) {
        age++;
        applyPose();
        effects();
        List<SynchedEntityData.DataValue<?>> dirty = entity.getEntityData().packDirty();
        if (dirty != null) {
            for (UUID uuid : List.copyOf(viewers)) {
                Player viewer = Bukkit.getPlayer(uuid);
                if (viewer != null) {
                    Packets.send(viewer, new ClientboundSetEntityDataPacket(entity.getId(), dirty));
                }
            }
        }
    }

    /** Aufploppen, gefühlstypische Bewegung, am Ende wieder wegploppen. */
    private void applyPose() {
        double scale;
        if (age < POP_TICKS) {
            scale = Math.max(0.001, easeOutBack(age / (double) POP_TICKS));
        } else if (age > DURATION - POP_TICKS) {
            scale = Math.max(0.001, (DURATION - age) / (double) POP_TICKS);
        } else {
            scale = 1;
        }

        double x = 0;
        double y = 0;
        double roll = 0;
        switch (type) {
            case HEART -> scale *= 1 + 0.15 * Math.pow(Math.sin(age * 0.35), 8); // Herzschlag
            case LAUGH -> {
                roll = Math.sin(age * 1.1) * 12;
                y = Math.abs(Math.sin(age * 0.55)) * 0.04;
            }
            case ANGRY -> {
                x = Math.sin(age * 2.6) * 0.03;
                scale *= 1 + 0.06 * Math.sin(age * 0.9);
            }
            case WOW -> y = age < 20 ? Math.abs(Math.sin(age * 0.31)) * 0.18 : 0;
            case THUMBS -> {
                y = Math.abs(Math.sin(age * 0.3)) * 0.08;
                roll = -8;
            }
            case SLEEPY -> {
                lift += 0.004;
                y = lift;
                roll = Math.sin(age * 0.08) * 10;
            }
            case PARTY -> roll = Math.sin(age * 0.5) * 15;
            default -> {
            }
        }

        view.setTransformationMatrix(new Matrix4f()
                .translate((float) x, (float) (HEIGHT + y), 0)
                .rotateZ((float) Math.toRadians(roll))
                .scale((float) (SIZE * scale)));
        view.setInterpolationDelay(0);
    }

    /** Geräusche und Partikel, passend zum Gefühl. */
    private void effects() {
        World world = wearer.getWorld();
        Location at = wearer.getLocation().add(0, wearer.getHeight() + HEIGHT, 0);
        switch (type) {
            case HEART -> {
                if (age == 0 || age == 8) {
                    world.playSound(at, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, age == 0 ? 1.5f : 2.0f);
                }
                if (age % 10 == 0 && age < DURATION - 10) {
                    world.spawnParticle(Particle.HEART, at, 1, 0.3, 0.2, 0.3, 0);
                }
            }
            case LAUGH -> {
                if (age == 0) {
                    world.playSound(at, Sound.ENTITY_WITCH_CELEBRATE, 0.7f, 1.6f);
                }
                if (age % 4 == 0 && age < DURATION - 10) {
                    world.spawnParticle(Particle.FALLING_WATER, at, 2, 0.22, 0.05, 0.05, 0);
                }
            }
            case ANGRY -> {
                if (age == 0) {
                    world.playSound(at, Sound.ENTITY_PIGLIN_ANGRY, 0.6f, 1.4f);
                }
                if (age % 3 == 0 && age < DURATION - 10) {
                    world.spawnParticle(Particle.SMOKE, at.clone().add(0, 0.2, 0), 1, 0.15, 0.02, 0.15, 0.01);
                }
                if (age % 12 == 0) {
                    world.spawnParticle(Particle.ANGRY_VILLAGER, at, 1, 0.3, 0.1, 0.3, 0);
                }
            }
            case WOW -> {
                if (age == 0) {
                    world.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
                    world.spawnParticle(Particle.CRIT, at, 12, 0.25, 0.25, 0.25, 0.2);
                }
            }
            case THUMBS -> {
                if (age == 0) {
                    world.playSound(at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, at, 8, 0.3, 0.3, 0.3, 0);
                }
            }
            case SLEEPY -> {
                if (age == 0 || age == 30) {
                    world.playSound(at, Sound.ENTITY_FOX_SLEEP, 0.7f, 1.2f);
                }
            }
            case PARTY -> {
                if (age == 0) {
                    world.playSound(at, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.2f);
                }
                if (age % 10 == 0 && age < DURATION - 10) {
                    for (int i = 0; i < 12; i++) {
                        Color color = CONFETTI[random.nextInt(CONFETTI.length)];
                        world.spawnParticle(Particle.DUST, at, 1, 0.4, 0.3, 0.4, 0, new Particle.DustOptions(color, 0.9f));
                    }
                }
            }
            default -> {
            }
        }
    }

    private static double easeOutBack(double t) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }
}
