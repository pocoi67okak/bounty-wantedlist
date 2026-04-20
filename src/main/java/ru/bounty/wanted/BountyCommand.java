package ru.bounty.wanted;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BountyCommand implements CommandExecutor, TabCompleter {
    private final BountyPlugin plugin;

    public BountyCommand(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда доступна только игроку.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("decline")) {
            decline(player);
            return true;
        }

        plugin.getMenus().openMain(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String typed = args[0].toLowerCase(Locale.ROOT);
        if ("decline".startsWith(typed)) {
            return List.of("decline");
        }
        return new ArrayList<>();
    }

    private void decline(Player player) {
        plugin.getStorage().getAcceptedBy(player.getUniqueId()).ifPresentOrElse(bounty -> {
            bounty.decline();
            plugin.getStorage().save();
            player.sendMessage(BountyPlugin.PREFIX + "Ты отказался от ставки на §e" + bounty.getTargetName() + "§f.");
        }, () -> player.sendMessage(BountyPlugin.PREFIX + "У тебя нет взятой ставки."));
    }
}
