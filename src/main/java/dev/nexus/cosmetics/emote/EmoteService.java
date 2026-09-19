package dev.nexus.cosmetics.emote;

import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.render.FakeCosmeticRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spielt Emotes ab.
 *
 * - Emoji-Emotes: ein animiertes 3D-Emoji über dem Kopf (siehe EmojiBubble).
 * - Posen-Emotes: bewegen den echten Körper mit Mitteln, die Minecraft von sich aus kennt
 *   (Sitzen auf einem unsichtbaren Sitz, Kriechen, Dreizack-Wirbel, Arm-Schwünge, Schleichen).
 *   Sie enden, sobald sich der Spieler bewegt.
 */
public final class EmoteService implements Listener {

    private static final String EMOTE_PLACE = "EMOTE";
    private static final long COOLDOWN_MILLIS = 1500;
    private static final double CANCEL_DISTANCE = 0.3;

    // Eine kleine Tanz-Melodie: {Tick, Tonhöhe}
    private static final float[][] DANCE_TUNE = {
            {0, 1.0f}, {5, 1.26f}, {10, 1.5f}, {15, 1.26f}, {20, 1.0f}, {25, 1.26f}, {30, 1.5f}, {35, 2.0f},
            {40, 1.78f}, {45, 1.5f}, {50, 1.26f}, {55, 1.5f}, {60, 1.0f}, {65, 1.26f}, {70, 1.5f}, {75, 2.0f}};

    private final Plugin plugin;
    private final EmoteRegistry registry;
    private final FakeCosmeticRenderer renderer;
    private final Map<UUID, ActivePose> poses = new HashMap<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private BukkitTask task;

    /** Ein laufendes Posen-Emote. */
    private static final class ActivePose {
        final Emote emote;
        final Location start;
        int age;
        ItemDisplay seat;
        Block crawlBlock;

        ActivePose(Emote emote, Location start) {
            this.emote = emote;
            this.start = start;
        }
    }

    public EmoteService(Plugin plugin, EmoteRegistry registry, FakeCosmeticRenderer renderer) {
        this.plugin = plugin;
        this.registry = registry;
        this.renderer = renderer;
    }

    public EmoteRegistry registry() {
        return registry;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (UUID uuid : List.copyOf(poses.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                stopPose(player);
            }
        }
    }

    public boolean canUse(Player player, Emote emote) {
        return player.hasPermission(emote.permission());
    }

