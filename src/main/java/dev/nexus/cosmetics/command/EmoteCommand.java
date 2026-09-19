package dev.nexus.cosmetics.command;

import dev.nexus.cosmetics.config.Messages;
import dev.nexus.cosmetics.emote.Emote;
import dev.nexus.cosmetics.emote.EmoteMenu;
import dev.nexus.cosmetics.emote.EmoteService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * /emote         -> öffnet das Emote-Menü
 * /emote <name>  -> spielt ein Emote direkt ab (z. B. /emote heart)
 */
public final class EmoteCommand implements BasicCommand {

    private final EmoteService service;
    private final Messages messages;

    public EmoteCommand(EmoteService service, Messages messages) {
        this.service = service;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getSender().sendMessage(messages.prefixed("only-players"));
            return;
        }
        if (args.length == 0) {
            new EmoteMenu(service, messages, player).open();
            return;
        }
        Emote emote = service.registry().get(args[0].toLowerCase());
        if (emote == null) {
            player.sendMessage(messages.prefixed("emotes.unknown", Placeholder.unparsed("name", args[0])));
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
