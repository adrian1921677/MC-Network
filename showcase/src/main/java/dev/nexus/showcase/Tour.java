package dev.nexus.showcase;

import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.crate.CrateMenu;
import dev.nexus.cosmetics.emote.Emote;
import dev.nexus.cosmetics.menu.CosmeticMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Eine laufende Führung für genau einen Besucher.
 *
 * Der Ablauf ist ein einfacher Zustandsautomat, den der TourManager jeden Tick antreibt:
 * Sätze abspielen, danach zur nächsten Haltestelle gleiten, dort wieder Sätze abspielen.
 *
 * Alles, was dem Besucher gehört (Inventar, Spielmodus), wird gemerkt und am Ende
 * zurückgegeben — sonst würde eine Führung einem Operator sein Inventar leeren.
 */
public final class Tour {

    /** Wie hoch der Bogen zwischen zwei Haltestellen ist. */
    private static final double TRAVEL_ARC = 5.0;
    /** Platz des Frage-Buchs in der Schnellleiste. */
    private static final int QUESTION_SLOT = 4;

    private enum Phase { SPEAKING, TRAVELLING, FINISHED }

    private final NexusShowcase plugin;
    private final ShowcaseWorld world;
    private final Script script;
    private final Player visitor;
    private final Guide guide;

    private final ItemStack[] savedInventory;
    private final GameMode savedGameMode;
    private final Location savedLocation;
    private final PermissionAttachment permissions;

    private Phase phase = Phase.SPEAKING;
    /** true = der Besucher wurde abgemeldet, der TourManager darf die Fuehrung vergessen. */
    private boolean stopped;
    private int stationIndex;
    private int stepIndex;
    private int timer;

    /** Ticks, die der Besucher nach der Führung noch bleiben darf. 0 = kein Rauswurf. */
    private int freeRoamLeft;

    private ArmorStand ride;
    private Location travelFrom;
    private Location travelTo;
    private int travelTicks;
    private final int travelTotal;

    public Tour(NexusShowcase plugin, Player visitor) {
        this.plugin = plugin;
        this.world = plugin.showcaseWorld();
        this.script = plugin.script();
        this.visitor = visitor;
        this.travelTotal = Math.max(20, plugin.getConfig().getInt("tour.travel-seconds", 5) * 20);

        this.savedInventory = visitor.getInventory().getContents().clone();
        this.savedGameMode = visitor.getGameMode();
        this.savedLocation = visitor.getLocation();

        // Waehrend der Fuehrung darf der Besucher alles anprobieren
        this.permissions = visitor.addAttachment(plugin);
        permissions.setPermission("nexuscosmetics.cosmetic.*", true);
        permissions.setPermission("nexuscosmetics.emote.*", true);
        permissions.setPermission("nexuscosmetics.use", true);

        this.guide = new Guide(world.guideSpot(0), plugin.guideSkin(), plugin.guideName());

        prepareVisitor();
        enterStation(0);
    }

    // ------------------------------------------------------------------ Aufbau und Abbau

    private void prepareVisitor() {
        visitor.setGameMode(GameMode.ADVENTURE);
        visitor.getInventory().clear();
        visitor.getInventory().setItem(QUESTION_SLOT, questionBook());
        visitor.getInventory().setHeldItemSlot(QUESTION_SLOT);
        visitor.setInvulnerable(true);
        visitor.setFlying(false);
        visitor.setAllowFlight(false);
        visitor.teleport(world.visitorSpot(0));
    }

