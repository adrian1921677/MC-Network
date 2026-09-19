package dev.nexus.cosmetics.command;

import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.emote.Emote;
import dev.nexus.cosmetics.emote.EmoteMenu;
import dev.nexus.cosmetics.emote.EmoteService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * /emote         -> öffnet das Emote-Menü
 * /emote <name>  -> spielt ein Emote direkt ab (z. B. /emote heart)
 */
public final class EmoteCommand implements BasicCommand {

    private final EmoteService service;

    public EmoteCommand(EmoteService service) {
        this.service = service;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getSender().sendMessage(Component.text("Nur Spieler können Emotes benutzen.", NamedTextColor.RED));
            return;
        }
        if (args.length == 0) {
            new EmoteMenu(service, player).open();
            return;
        }
        Emote emote = service.registry().get(args[0].toLowerCase());
        if (emote == null) {
            player.sendMessage(CosmeticManager.prefix().append(
                    Component.text("Unbekanntes Emote: " + args[0], NamedTextColor.RED)));
            return;
        }
        service.play(player, emote);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length > 1) {
            return List.of();
        }
        String typed = args.length == 0 ? "" : args[0].toLowerCase();
        return service.registry().all().stream()
                .map(Emote::id)
                .filter(id -> id.startsWith(typed))
                .toList();
    }

    @Override
    public String permission() {
        return "nexuscosmetics.use";
    }
}
