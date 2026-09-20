package dev.nexus.showcase;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Die Showcase-Welt.
 *
 * Das Plugin legt sie selbst an — eine leere Void-Welt — und baut für jede Haltestelle eine
 * Plattform hinein. Dadurch hängt die Führung nicht davon ab, was auf dem Server sonst
 * herumsteht, und sie lässt sich jederzeit reparieren: Welt löschen, Server neu starten, fertig.
 *
 * Die Haltestellen liegen aufgereiht entlang der X-Achse. Der Besucher schaut immer nach Osten,
 * also genau in die Richtung, in die es als Nächstes weitergeht.
 */
public final class ShowcaseWorld {

    /** Radius der Plattform in Blöcken. */
    private static final int RADIUS = 14;
    /** Abstand zwischen Besucher und Führer. */
    private static final double GUIDE_DISTANCE = 4.5;
    /** Blickrichtung des Besuchers: Osten (+X). */
    private static final float VISITOR_YAW = -90f;

    private final JavaPlugin plugin;
    private final String name;
    private final int spacing;
    private final int height;
    private World world;

    public ShowcaseWorld(JavaPlugin plugin, String name, int spacing, int height) {
        this.plugin = plugin;
        this.name = name;
        this.spacing = spacing;
        this.height = height;
    }

    /** Leere Welt ohne Gelände. Was hier steht, hat das Plugin selbst gebaut. */
    private static final class VoidGenerator extends ChunkGenerator {

        @Override
        public boolean shouldGenerateNoise() {
            return false;
        }

        @Override
        public boolean shouldGenerateSurface() {
            return false;
        }

        @Override
        public boolean shouldGenerateCaves() {
            return false;
        }

        @Override
        public boolean shouldGenerateDecorations() {
            return false;
        }

        @Override
        public boolean shouldGenerateMobs() {
            return false;
        }

        @Override
        public boolean shouldGenerateStructures() {
            return false;
        }
    }

    // ------------------------------------------------------------------ Anlegen

    public World world() {
        return world;
    }