    private ItemStack questionBook() {
        ItemStack book = ItemStack.of(org.bukkit.Material.WRITABLE_BOOK);
        book.editMeta(meta -> {
            meta.itemName(script.questionItem());
            meta.lore(List.of(script.questionItemLore()
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        });
        return book;
    }

    /** Beendet die Führung und gibt dem Besucher alles zurück, was ihm gehört. */
    public void stop(boolean teleportBack) {
        dropRide();
        guide.remove();
        plugin.nexus().cosmetics().unequipAll(visitor);
        visitor.removeAttachment(permissions);
        if (visitor.isOnline()) {
            visitor.getInventory().setContents(savedInventory);
            visitor.setGameMode(savedGameMode);
            visitor.setInvulnerable(false);
            if (teleportBack && savedLocation.getWorld() != null
                    && !savedLocation.getWorld().equals(world.world())) {
                visitor.teleport(savedLocation);
            }
        }
        phase = Phase.FINISHED;
        stopped = true;
    }

    /** Die Führung ist durchgelaufen. Der Besucher darf sich ab jetzt frei bewegen. */
    public boolean finished() {
        return phase == Phase.FINISHED;
    }

    /** Endgültig vorbei: aufgeräumt, der Besucher hat sein Inventar zurück. */
    public boolean stopped() {
        return stopped;
    }

    /** Während der Fahrt darf sich der Besucher bewegen — er sitzt ja auf dem Träger. */
    public boolean riding() {
        return phase == Phase.TRAVELLING;
    }

    public Player visitor() {
        return visitor;
    }

    /** Alles, was nur dieser Besucher sehen soll. */
    public List<Entity> ownEntities() {
        List<Entity> entities = new ArrayList<>(guide.entities());
        if (ride != null) {
            entities.add(ride);
        }
        return entities;
    }

    // ------------------------------------------------------------------ Ablauf

    public void tick() {
        if (phase == Phase.FINISHED) {
            tickFreeRoam();
            return;
        }
        if (phase == Phase.TRAVELLING) {
            tickTravel();
            return;
        }
        if (--timer > 0) {
            return;
        }
        stepIndex++;
        if (stepIndex < currentStation().steps().size()) {
            playStep();
        } else {
            leaveStation();
        }
    }

    private Script.Station currentStation() {
        return script.stations().get(stationIndex);
    }

    private void enterStation(int index) {
        stationIndex = index;
        stepIndex = 0;
        phase = Phase.SPEAKING;
        guide.moveTo(world.guideSpot(index));
        visitor.teleport(world.visitorSpot(index));
        visitor.showTitle(Title.title(currentStation().title(), Component.empty(),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(600))));
        visitor.playSound(visitor.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
        playStep();
    }

    private void playStep() {
        Script.Step step = currentStation().steps().get(stepIndex);
        timer = step.hold();
        guide.say(step.text());
        visitor.sendMessage(script.prefix().append(step.text()));
        visitor.playSound(visitor.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.35f, 1.5f);

        if (step.equip() != null) {
            equip(step.equip());
        }
        if (step.emote() != null) {
            emote(step.emote());
        }
        if (step.action() != null) {
            action(step.action());
        }
    }

    private void leaveStation() {
        if (stationIndex + 1 >= script.stations().size()) {
            finish();
        } else {
            startTravel();
        }
    }

    // ------------------------------------------------------------------ Was an einer Haltestelle passiert

    private void equip(String id) {
        Cosmetic cosmetic = plugin.nexus().cosmetics().registry().get(id);
        if (cosmetic == null) {
            plugin.getLogger().warning("Unbekanntes Cosmetic in der Tour: " + id);
            return;
        }
        plugin.nexus().cosmetics().equip(visitor, cosmetic);
        visitor.playSound(visitor.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.8f, 1.2f);
    }

    private void emote(String id) {
        Emote found = plugin.nexus().emotes().registry().get(id);
        if (found == null) {
            plugin.getLogger().warning("Unbekanntes Emote in der Tour: " + id);
            return;
        }
        plugin.nexus().emotes().play(visitor, found);
    }

    private void action(String action) {
        switch (action) {
            case "strip" -> plugin.nexus().cosmetics().unequipAll(visitor);
            case "menu" -> new CosmeticMenu(plugin.nexus(), visitor).open();
            case "crate" -> {
                plugin.nexus().cosmetics().modifyProfile(visitor.getUniqueId(),
                        profile -> profile.addKeys("standard", 1));
                new CrateMenu(plugin.nexus().crates(), plugin.nexus().cosmetics(),
                        plugin.nexus().messages(), visitor).open();
            }
            case "buy" -> showBuyLink();
            default -> plugin.getLogger().warning("Unbekannte Aktion in der Tour: " + action);
        }
    }

    private void showBuyLink() {
        String url = plugin.getConfig().getString("buy.url", "");
        String label = plugin.getConfig().getString("buy.label", "Info");
        if (url.isBlank()) {
            return;
        }
        visitor.sendMessage(script.prefix()
                .append(Component.text("[" + label + "]", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.openUrl(url))
                        .hoverEvent(Component.text(url, NamedTextColor.GRAY))));
    }

    // ------------------------------------------------------------------ Fahrt zur nächsten Haltestelle

    private void startTravel() {
        travelFrom = world.visitorSpot(stationIndex);
        travelTo = world.visitorSpot(stationIndex + 1);
        travelTicks = 0;
        phase = Phase.TRAVELLING;

        // Der Fuehrer wartet am Ziel schon. Der Besucher schaut nach vorne und sieht es nicht.
        guide.moveTo(world.guideSpot(stationIndex + 1));
        guide.silent(script.guideIdle());

        ride = world.world().spawn(travelFrom, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setBasePlate(false);
            stand.setPersistent(false);
            stand.setSilent(true);
            stand.setCollidable(false);
        });
        plugin.tours().hideFromOthers(this, ride);
        ride.addPassenger(visitor);

        visitor.showTitle(Title.title(Component.empty(), script.travelSubtitle(),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(1), Duration.ofMillis(500))));
    }

