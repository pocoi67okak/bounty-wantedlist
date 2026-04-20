package ru.bounty.wanted;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

public final class ChatPromptListener implements Listener {
    private final BountyPlugin plugin;

    public ChatPromptListener(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.consumeTargetPrompt(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> handleTargetInput(player, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.cancelTargetPrompt(event.getPlayer().getUniqueId());
    }

    @SuppressWarnings("deprecation")
    private void handleTargetInput(Player player, String message) {
        if (!player.isOnline()) {
            return;
        }

        if (message.equalsIgnoreCase("отмена") || message.equalsIgnoreCase("cancel")) {
            player.sendMessage(BountyPlugin.PREFIX + "Создание ставки отменено.");
            return;
        }

        if (!isValidName(message)) {
            plugin.startTargetPrompt(player);
            player.sendMessage(BountyPlugin.PREFIX + "Напиши обычный ник Minecraft или §eОтмена§f для отмены.");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(message);
        plugin.getMenus().openDeposit(player, target.getName() == null ? message : target.getName(), target.getUniqueId());
    }

    private static boolean isValidName(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.length() >= 3
                && normalized.length() <= 16
                && normalized.matches("[a-z0-9_]+");
    }
}