    /** Spielt ein Emote ab. */
    public void play(Player player, Emote emote) {
        if (!canUse(player, emote)) {
            player.sendMessage(CosmeticManager.prefix().append(
                    Component.text("Dieses Emote hast du noch nicht freigeschaltet.", NamedTextColor.RED)));
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MILLIS) {
            return;
        }
        lastUse.put(player.getUniqueId(), now);

        if (emote.type().isEmoji()) {
            renderer.show(player, EMOTE_PLACE, new EmojiBubble(player, emote));
            return;
        }

        stopPose(player);
        ActivePose pose = new ActivePose(emote, player.getLocation());
        switch (emote.type()) {
            case SIT -> {
                if (!onGround(player)) {
                    player.sendMessage(CosmeticManager.prefix().append(
                            Component.text("Du kannst dich nur auf den Boden setzen.", NamedTextColor.RED)));
                    return;
                }
                // Unsichtbarer Sitz: Wer auf einem Entity reitet, sitzt automatisch
                Location seatLocation = player.getLocation();
                pose.seat = player.getWorld().spawn(seatLocation, ItemDisplay.class, seat -> seat.setPersistent(false));
                pose.seat.addPassenger(player);
            }
            case LIE -> {
                // Trick: Ein unsichtbarer Block über dem Kopf (nur für diesen Spieler) lässt ihn kriechen
                Block above = player.getLocation().getBlock().getRelative(0, 1, 0);
                if (!above.getType().isAir()) {
                    player.sendMessage(CosmeticManager.prefix().append(
                            Component.text("Hier ist nicht genug Platz zum Hinlegen.", NamedTextColor.RED)));
                    return;
                }
                pose.crawlBlock = above;
                player.sendBlockChange(above.getLocation(), Material.BARRIER.createBlockData());
                player.setPose(Pose.SWIMMING, true);
            }
            case SPIN -> {
                player.startRiptideAttack(40, 0f, null);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.8f, 1.2f);
            }
            default -> {
            }
        }
        poses.put(player.getUniqueId(), pose);
    }

    /** Beendet ein laufendes Posen-Emote und räumt auf. */
    public void stopPose(Player player) {
        ActivePose pose = poses.remove(player.getUniqueId());
        if (pose == null) {
            return;
        }
        if (pose.seat != null) {
            pose.seat.removePassenger(player);
            pose.seat.remove();
        }
        if (pose.crawlBlock != null) {
            player.sendBlockChange(pose.crawlBlock.getLocation(), pose.crawlBlock.getBlockData());
            player.setPose(Pose.STANDING, false);
        }
        if (pose.emote.type() == EmoteType.DANCE) {
            player.setSneaking(false);
        }
    }

    private void tick() {
        for (UUID uuid : List.copyOf(poses.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            ActivePose pose = poses.get(uuid);
            if (player == null || pose == null) {
                poses.remove(uuid);
                continue;
            }
            pose.age++;

            // Bewegung beendet das Emote (außer beim Sitzen: da steht man mit Schleichen auf)
            boolean moved = pose.emote.type() != EmoteType.SIT
                    && pose.emote.type() != EmoteType.SPIN
                    && (!player.getWorld().equals(pose.start.getWorld()) || player.getLocation().distance(pose.start) > CANCEL_DISTANCE);
            if (moved || (pose.emote.type() == EmoteType.LIE && player.isSneaking())) {
                stopPose(player);
                continue;
            }
            if (animate(player, pose)) {
                stopPose(player);
            }
        }
    }

    /** Animiert ein Posen-Emote. Gibt true zurück, wenn es fertig ist. */
    private boolean animate(Player player, ActivePose pose) {
        int age = pose.age;
        World world = player.getWorld();
        Location head = player.getLocation().add(0, player.getHeight() + 0.3, 0);
        switch (pose.emote.type()) {
            case WAVE -> {
                if (age % 6 == 0) {
                    player.swingMainHand();
                }
                return age >= 42;
            }
            case CLAP -> {
                if (age % 4 == 0) {
                    if ((age / 4) % 2 == 0) {
                        player.swingMainHand();
                    } else {
                        player.swingOffHand();
                    }
                    world.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.8f);
                }
                if (age % 8 == 0) {
                    world.spawnParticle(Particle.CRIT, head.clone().add(0, -0.8, 0), 3, 0.2, 0.1, 0.2, 0.05);
                }
                return age >= 48;
            }
            case SPIN -> {
                double angle = age * 0.6;
                world.spawnParticle(Particle.END_ROD,
                        player.getLocation().add(Math.cos(angle) * 0.7, 0.2 + age * 0.03, Math.sin(angle) * 0.7), 1, 0, 0, 0, 0);
                return age >= 40;
            }
            case DANCE -> {
                if (age % 5 == 0) {
                    player.setSneaking((age / 5) % 2 == 0);
                }
                if (age % 10 == 0) {
                    if ((age / 10) % 2 == 0) {
                        player.swingMainHand();
                    } else {
                        player.swingOffHand();
                    }
                }
                for (float[] note : DANCE_TUNE) {
                    if ((int) note[0] == age) {
                        world.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, note[1]);
                        world.spawnParticle(Particle.NOTE, head, 1, 0.4, 0.2, 0.4, 1);
                    }
                }
                if (age % 15 == 0) {
                    world.spawnParticle(Particle.DUST, head, 6, 0.5, 0.3, 0.5, 0,
                            new Particle.DustOptions(Color.fromRGB(0xCC5DE8), 1f));
                }
                return age >= 80;
            }
            default -> {
                // Sitzen und Liegen dauern, bis der Spieler aufsteht (höchstens 10 Minuten)
                return age >= 12000;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean onGround(Player player) {
        return player.isOnGround();
    }

    // ------------------------------------------------------------------ Events

    /** Aufstehen vom Sitz (Schleichen). */
    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            ActivePose pose = poses.get(player.getUniqueId());
            if (pose != null && pose.seat != null && pose.seat.equals(event.getDismounted())) {
                pose.seat.remove();
                pose.seat = null;
                poses.remove(player.getUniqueId());
                // Nach dem Aufstehen ein kleines Stück nach oben, damit man nicht im Boden steckt
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        player.teleport(player.getLocation().add(0, 0.3, 0)));
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        stopPose(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        stopPose(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopPose(event.getPlayer());
        lastUse.remove(event.getPlayer().getUniqueId());
    }

    /** Schnellzugriff: Schleichen + F (Hand wechseln) öffnet das Emote-Menü. */
    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && player.hasPermission("nexuscosmetics.use")) {
            event.setCancelled(true);
            new EmoteMenu(this, player).open();
        }
    }
}
