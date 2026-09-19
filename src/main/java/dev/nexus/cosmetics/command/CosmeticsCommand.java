package dev.nexus.cosmetics.command;

import dev.nexus.cosmetics.cosmetic.CosmeticManager;
import dev.nexus.cosmetics.menu.CosmeticMenu;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * /cosmetics      -> öffnet das Menü
 * /cosmetics off  -> legt das aktuelle Cosmetic ab
 */
public final class CosmeticsCommand implements BasicCommand {

    private final CosmeticManager manager;

    public CosmeticsCommand(CosmeticManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getSender().sendMessage(Component.text("Nur Spieler können Cosmetics benutzen.", NamedTextColor.RED));
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
            manager.unequipAll(player);
            player.sendMessage(CosmeticManager.prefix().append(Component.text("Cosmetic abgelegt.", NamedTextColor.GRAY)));
            return;
        }

        new CosmeticMenu(manager, player).open();
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        return args.length <= 1 ? List.of("off") : List.of();
    }

    @Override
    public String permission() {
        return "nexuscosmetics.use";
    }
}
