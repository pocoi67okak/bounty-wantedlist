package ru.bounty.wanted;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class KillListener implements Listener {
    private final BountyPlugin plugin;

    public KillListener(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player target = event.getEntity();
        Player killer = target.getKiller();
        if (killer == null) {
            return;
        }

        plugin.getStorage().getAcceptedBy(killer.getUniqueId())
                .filter(bounty -> bounty.isTarget(target))
                .ifPresent(bounty -> completeBounty(killer, target, bounty));
    }

    private void completeBounty(Player killer, Player target, Bounty bounty) {
        for (ItemStack reward : bounty.getReward()) {
            Map<Integer, ItemStack> overflow = killer.getInventory().addItem(reward);
            overflow.values().forEach(item -> killer.getWorld().dropItemNaturally(killer.getLocation(), item));
        }

        plugin.getStorage().remove(bounty.getId());
        plugin.getStorage().save();

        killer.sendMessage(BountyPlugin.PREFIX + "Ты выполнил ставку на §e" + target.getName() + "§f и получил награду.");

        Player owner = Bukkit.getPlayer(bounty.getOwnerUuid());
        if (owner != null) {
            owner.sendMessage(BountyPlugin.PREFIX + killer.getName() + " убил " + target.getName() + ".");
        }
    }
}
