package dev.nexus.cosmetics.command;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.menu.CosmeticMenu;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * /cosmetics         -> öffnet das Menü
 * /cosmetics off     -> legt alle Cosmetics ab
 * /cosmetics reload  -> lädt alle Konfigurationsdateien neu (nur Admins)
 */
public final class CosmeticsCommand implements BasicCommand {

    private static final String ADMIN_PERMISSION = "nexuscosmetics.admin";

    private final NexusCosmetics plugin;

    public CosmeticsCommand(NexusCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(plugin.messages().prefixed("no-permission"));
                return;
            }
            plugin.reloadAll();
            sender.sendMessage(plugin.messages().prefixed("reloaded",
                    Placeholder.unparsed("cosmetics", String.valueOf(plugin.cosmetics().registry().all().size())),
                    Placeholder.unparsed("emotes", String.valueOf(plugin.emotes().registry().all().size()))));
            return;
        }

        if (!(source.getExecutor() instanceof Player player)) {
            sender.sendMessage(plugin.messages().prefixed("only-players"));
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
            plugin.cosmetics().unequipAll(player);
            player.sendMessage(plugin.messages().prefixed("cosmetics.unequipped-all"));
            return;
        }

        new CosmeticMenu(plugin.cosmetics(), plugin.emotes(), plugin.messages(), player).open();
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length > 1) {
            return List.of();
        }
        List<String> options = new ArrayList<>(List.of("off"));
        if (source.getSender().hasPermission(ADMIN_PERMISSION)) {
            options.add("reload");
        }
        String typed = args.length == 0 ? "" : args[0].toLowerCase();
        return options.stream().filter(option -> option.startsWith(typed)).toList();
    }

    @Override
    public String permission() {
        return "nexuscosmetics.use";
    }
}
