package dev.nexus.showcase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Fragen, die ein Besucher unterwegs stellen darf.
 *
 * Es sind bewusst feste Fragen und keine Freitext-Eingabe: So weiß der Führer immer eine
 * Antwort, und die Antworten bleiben die, die der Verkäufer geben will.
 */
public final class QuestionMenu implements InventoryHolder {

    private static final int SIZE = 36;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25};
    private static final int CLOSE_SLOT = 31;

    private final Tour tour;
    private final Inventory inventory;
    private final Map<Integer, Script.Question> slots = new HashMap<>();

    public QuestionMenu(Script script, Tour tour) {
        this.tour = tour;
        this.inventory = Bukkit.createInventory(this, SIZE, script.questionTitle());

        List<Script.Question> questions = script.questions();
        for (int i = 0; i < SLOTS.length && i < questions.size(); i++) {
            Script.Question question = questions.get(i);
            inventory.setItem(SLOTS[i], icon(question));
            slots.put(SLOTS[i], question);
        }
        inventory.setItem(CLOSE_SLOT, button(Material.BARRIER, script.questionBack()));
    }

    private static ItemStack icon(Script.Question question) {
        List<Component> lore = new ArrayList<>();
        question.answer().forEach(line ->
                lore.add(line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        ItemStack item = ItemStack.of(question.icon());
        item.editMeta(meta -> {
            meta.itemName(question.question());
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
        });
        return item;
    }

    private static ItemStack button(Material material, Component name) {
        ItemStack item = ItemStack.of(material);
        item.editMeta(meta -> {
            meta.itemName(name);
            meta.addItemFlags(ItemFlag.values());
        });
        return item;
    }

    public void open() {
        tour.visitor().openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(int slot) {
        if (slot == CLOSE_SLOT) {
            tour.visitor().closeInventory();
            return;
        }
        Script.Question question = slots.get(slot);
        if (question == null) {
            return;
        }
        // Menue zu, damit der Besucher den Fuehrer beim Antworten auch sieht
        tour.visitor().closeInventory();
        tour.visitor().playSound(tour.visitor().getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
        tour.answer(question);
    }
}
