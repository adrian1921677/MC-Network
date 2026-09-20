package dev.nexus.cosmetics.menu;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.config.Messages;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Fragt einen Spieler etwas über den Chat ab — für Dinge, die sich in einem Kisten-Menü nicht
 * eingeben lassen: den Suchbegriff und den Namen eines Outfits.
 *
 * Der Ablauf: Menü schließen, Frage anzeigen, auf die nächste Chat-Nachricht warten. Die Nachricht
 * wird abgefangen und landet nicht im öffentlichen Chat. "abbrechen" bricht ab.
 */
public final class ChatInputService implements Listener {

    private record Pending(Consumer<String> onInput, Runnable onCancel) {
    }

    private final NexusCosmetics plugin;
    private final Messages messages;
    private final Map<UUID, Pending> waiting = new ConcurrentHashMap<>();

    public ChatInputService(NexusCosmetics plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
    }

    /** Schließt das Menü, stellt die Frage und ruft danach onInput auf dem Server-Thread auf. */
    public void ask(Player player, String promptKey, Consumer<String> onInput, Runnable onCancel) {
        player.closeInventory();
        waiting.put(player.getUniqueId(), new Pending(onInput, onCancel));
        player.sendMessage(messages.prefixed(promptKey));
        player.sendMessage(messages.prefixed("menu.input-cancel-hint"));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Pending pending = waiting.remove(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }
        // Die Eingabe gehört uns und darf nicht im Chat aller Spieler landen
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        // Zurück auf den Server-Thread: Menüs dürfen nur von dort geöffnet werden
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!event.getPlayer().isOnline()) {
                return;
            }
            if (text.isEmpty() || isCancel(text)) {
                pending.onCancel().run();
            } else {
                pending.onInput().accept(text);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        waiting.remove(event.getPlayer().getUniqueId());
    }

    private boolean isCancel(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.equals("abbrechen") || lower.equals("cancel");
    }
}
