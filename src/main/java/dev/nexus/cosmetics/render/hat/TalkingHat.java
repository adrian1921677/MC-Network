package dev.nexus.cosmetics.render.hat;

import dev.nexus.cosmetics.config.Messages;
import dev.nexus.cosmetics.config.Settings;
import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.render.FakeCosmetic;
import dev.nexus.cosmetics.render.Packets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Der sprechende Hut. Das Hut-Modell selbst sitzt im Helm-Slot; diese Klasse sorgt fürs Reden:
 * - eine Sprechblase über dem Kopf (Text-Anzeige, die als Passagier auf dem Spieler sitzt),
 * - der Text erscheint Buchstabe für Buchstabe mit Plapper-Geräuschen,
 * - währenddessen klappt der Mund auf und zu (das Helm-Item wechselt zwischen zwei Modellen).
 *
 * Beim Aufsetzen teilt der Hut den Spieler feierlich einem Team zu.
 */
public final class TalkingHat implements FakeCosmetic {

    private static final int TICKS_PER_LETTER = 1;
    private static final int HOLD_TICKS = 70;
    private static final int PAUSE_BETWEEN_LINES = 12;
    private static final float BUBBLE_HEIGHT = 0.95f;
    private static final float BUBBLE_SCALE = 0.7f;

    /** Hier merkt sich der Hut dauerhaft das Team des Spielers (in den Spielerdaten). */
    private static final NamespacedKey TEAM_KEY = new NamespacedKey("nexuscosmetics", "hat_team");

    private final Player wearer;
    private final Messages messages;
    private final Settings settings;
    private final NamespacedKey closedModel;
    private final NamespacedKey talkingModel;
    private final Predicate<ItemStack> isHatItem;
    private final Display.TextDisplay bubble;
    private final TextDisplay bubbleView;
    private final Set<UUID> viewers = new HashSet<>();
    private final Random random = new Random();

    private final Queue<Component> queue = new ArrayDeque<>();
    private Component currentLine;
    private String currentText = "";
    private int revealed;
    private int holdTicks;
    private int silenceTicks;
    private boolean mouthOpen;
    /** Das Team, das gerade verkündet wird (für das Feuerwerk), sonst null. */
    private Component announcedTeam;

    public TalkingHat(Player wearer, Cosmetic cosmetic, Predicate<ItemStack> isHatItem, Messages messages, Settings settings) {
        this.wearer = wearer;
        this.messages = messages;
        this.settings = settings;
        this.closedModel = cosmetic.model();
        this.talkingModel = cosmetic.model("talk");
        this.isHatItem = isHatItem;

        this.bubble = Packets.createTextDisplay(wearer.getWorld());
        this.bubbleView = (TextDisplay) bubble.getBukkitEntity();
        bubbleView.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        bubbleView.setBackgroundColor(Color.fromARGB(190, 25, 20, 35));
        bubbleView.setLineWidth(170);
        bubbleView.setInterpolationDuration(3);
        bubbleView.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        setBubbleVisible(false);

        // Beim ersten Aufsetzen: feierliche Team-Zuteilung. Danach erinnert sich der Hut.
        List<String> teams = messages.keys("talking-hat.teams");
        String savedTeam = wearer.getPersistentDataContainer().get(TEAM_KEY, PersistentDataType.STRING);
        if (savedTeam == null || !teams.contains(savedTeam)) {
            if (!teams.isEmpty()) {
                String team = teams.get(random.nextInt(teams.size()));
                wearer.getPersistentDataContainer().set(TEAM_KEY, PersistentDataType.STRING, team);
                announcedTeam = messages.get("talking-hat.teams." + team);
                messages.rawList("talking-hat.sorting").forEach(line -> queue.add(speech(line)));
                queue.add(speech(messages.raw("talking-hat.sorting-result"), Placeholder.component("team", announcedTeam)));
            }
        } else {
            queue.add(speech(messages.raw("talking-hat.welcome-back"),
                    Placeholder.component("team", messages.get("talking-hat.teams." + savedTeam))));
        }
        silenceTicks = 30;
    }

    // ------------------------------------------------------------------ Sichtbarkeit

    @Override
    public void show(Player viewer) {
        viewers.add(viewer.getUniqueId());
        Location location = wearer.getLocation();
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        packets.add(Packets.spawn(bubble, location.getX(), location.getY() + wearer.getHeight(), location.getZ()));
        List<SynchedEntityData.DataValue<?>> data = bubble.getEntityData().getNonDefaultValues();
        if (data != null) {
            packets.add(new ClientboundSetEntityDataPacket(bubble.getId(), data));
        }
        Packets.send(viewer, new ClientboundBundlePacket(packets));
    }

    @Override
    public void hide(Player viewer) {
        if (viewers.remove(viewer.getUniqueId())) {
            Packets.send(viewer, new ClientboundRemoveEntitiesPacket(bubble.getId()));
        }
    }

    @Override
    public void forget(UUID viewer) {
        viewers.remove(viewer);
    }

    @Override
    public void destroy() {
        sendToViewers(new ClientboundRemoveEntitiesPacket(bubble.getId()));
        viewers.clear();
    }