    /**
     * Bewegt den Träger jeden Tick ein Stück weiter, in einem flachen Bogen und mit weichem
     * Anfahren und Abbremsen. Der Client zieht die Bewegung des Fahrzeugs glatt, deshalb sieht
     * das für den Besucher wie ein Flug aus und nicht wie zwanzig Teleports pro Sekunde.
     */
    private void tickTravel() {
        travelTicks++;
        double progress = Math.min(1.0, travelTicks / (double) travelTotal);
        double eased = progress * progress * (3 - 2 * progress);

        Location next = new Location(world.world(),
                travelFrom.getX() + (travelTo.getX() - travelFrom.getX()) * eased,
                travelFrom.getY() + Math.sin(Math.PI * eased) * TRAVEL_ARC,
                travelFrom.getZ() + (travelTo.getZ() - travelFrom.getZ()) * eased,
                travelFrom.getYaw(), 0f);
        // Passagiere kommen seit 26.3 von selbst mit; ein Extra-Flag dafuer gibt es nicht mehr.
        ride.teleport(next);

        if (progress >= 1.0) {
            dropRide();
            enterStation(stationIndex + 1);
        }
    }

    private void dropRide() {
        if (ride == null) {
            return;
        }
        ride.removePassenger(visitor);
        ride.remove();
        ride = null;
    }

    // ------------------------------------------------------------------ Ende

    /**
     * Ende der Runde. Entweder darf der Besucher sich jetzt frei umsehen — mit allen Cosmetics
     * und dem Menü, das ist der beste Moment zum Ausprobieren — oder die Führung beginnt von vorne.
     */
    private void finish() {
        if (!plugin.getConfig().getBoolean("tour.free-roam-at-end", true)) {
            enterStation(0);
            return;
        }
        phase = Phase.FINISHED;
        visitor.setAllowFlight(true);
        visitor.playSound(visitor.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);

        int seconds = Math.max(0, plugin.getConfig().getInt("tour.free-roam-seconds", 180));
        freeRoamLeft = seconds * 20;
        if (seconds > 0) {
            visitor.sendMessage(script.prefix().append(script.freeRoam(Math.max(1, seconds / 60))));
        }
    }

    /**
     * Zählt herunter, wie lange der Besucher noch bleiben darf.
     *
     * Auf einem kleinen Demo-Server sind die Plätze knapp. Wer die Runde hinter sich hat und
     * seitdem nur noch herumfliegt, blockiert einen Platz für den nächsten Interessenten —
     * deshalb mit Vorwarnung hinaus. Steht free-roam-seconds auf 0, bleibt jeder.
     */
    private void tickFreeRoam() {
        if (freeRoamLeft <= 0) {
            return;
        }
        freeRoamLeft--;
        if (freeRoamLeft == 0) {
            sendAway();
            return;
        }
        if (freeRoamLeft % 20 != 0) {
            return;
        }
        int seconds = freeRoamLeft / 20;
        if (seconds == 60 || seconds == 30 || seconds == 10 || seconds <= 5) {
            visitor.sendActionBar(script.freeRoamWarning(seconds));
            visitor.playSound(visitor.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.4f);
        }
    }

    /** Aufräumen, dann hinausbegleiten — mit dem Link auf dem Trennbildschirm. */
    private void sendAway() {
        Component reason = Component.empty();
        List<Component> lines = script.kickLines();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                reason = reason.append(Component.newline());
            }
            reason = reason.append(lines.get(i));
        }
        String url = plugin.getConfig().getString("buy.url", "");
        if (!url.isBlank()) {
            reason = reason.append(Component.newline()).append(Component.newline())
                    .append(Component.text(url, NamedTextColor.AQUA));
        }
        stop(false);
        visitor.kick(reason);
    }

    /** Der Führer antwortet auf eine Frage aus dem Buch. */
    public void answer(Script.Question question) {
        guide.say(question.answer().isEmpty() ? Component.empty() : question.answer().getFirst());
        visitor.sendMessage(script.prefix().append(question.question()));
        question.answer().forEach(line -> visitor.sendMessage(script.prefix().append(line)));
        visitor.playSound(visitor.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.5f, 1.4f);
        // Die Fuehrung soll nicht mitten in der Antwort weiterlaufen
        if (phase == Phase.SPEAKING) {
            timer = Math.max(timer, question.answer().size() * 45);
        }
    }
}
