package ru.bounty.wanted;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class BountyStorage {
    private final BountyPlugin plugin;
    private final File file;
    private final Map<String, Bounty> bounties = new LinkedHashMap<>();

    public BountyStorage(BountyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bounties.yml");
    }

    public void load() {
        bounties.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("bounties");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            String targetName = section.getString("target-name");
            String ownerName = section.getString("owner-name");
            UUID targetUuid = readUuid(section.getString("target-uuid"));
            UUID ownerUuid = readUuid(section.getString("owner-uuid"));
            UUID hunterUuid = readUuid(section.getString("hunter-uuid"));
            String hunterName = section.getString("hunter-name");
            List<ItemStack> reward = readReward(section);

            if (targetName == null || ownerName == null || ownerUuid == null || reward.isEmpty()) {
                plugin.getLogger().warning("Пропускаю поврежденную ставку " + id + " в bounties.yml");
                continue;
            }

            bounties.put(id, new Bounty(
                    id,
                    targetUuid,
                    targetName,
                    ownerUuid,
                    ownerName,
                    reward,
                    hunterUuid,
                    hunterName
            ));
        }
    }

    public void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Не удалось создать папку плагина: " + plugin.getDataFolder());
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("bounties");
        for (Bounty bounty : bounties.values()) {
            ConfigurationSection section = root.createSection(bounty.getId());
            section.set("target-uuid", bounty.getTargetUuid() == null ? null : bounty.getTargetUuid().toString());
            section.set("target-name", bounty.getTargetName());
            section.set("owner-uuid", bounty.getOwnerUuid().toString());
            section.set("owner-name", bounty.getOwnerName());
            section.set("hunter-uuid", bounty.getHunterUuid() == null ? null : bounty.getHunterUuid().toString());
            section.set("hunter-name", bounty.getHunterName());
            section.set("reward", bounty.getReward());
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить bounties.yml", exception);
        }
    }

    public void add(Bounty bounty) {
        bounties.put(bounty.getId(), bounty);
    }

    public void remove(String id) {
        bounties.remove(id);
    }

    public Optional<Bounty> find(String id) {
        return Optional.ofNullable(bounties.get(id));
    }

    public List<Bounty> getOpenBounties() {
        return bounties.values().stream()
                .filter(bounty -> !bounty.isTaken())
                .toList();
    }

    public List<Bounty> getOwnedBounties(UUID ownerUuid) {
        return bounties.values().stream()
                .filter(bounty -> bounty.getOwnerUuid().equals(ownerUuid))
                .toList();
    }

    public Optional<Bounty> getAcceptedBy(UUID hunterUuid) {
        return bounties.values().stream()
                .filter(bounty -> hunterUuid.equals(bounty.getHunterUuid()))
                .findFirst();
    }

    private static UUID readUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static List<ItemStack> readReward(ConfigurationSection section) {
        List<?> rawItems = section.getList("reward", Collections.emptyList());
        List<ItemStack> reward = new ArrayList<>();
        for (Object rawItem : rawItems) {
            if (rawItem instanceof ItemStack item && !item.getType().isAir()) {
                reward.add(item.clone());
            }
        }
        return reward;
    }
}
