package dev.nexus.showcase;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * /tour          -> Führung (neu) starten
 * /tour stop     -> Führung beenden, Inventar zurück
 * /tour reload   -> Texte neu einlesen (Admin)
 * /tour build    -> Haltestellen neu bauen (Admin)
 */
public final class TourCommand implements BasicCommand {

    private static final String ADMIN = "nexusshowcase.admin";

    private final NexusShowcase plugin;

    public TourCommand(NexusShowcase plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";

        if (sub.equals("reload") || sub.equals("build")) {
            if (!sender.hasPermission(ADMIN)) {
                sender.sendMessage(Component.text("Dafür fehlt dir das Recht.", NamedTextColor.RED));
                return;
            }
            if (sub.equals("reload")) {
                plugin.reloadEverything();
                sender.sendMessage(Component.text("Texte neu eingelesen: "
                        + plugin.script().stations().size() + " Haltestellen, "
                        + plugin.script().questions().size() + " Fragen.", NamedTextColor.GREEN));
            } else {
                plugin.showcaseWorld().build(plugin.script().stations());
                sender.sendMessage(Component.text("Haltestellen neu gebaut.", NamedTextColor.GREEN));
            }
            return;
        }

        if (!(source.getExecutor() instanceof Player player)) {
            sender.sendMessage(Component.text("Das geht nur im Spiel.", NamedTextColor.RED));
            return;
        }

        if (sub.equals("stop")) {
            plugin.tours().end(player, true);
            player.sendMessage(Component.text("Führung beendet.", NamedTextColor.GRAY));
            return;
        }
        plugin.tours().begin(player);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length > 1) {
            return List.of();
        }
        String typed = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        List<String> options = source.getSender().hasPermission(ADMIN)
                ? List.of("stop", "reload", "build")
                : List.of("stop");
        return options.stream().filter(option -> option.startsWith(typed)).toList();
    }
}