    /** Legt die Welt an, falls sie fehlt, und stellt sie auf ewigen Nachmittag. */
    public void prepare() {
        world = Bukkit.getWorld(name);
        if (world == null) {
            world = new WorldCreator(name)
                    .generator(new VoidGenerator())
                    .generateStructures(false)
                    .createWorld();
        }
        if (world == null) {
            throw new IllegalStateException("Die Showcase-Welt '" + name + "' liess sich nicht anlegen.");
        }
        world.setSpawnLocation(visitorSpot(0));
        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.MOB_GRIEFING, false);
        world.setGameRule(GameRules.FALL_DAMAGE, false);
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        // In einer leeren Welt gibt es nichts zu wachsen oder zu verfallen
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
    }

    /**
     * Hält die Haltestellen dauerhaft geladen.
     *
     * Ohne das entlädt der Server eine Plattform, sobald niemand dort steht — und nimmt die
     * schwebenden Namensschilder gleich mit. Es sind ein paar Dutzend leere Chunks ohne Mobs
     * und ohne Zufalls-Ticks; das kostet praktisch nichts.
     */
    private void keepLoaded(int stationCount) {
        for (int index = 0; index < stationCount; index++) {
            int cx = centreX(index);
            for (int chunkX = (cx - RADIUS) >> 4; chunkX <= (cx + RADIUS) >> 4; chunkX++) {
                for (int chunkZ = (-RADIUS) >> 4; chunkZ <= RADIUS >> 4; chunkZ++) {
                    world.setChunkForceLoaded(chunkX, chunkZ, true);
                }
            }
        }
    }

    /**
     * Baut alle Haltestellen neu.
     *
     * Wird bei jedem Start ausgeführt: Das kostet kaum etwas und repariert nebenbei alles,
     * was ein Besucher oder ein Absturz hinterlassen hat.
     */
    public void build(List<Script.Station> stations) {
        keepLoaded(stations.size());
        clearLeftovers();
        for (int index = 0; index < stations.size(); index++) {
            buildStation(index, stations.get(index));
        }
        plugin.getLogger().info(stations.size() + " Haltestellen gebaut in Welt '" + name + "'.");
    }

    /** Entfernt Puppen, Trägertiere und Schilder aus früheren Läufen. Spieler bleiben. */
    private void clearLeftovers() {
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
    }

    private void buildStation(int index, Script.Station station) {
        int cx = centreX(index);
        Material accent = station.accent();

        // Plattform: heller Kern, dunkler Ring, Akzentkante
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > RADIUS + 0.4) {
                    continue;
                }
                Material top = distance > RADIUS - 0.6 ? accent
                        : distance > RADIUS - 1.6 ? Material.POLISHED_DEEPSLATE
                        : Material.SMOOTH_QUARTZ;
                world.getBlockAt(cx + dx, height, dz).setType(top, false);
                world.getBlockAt(cx + dx, height - 1, dz).setType(Material.DEEPSLATE_BRICKS, false);
            }
        }

        // Rückwand hinter dem Führer, damit er nicht vor dem Nichts steht
        for (int degrees = -70; degrees <= 70; degrees += 2) {
            double angle = Math.toRadians(degrees);
            int bx = cx + (int) Math.round(Math.cos(angle) * (RADIUS - 1));
            int bz = (int) Math.round(Math.sin(angle) * (RADIUS - 1));
            for (int h = 1; h <= 5; h++) {
                Material block = h == 5 ? accent
                        : h % 2 == 0 ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_TILES;
                world.getBlockAt(bx, height + h, bz).setType(block, false);
            }
        }

        // Säulen mit Licht auf der offenen Seite, damit die Plattform nicht flach wirkt
        for (int degrees = 100; degrees <= 260; degrees += 40) {
            double angle = Math.toRadians(degrees);
            int px = cx + (int) Math.round(Math.cos(angle) * (RADIUS - 2));
            int pz = (int) Math.round(Math.sin(angle) * (RADIUS - 2));
            for (int h = 1; h <= 4; h++) {
                world.getBlockAt(px, height + h, pz).setType(Material.POLISHED_DEEPSLATE, false);
            }
            world.getBlockAt(px, height + 5, pz).setType(Material.SEA_LANTERN, false);
            world.getBlockAt(px, height + 6, pz).setType(accent, false);
        }

        // Licht unter der Plattform, sonst wird es auf der dunklen Seite fleckig
        world.getBlockAt(cx, height - 1, 0).setType(Material.SEA_LANTERN, false);

        spawnTitle(index, station.title());
    }

    /** Der Name der Haltestelle schwebt über der Rückwand. */
    private void spawnTitle(int index, Component title) {
        Location at = new Location(world, centreX(index) + RADIUS - 2.0, height + 7.0, 0.5);
        world.spawn(at, TextDisplay.class, display -> {
            display.text(title);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            // Bleibt gespeichert. Beim naechsten Bauen raeumt clearLeftovers() die alten weg,
            // deshalb sammeln sie sich nicht an.
            display.setPersistent(true);
            display.setTransformation(scaled(display, 2.2f));
        });
    }

    private static org.bukkit.util.Transformation scaled(TextDisplay display, float factor) {
        org.bukkit.util.Transformation transformation = display.getTransformation();
        transformation.getScale().set(factor, factor, factor);
        return transformation;
    }

    // ------------------------------------------------------------------ Plätze

    public int centreX(int index) {
        return index * spacing;
    }

    /** Wo der Besucher an dieser Haltestelle steht. */
    public Location visitorSpot(int index) {
        return new Location(world, centreX(index) + 0.5, height + 1.0, 0.5, VISITOR_YAW, 0f);
    }

    /** Wo der Führer steht: vor dem Besucher, mit dem Gesicht zu ihm. */
    public Location guideSpot(int index) {
        return new Location(world, centreX(index) + GUIDE_DISTANCE, height + 1.0, 0.5, VISITOR_YAW + 180f, 0f);
    }
}
