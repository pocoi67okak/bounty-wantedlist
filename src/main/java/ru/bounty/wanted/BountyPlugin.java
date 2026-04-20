package ru.bounty.wanted;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BountyPlugin extends JavaPlugin {
    public static final String PREFIX = "§6[Баунти] §f";

    private final Set<UUID> targetPrompts = ConcurrentHashMap.newKeySet();
    private BountyStorage storage;
    private NotificationSettings settings;
    private MenuManager menus;

    @Override
    public void onEnable() {
        storage = new BountyStorage(this);
        storage.load();
        settings = new NotificationSettings(this);
        settings.load();

        menus = new MenuManager(this);
        BountyCommand command = new BountyCommand(this);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("wantedlist"), "wantedlist command");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        Bukkit.getPluginManager().registerEvents(menus, this);
        Bukkit.getPluginManager().registerEvents(new ChatPromptListener(this), this);
        Bukkit.getPluginManager().registerEvents(new KillListener(this), this);
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.save();
        }
        if (settings != null) {
            settings.save();
        }
    }

    public BountyStorage getStorage() {
        return storage;
    }

    public MenuManager getMenus() {
        return menus;
    }

    public NotificationSettings getSettings() {
        return settings;
    }

    public void notifyNewBounty(Bounty bounty) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().equals(bounty.getOwnerUuid())) {
                continue;
            }
            if (!settings.isNewBountyNotificationEnabled(player.getUniqueId())) {
                continue;
            }

            TextComponent message = new TextComponent(PREFIX + "Новая ставка! "
                    + bounty.getOwnerName() + " поставил награду за " + bounty.getTargetName() + ". ");
            TextComponent button = new TextComponent("§e[Жми для просмотра награды]");
            button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wantedlist open " + bounty.getId()));
            button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Открыть награду").create()));
            player.spigot().sendMessage(message, button);
        }
    }

    public void startTargetPrompt(Player player) {
        targetPrompts.add(player.getUniqueId());
    }

    public boolean consumeTargetPrompt(UUID playerUuid) {
        return targetPrompts.remove(playerUuid);
    }

    public void cancelTargetPrompt(UUID playerUuid) {
        targetPrompts.remove(playerUuid);
    }
}
