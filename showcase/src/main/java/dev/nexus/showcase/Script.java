package dev.nexus.showcase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Der Text der Führung: Haltestellen, Sätze und die Fragen, die ein Besucher stellen darf.
 *
 * Alles steht in tour_de.yml bzw. tour_en.yml. Wie lange ein Satz stehen bleibt, rechnet
 * diese Klasse aus seiner Länge aus — sonst müsste man für jeden Satz eine Zahl pflegen,
 * und die wäre nach der ersten Textänderung falsch.
 */
public final class Script {

    /**
     * Ein Satz des Führers, dazu optional etwas, das dabei passiert.
     *
     * @param equip  Cosmetic, das dem Besucher angelegt wird (oder null)
     * @param emote  Emote, das auf dem Besucher abgespielt wird (oder null)
     * @param action menu, crate, strip oder buy (oder null)
     * @param hold   Ticks, die der Satz stehen bleibt
     */
    public record Step(Component text, String equip, String emote, String action, int hold) {
    }

    public record Station(String id, Component title, Material accent, List<Step> steps) {
    }

    public record Question(String id, Component question, Material icon, List<Component> answer) {
    }

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Component prefix;
    private final Component guideIdle;
    private final Component questionItem;
    private final Component questionItemLore;
    private final Component questionTitle;
    private final Component questionBack;
    private final Component travelSubtitle;
    private final List<Station> stations;
    private final List<Question> questions;

    private Script(YamlConfiguration file, int ticksPerWord, int minHold) {
        this.prefix = text(file, "prefix");
        this.guideIdle = text(file, "guide-idle");
        this.questionItem = text(file, "question-item");
        this.questionItemLore = text(file, "question-item-lore");
        this.questionTitle = text(file, "question-title");
        this.questionBack = text(file, "question-back");
        this.travelSubtitle = text(file, "travel-subtitle");
        this.stations = readStations(file, ticksPerWord, minHold);
        this.questions = readQuestions(file);
    }

    /**
     * Liest die Datei des Spielers, greift aber für fehlende Einträge auf die mitgelieferte
     * zurück. So bleibt eine selbst angepasste Datei nach einem Update benutzbar.
     */
    public static Script load(JavaPlugin plugin, String language, int ticksPerWord, int minHold) {
        String name = "tour_" + (List.of("de", "en").contains(language) ? language : "en") + ".yml";
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        InputStream bundled = plugin.getResource(name);
        if (bundled != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
        return new Script(config, ticksPerWord, minHold);
    }

    // ------------------------------------------------------------------ Lesen

    private static List<Station> readStations(YamlConfiguration file, int ticksPerWord, int minHold) {
        List<Station> result = new ArrayList<>();
        for (Map<?, ?> raw : file.getMapList("stations")) {
            List<Step> steps = new ArrayList<>();
            Object rawSteps = raw.get("steps");
            if (rawSteps instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof Map<?, ?> step) {
                        steps.add(readStep(step, ticksPerWord, minHold));
                    }
                }
            }
            result.add(new Station(
                    String.valueOf(raw.get("id")),
                    MINI.deserialize(string(raw.get("title"), "")),
                    material(raw.get("accent"), Material.SMOOTH_QUARTZ),
                    List.copyOf(steps)));
        }
        return List.copyOf(result);
    }

    private static Step readStep(Map<?, ?> raw, int ticksPerWord, int minHold) {
        String text = string(raw.get("text"), "");
        int hold = raw.get("hold") instanceof Number number
                ? number.intValue()
                : Math.max(minHold, wordCount(text) * ticksPerWord);
        return new Step(
                MINI.deserialize(text),
                lower(raw.get("equip")),
                lower(raw.get("emote")),
                lower(raw.get("action")),
                hold);
    }

    private static List<Question> readQuestions(YamlConfiguration file) {
        List<Question> result = new ArrayList<>();
        for (Map<?, ?> raw : file.getMapList("questions")) {
            List<Component> answer = new ArrayList<>();
            if (raw.get("answer") instanceof List<?> lines) {
                lines.forEach(line -> answer.add(MINI.deserialize(String.valueOf(line))));
            }
            result.add(new Question(
                    String.valueOf(raw.get("id")),
                    MINI.deserialize(string(raw.get("question"), "?")),
                    material(raw.get("icon"), Material.PAPER),
                    List.copyOf(answer)));
        }
        return List.copyOf(result);
    }

    /** Wörter zählen reicht als Lesetempo — Zeichen zählen bestraft lange Wörter doppelt. */
    private static int wordCount(String text) {
        return text.isBlank() ? 1 : text.trim().split("\\s+").length;
    }

    private static Component text(YamlConfiguration file, String key) {
        return MINI.deserialize(file.getString(key, ""));
    }

    private static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private static String lower(Object value) {
        return value == null ? null : String.valueOf(value).toLowerCase(Locale.ROOT);
    }

    private static Material material(Object value, Material fallback) {
        if (value == null) {
            return fallback;
        }
        Material found = Material.matchMaterial(String.valueOf(value));
        return found != null ? found : fallback;
    }

    // ------------------------------------------------------------------ Zugriff

    public Component prefix() {
        return prefix;
    }

    public Component guideIdle() {
        return guideIdle;
    }

    public Component questionItem() {
        return questionItem;
    }

    public Component questionItemLore() {
        return questionItemLore;
    }

    public Component questionTitle() {
        return questionTitle;
    }

    public Component questionBack() {
        return questionBack;
    }

    public Component travelSubtitle() {
        return travelSubtitle;
    }

    public List<Station> stations() {
        return stations;
    }

    public List<Question> questions() {
        return questions;
    }
}
