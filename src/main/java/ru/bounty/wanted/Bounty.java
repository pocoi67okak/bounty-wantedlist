package ru.bounty.wanted;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class Bounty {
    private final String id;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID ownerUuid;
    private final String ownerName;
    private final List<ItemStack> reward;
    private UUID hunterUuid;
    private String hunterName;

    public Bounty(
            String id,
            UUID targetUuid,
            String targetName,
            UUID ownerUuid,
            String ownerName,
            List<ItemStack> reward,
            UUID hunterUuid,
            String hunterName
    ) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.reward = cloneItems(reward);
        this.hunterUuid = hunterUuid;
        this.hunterName = hunterName;
    }

    public static Bounty create(UUID targetUuid, String targetName, Player owner, List<ItemStack> reward) {
        return new Bounty(
                UUID.randomUUID().toString(),
                targetUuid,
                targetName,
                owner.getUniqueId(),
                owner.getName(),
                reward,
                null,
                null
        );
    }

    public String getId() {
        return id;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public List<ItemStack> getReward() {
        return cloneItems(reward);
    }

    public UUID getHunterUuid() {
        return hunterUuid;
    }

    public String getHunterName() {
        return hunterName;
    }

    public boolean isTaken() {
        return hunterUuid != null;
    }

    public void take(Player hunter) {
        hunterUuid = hunter.getUniqueId();
        hunterName = hunter.getName();
    }

    public void decline() {
        hunterUuid = null;
        hunterName = null;
    }

    public boolean isTarget(Player player) {
        if (targetUuid != null && targetUuid.equals(player.getUniqueId())) {
            return true;
        }
        return targetName.equalsIgnoreCase(player.getName());
    }

    public String getTargetKey() {
        return targetName.toLowerCase(Locale.ROOT);
    }

    private static List<ItemStack> cloneItems(List<ItemStack> items) {
        List<ItemStack> cloned = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                cloned.add(item.clone());
            }
        }
        return cloned;
    }
}
