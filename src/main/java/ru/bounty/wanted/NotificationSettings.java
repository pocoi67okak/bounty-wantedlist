package ru.bounty.wanted;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class NotificationSettings {
    private final BountyPlugin plugin;
    private final File file;
    private final Set<UUID> newBountyNotifications = new HashSet<>();

    public NotificationSettings(BountyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "settings.yml");
    }

    public void load() {
        newBountyNotifications.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("new-bounty-notifications");
        if (section == null) {
            return;
        }

        for (String rawUuid : section.getKeys(false)) {
            if (!section.getBoolean(rawUuid, false)) {
                continue;
            }
            try {
                newBountyNotifications.add(UUID.fromString(rawUuid));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Пропускаю неверный UUID в settings.yml: " + rawUuid);
            }
        }
    }

    public void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Не удалось создать папку плагина: " + plugin.getDataFolder());
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("new-bounty-notifications");
        for (UUID uuid : newBountyNotifications) {
            section.set(uuid.toString(), true);
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить settings.yml", exception);
        }
    }

    public boolean isNewBountyNotificationEnabled(UUID playerUuid) {
        return newBountyNotifications.contains(playerUuid);
    }

    public boolean toggleNewBountyNotification(UUID playerUuid) {
        if (newBountyNotifications.remove(playerUuid)) {
            save();
            return false;
        }
        newBountyNotifications.add(playerUuid);
        save();
        return true;
    }
}