    @Override
    public int[] passengerIds(Player viewer) {
        return new int[]{bubble.getId()};
    }

    // ------------------------------------------------------------------ Reden

    @Override
    public void tick(int serverTick) {
        if (currentLine == null) {
            if (silenceTicks > 0) {
                silenceTicks--;
            } else {
                startLine(queue.isEmpty() ? randomLine() : queue.poll());
            }
        } else if (revealed < currentText.length()) {
            revealLetter(serverTick);
        } else if (holdTicks > 0) {
            holdTicks--;
            setMouth(false);
        } else {
            finishLine();
        }

        List<SynchedEntityData.DataValue<?>> dirty = bubble.getEntityData().packDirty();
        if (dirty != null) {
            sendToViewers(new ClientboundSetEntityDataPacket(bubble.getId(), dirty));
        }
    }

    private void startLine(Component line) {
        currentLine = line;
        currentText = PlainTextComponentSerializer.plainText().serialize(line);
        revealed = 0;
        holdTicks = HOLD_TICKS;
        setBubbleVisible(true);
    }

    private void revealLetter(int serverTick) {
        if (serverTick % TICKS_PER_LETTER != 0) {
            return;
        }
        revealed++;
        bubbleView.text(partialLine(revealed));

        // Mund auf und zu, Plapper-Geräusch bei jedem zweiten Buchstaben
        if (revealed % 3 == 0) {
            setMouth(!mouthOpen);
        }
        if (revealed % 2 == 0 && currentText.charAt(revealed - 1) != ' ') {
            wearer.getWorld().playSound(wearer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.25f,
                    1.3f + random.nextFloat() * 0.4f);
        }

        if (revealed == currentText.length()) {
            // Der Träger sieht die Blase in der Ich-Perspektive nicht, deshalb auch in die Aktionsleiste
            wearer.sendActionBar(Component.text("🎩 ", NamedTextColor.GOLD).append(currentLine));
            if (announcedTeam != null && queue.isEmpty()) {
                celebrate();
            }
        }
    }

    /** Zeigt nur die ersten Buchstaben an, behält aber die Farben der Zeile. */
    private Component partialLine(int letters) {
        if (letters >= currentText.length()) {
            return currentLine;
        }
        return Component.text(currentText.substring(0, letters), NamedTextColor.WHITE);
    }

    private void finishLine() {
        currentLine = null;
        setBubbleVisible(false);
        setMouth(false);
        silenceTicks = queue.isEmpty()
                ? settings.hatMinSilenceTicks() + random.nextInt(settings.hatMaxSilenceTicks() - settings.hatMinSilenceTicks())
                : PAUSE_BETWEEN_LINES;
    }

    /** Team verkündet: Feuerwerk aus Partikeln und ein Fanfaren-Sound. */
    private void celebrate() {
        Location location = wearer.getLocation().add(0, wearer.getHeight() + 0.3, 0);
        TextColor teamColor = firstColor(announcedTeam);
        Color color = Color.fromRGB(teamColor == null ? 0xFFD43B : teamColor.value());
        wearer.getWorld().spawnParticle(Particle.DUST, location, 40, 0.5, 0.4, 0.5,
                new Particle.DustOptions(color, 1.4f));
        wearer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location, 30, 0.3, 0.3, 0.3, 0.3);
        wearer.getWorld().playSound(wearer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        announcedTeam = null;
    }

    private Component randomLine() {
        List<String> lines = messages.rawList("talking-hat.lines");
        return lines.isEmpty() ? Component.text("...") : speech(lines.get(random.nextInt(lines.size())));
    }

    /** Ein Spruch in Weiß, Platzhalter wie <team> werden ersetzt. */
    private Component speech(String text, TagResolver... placeholders) {
        return Component.text().color(NamedTextColor.WHITE).append(messages.parse(text, placeholders)).build();
    }

    /** Sucht die erste Farbe in einem Text (für das Team-Feuerwerk). */
    private static TextColor firstColor(Component component) {
        if (component.color() != null) {
            return component.color();
        }
        for (Component child : component.children()) {
            TextColor color = firstColor(child);
            if (color != null) {
                return color;
            }
        }
        return null;
    }

    private void setBubbleVisible(boolean visible) {
        bubbleView.setTransformationMatrix(new Matrix4f()
                .translate(0, BUBBLE_HEIGHT, 0)
                .scale(visible ? BUBBLE_SCALE : 0.001f));
        bubbleView.setInterpolationDelay(0);
        if (!visible) {
            bubbleView.text(Component.empty());
        }
    }

    /** Wechselt das Helm-Item zwischen offenem und geschlossenem Mund. */
    private void setMouth(boolean open) {
        if (open == mouthOpen) {
            return;
        }
        ItemStack helmet = wearer.getInventory().getHelmet();
        if (!isHatItem.test(helmet)) {
            return;
        }
        mouthOpen = open;
        helmet.editMeta(meta -> meta.setItemModel(open ? talkingModel : closedModel));
        wearer.getInventory().setHelmet(helmet);
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
