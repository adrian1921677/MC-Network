package dev.nexus.cosmetics.command;

import dev.nexus.cosmetics.NexusCosmetics;
import dev.nexus.cosmetics.cosmetic.Cosmetic;
import dev.nexus.cosmetics.crate.Crate;
import dev.nexus.cosmetics.emote.Emote;
import dev.nexus.cosmetics.menu.CosmeticMenu;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * /cosmetics                                   -> öffnet das Menü
 * /cosmetics off                               -> legt alle Cosmetics ab
 * /cosmetics item                              -> gibt das Menü-Item (Geschenk) zurück
 * /cosmetics reload                            -> lädt alle Dateien neu (Admin)
 * /cosmetics givekey <spieler> <truhe> [anzahl] -> Truhen-Schlüssel geben (Admin, z. B. für Shops)
 * /cosmetics give <spieler> <cosmetic|emote:id> -> Cosmetic/Emote dauerhaft freischalten (Admin)
 *
 * Admin-Befehle funktionieren auch über die Konsole und für Offline-Spieler.
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
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";

        if (List.of("reload", "givekey", "give").contains(sub)) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(plugin.messages().prefixed("no-permission"));
                return;
            }
            switch (sub) {
                case "reload" -> reload(sender);
                case "givekey" -> giveKey(sender, args);
                default -> give(sender, args);
            }
            return;
        }

        if (!(source.getExecutor() instanceof Player player)) {
            sender.sendMessage(plugin.messages().prefixed("only-players"));
            return;
        }

        if (sub.equals("item")) {
            plugin.menuItem().give(player, true);
            return;
        }

        if (sub.equals("off")) {
            plugin.cosmetics().unequipAll(player);
            player.sendMessage(plugin.messages().prefixed("cosmetics.unequipped-all"));
            return;
        }

        new CosmeticMenu(plugin, player).open();
    }

    private void reload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(plugin.messages().prefixed("reloaded",
                Placeholder.unparsed("cosmetics", String.valueOf(plugin.cosmetics().registry().all().size())),
                Placeholder.unparsed("emotes", String.valueOf(plugin.emotes().registry().all().size()))));
    }

    private void giveKey(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.messages().prefixed("crates.usage-givekey"));
            return;
        }
        OfflinePlayer target = findPlayer(sender, args[1]);
        Crate crate = plugin.crates().registry().get(args[2].toLowerCase(Locale.ROOT));
        if (target == null) {
            return;
        }
        if (crate == null) {
            sender.sendMessage(plugin.messages().prefixed("crates.unknown-crate", Placeholder.unparsed("name", args[2])));
            return;
        }
        int amount;
        try {
            amount = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        } catch (NumberFormatException exception) {
            sender.sendMessage(plugin.messages().prefixed("crates.usage-givekey"));
            return;
        }

        plugin.cosmetics().modifyProfile(target.getUniqueId(), profile -> profile.addKeys(crate.id(), amount));
        sender.sendMessage(plugin.messages().prefixed("crates.key-given",
                Placeholder.unparsed("player", String.valueOf(target.getName())),
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.component("crate", crate.displayName())));
        if (target.getPlayer() != null && amount > 0) {
            target.getPlayer().sendMessage(plugin.messages().prefixed("crates.key-received",
                    Placeholder.unparsed("amount", String.valueOf(amount)),
                    Placeholder.component("crate", crate.displayName())));
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.messages().prefixed("crates.usage-give"));
            return;
        }
        OfflinePlayer target = findPlayer(sender, args[1]);
        if (target == null) {
            return;
        }
        String key = args[2].toLowerCase(Locale.ROOT);
        Component name;
        if (key.startsWith("emote:")) {
            Emote emote = plugin.emotes().registry().get(key.substring(6));
            name = emote == null ? null : emote.displayName();
        } else {
            Cosmetic cosmetic = plugin.cosmetics().registry().get(key);
            name = cosmetic == null ? null : cosmetic.displayName();
        }
        if (name == null) {
            sender.sendMessage(plugin.messages().prefixed("crates.unknown-reward", Placeholder.unparsed("name", args[2])));
            return;
        }
        plugin.cosmetics().modifyProfile(target.getUniqueId(), profile -> profile.owned().add(key));
        sender.sendMessage(plugin.messages().prefixed("crates.unlock-given",
                Placeholder.unparsed("player", String.valueOf(target.getName())),
                Placeholder.component("name", name)));
    }

    /** Sucht einen Spieler, auch wenn er offline ist (er muss aber schon einmal auf dem Server gewesen sein). */
    @SuppressWarnings("deprecation")
    private OfflinePlayer findPlayer(CommandSender sender, String name) {
        OfflinePlayer target = Bukkit.getPlayerExact(name);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(name);
            if (!target.hasPlayedBefore()) {
                sender.sendMessage(plugin.messages().prefixed("crates.unknown-player", Placeholder.unparsed("name", name)));
                return null;
            }
        }
        return target;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        boolean admin = source.getSender().hasPermission(ADMIN_PERMISSION);
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();

        if (args.length <= 1) {
            options.add("off");
            options.add("item");
            if (admin) {
                options.addAll(List.of("reload", "givekey", "give"));
            }
        } else if (admin && args.length == 2 && List.of("givekey", "give").contains(args[0].toLowerCase(Locale.ROOT))) {
            Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
        } else if (admin && args.length == 3 && args[0].equalsIgnoreCase("givekey")) {
            plugin.crates().registry().all().forEach(crate -> options.add(crate.id()));
        } else if (admin && args.length == 3 && args[0].equalsIgnoreCase("give")) {
            Stream.concat(
                    plugin.cosmetics().registry().all().stream().map(Cosmetic::ownershipKey),
                    plugin.emotes().registry().all().stream().map(Emote::ownershipKey)
            ).forEach(options::add);
        }
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    @Override
    public String permission() {
        return "nexuscosmetics.use";
    }
}
