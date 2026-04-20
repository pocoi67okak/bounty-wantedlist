package ru.bounty.wanted;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BountyPlugin extends JavaPlugin {
    public static final String PREFIX = "§6[Баунти] §f";

    private final Set<UUID> targetPrompts = ConcurrentHashMap.newKeySet();
    private BountyStorage storage;
    private MenuManager menus;

    @Override
    public void onEnable() {
        storage = new BountyStorage(this);
        storage.load();

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
    }

    public BountyStorage getStorage() {
        return storage;
    }

    public MenuManager getMenus() {
        return menus;
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
