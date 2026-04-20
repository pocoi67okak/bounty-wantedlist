package ru.bounty.wanted;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MenuManager implements Listener {
    private static final int INVENTORY_SIZE = 54;
    private static final int REWARD_LIMIT = 45;
    private static final int BACK_SLOT = 49;
    private static final int ACCEPT_SLOT = 47;
    private static final int CANCEL_SLOT = 51;

    private final BountyPlugin plugin;

    public MenuManager(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        MainHolder holder = new MainHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, color("&6Баунти"));
        holder.attach(inventory);

        inventory.setItem(11, named(Material.PLAYER_HEAD, "&eСписок розыска", "&7Посмотреть активные ставки."));
        inventory.setItem(13, named(Material.NAME_TAG, "&aПоставить розыск", "&7Выбрать игрока и положить награду."));
        inventory.setItem(15, named(Material.CHEST, "&bМои ставки", "&7Посмотреть твои созданные ставки."));

        player.openInventory(inventory);
    }

    public void openWantedList(Player player) {
        WantedListHolder holder = new WantedListHolder();
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, color("&6Список розыска"));
        holder.attach(inventory);

        int slot = 0;
        for (Bounty bounty : plugin.getStorage().getOpenBounties()) {
            if (slot >= REWARD_LIMIT) {
                break;
            }
            inventory.setItem(slot, head(bounty.getTargetUuid(), bounty.getTargetName(),
                    "&e" + bounty.getTargetName(),
                    "&7Нажми, чтобы посмотреть награду."));
            holder.slotToBounty.put(slot, bounty.getId());
            slot++;
        }

        if (slot == 0) {
            inventory.setItem(22, named(Material.PAPER, "&fСписок пуст", "&7Пока никто не поставил розыск."));
        }

        inventory.setItem(BACK_SLOT, named(Material.ARROW, "&eНазад", "&7Вернуться в главное меню."));
        player.openInventory(inventory);
    }

    public void openBountyDetails(Player player, String bountyId) {
        plugin.getStorage().find(bountyId).ifPresentOrElse(bounty -> {
            BountyDetailsHolder holder = new BountyDetailsHolder(bountyId);
            Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, color("&6Розыск: &e" + bounty.getTargetName()));
            holder.attach(inventory);

            placeRewardPreview(inventory, bounty.getReward());
            inventory.setItem(ACCEPT_SLOT, named(Material.EMERALD_BLOCK, "&aПринять", "&7Ты возьмешь эту ставку."));
            inventory.setItem(CANCEL_SLOT, named(Material.BARRIER, "&cОтмена", "&7Вернуться в меню розыска."));
            player.openInventory(inventory);
        }, () -> {
            player.sendMessage(BountyPlugin.PREFIX + "Эта ставка уже недоступна.");
            openWantedList(player);
        });
    }

    public void openDeposit(Player player, String targetName, UUID targetUuid) {
        DepositHolder holder = new DepositHolder(targetName, targetUuid);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, color("&6Ставка: &e" + targetName));
        holder.attach(inventory);

        fillBottomControls(inventory);
        inventory.setItem(ACCEPT_SLOT, named(Material.EMERALD_BLOCK, "&aПоставить", "&7Создать ставку с этими ресурсами."));
        inventory.setItem(CANCEL_SLOT, named(Material.BARRIER, "&cОтмена", "&7Забрать ресурсы и закрыть меню."));
        player.openInventory(inventory);
    }

    public void openMyBounties(Player player) {
        MyBountiesHolder holder = new MyBountiesHolder();
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, color("&6Мои ставки"));
        holder.attach(inventory);

        int slot = 0;
        for (Bounty bounty : plugin.getStorage().getOwnedBounties(player.getUniqueId())) {
            if (slot >= REWARD_LIMIT) {
                break;
            }
            String status = bounty.isTaken()
                    ? "&cСтавку взял: " + bounty.getHunterName()
                    : "&aСтавку еще никто не взял";
            inventory.setItem(slot, head(bounty.getTargetUuid(), bounty.getTargetName(),
                    "&e" + bounty.getTargetName(),
                    color(status),
                    color("&7Нажми, чтобы открыть.")));
            holder.slotToBounty.put(slot, bounty.getId());
            slot++;
        }

        if (slot == 0) {
            inventory.setItem(22, named(Material.PAPER, "&fСписок пуст", "&7У тебя нет созданных ставок."));
        }

        inventory.setItem(BACK_SLOT, named(Material.ARROW, "&eНазад", "&7Вернуться в главное меню."));
        player.openInventory(inventory);
    }

    public void openOwnedBounty(Player player, String bountyId) {
        plugin.getStorage().find(bountyId).ifPresentOrElse(bounty -> {
            if (!bounty.getOwnerUuid().equals(player.getUniqueId())) {
                player.sendMessage(BountyPlugin.PREFIX + "Это не твоя ставка.");
                openMyBounties(player);
                return;
            }

            OwnedBountyHolder holder = new OwnedBountyHolder(bountyId);
            Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, color("&6Моя ставка: &e" + bounty.getTargetName()));
            holder.attach(inventory);

            placeRewardPreview(inventory, bounty.getReward());
            String lore = bounty.isTaken()
                    ? "&7Ставку уже приняли. Забрать нельзя."
                    : "&7Ставка будет отменена, ресурсы вернутся.";
            inventory.setItem(ACCEPT_SLOT, named(Material.REDSTONE_BLOCK, "&cОтменить ставку", lore));
            inventory.setItem(BACK_SLOT, named(Material.ARROW, "&eНазад", "&7Вернуться к моим ставкам."));
            inventory.setItem(CANCEL_SLOT, named(Material.CHEST, "&aЗабрать ставку", lore));
            player.openInventory(inventory);
        }, () -> {
            player.sendMessage(BountyPlugin.PREFIX + "Эта ставка уже недоступна.");
            openMyBounties(player);
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof MenuHolder)) {
            return;
        }

        if (holder instanceof DepositHolder depositHolder) {
            handleDepositClick(event, player, depositHolder);
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= topInventory.getSize()) {
            return;
        }

        if (holder instanceof MainHolder) {
            handleMainClick(player, event.getRawSlot());
        } else if (holder instanceof WantedListHolder wantedListHolder) {
            handleWantedListClick(player, wantedListHolder, event.getRawSlot());
        } else if (holder instanceof BountyDetailsHolder detailsHolder) {
            handleBountyDetailsClick(player, detailsHolder, event.getRawSlot());
        } else if (holder instanceof MyBountiesHolder myBountiesHolder) {
            handleMyBountiesClick(player, myBountiesHolder, event.getRawSlot());
        } else if (holder instanceof OwnedBountyHolder ownedBountyHolder) {
            handleOwnedBountyClick(player, ownedBountyHolder, event.getRawSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof MenuHolder)) {
            return;
        }

        if (holder instanceof DepositHolder) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topInventory.getSize() && rawSlot >= REWARD_LIMIT) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topInventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof DepositHolder depositHolder) || depositHolder.finished) {
            return;
        }

        depositHolder.finished = true;
        returnRewardItems((Player) event.getPlayer(), event.getInventory());
        event.getPlayer().sendMessage(BountyPlugin.PREFIX + "Создание ставки отменено, ресурсы возвращены.");
    }

    private void handleMainClick(Player player, int rawSlot) {
        if (rawSlot == 11) {
            openWantedList(player);
        } else if (rawSlot == 13) {
            plugin.startTargetPrompt(player);
            player.closeInventory();
            player.sendMessage(BountyPlugin.PREFIX + "Напиши ник человека или §eОтмена§f для отмены.");
        } else if (rawSlot == 15) {
            openMyBounties(player);
        }
    }

    private void handleWantedListClick(Player player, WantedListHolder holder, int rawSlot) {
        if (rawSlot == BACK_SLOT) {
            openMain(player);
            return;
        }

        String bountyId = holder.slotToBounty.get(rawSlot);
        if (bountyId != null) {
            openBountyDetails(player, bountyId);
        }
    }

    private void handleBountyDetailsClick(Player player, BountyDetailsHolder holder, int rawSlot) {
        if (rawSlot == CANCEL_SLOT) {
            openWantedList(player);
            return;
        }

        if (rawSlot != ACCEPT_SLOT) {
            return;
        }

        if (plugin.getStorage().getAcceptedBy(player.getUniqueId()).isPresent()) {
            player.sendMessage(BountyPlugin.PREFIX + "У тебя уже есть взятая ставка. Отказ: §e/wantedlist decline§f.");
            player.closeInventory();
            return;
        }

        plugin.getStorage().find(holder.bountyId).ifPresentOrElse(bounty -> {
            if (bounty.isTaken()) {
                player.sendMessage(BountyPlugin.PREFIX + "Эту ставку уже взяли.");
                openWantedList(player);
                return;
            }
            bounty.take(player);
            plugin.getStorage().save();
            player.closeInventory();
            player.sendMessage(BountyPlugin.PREFIX + "Ты принял ставку на §e" + bounty.getTargetName() + "§f. Отказ: §e/wantedlist decline§f.");
        }, () -> {
            player.sendMessage(BountyPlugin.PREFIX + "Эта ставка уже недоступна.");
            openWantedList(player);
        });
    }

    private void handleDepositClick(InventoryClickEvent event, Player player, DepositHolder holder) {
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize()) {
            if (rawSlot < REWARD_LIMIT) {
                event.setCancelled(false);
                return;
            }

            event.setCancelled(true);
            if (rawSlot == ACCEPT_SLOT) {
                placeBounty(player, holder, event.getView().getTopInventory());
            } else if (rawSlot == CANCEL_SLOT) {
                holder.finished = true;
                returnRewardItems(player, event.getView().getTopInventory());
                player.closeInventory();
                player.sendMessage(BountyPlugin.PREFIX + "Создание ставки отменено, ресурсы возвращены.");
            }
            return;
        }

        event.setCancelled(false);
    }

    private void placeBounty(Player player, DepositHolder holder, Inventory inventory) {
        List<ItemStack> reward = collectReward(inventory);
        if (reward.isEmpty()) {
            player.sendMessage(BountyPlugin.PREFIX + "Положи награду в верхнюю часть меню.");
            return;
        }

        clearRewardSlots(inventory);
        holder.finished = true;

        Bounty bounty = Bounty.create(holder.targetUuid, holder.targetName, player, reward);
        plugin.getStorage().add(bounty);
        plugin.getStorage().save();

        player.closeInventory();
        player.sendMessage(BountyPlugin.PREFIX + "Розыск на §e" + holder.targetName + "§f поставлен.");
    }

    private void handleMyBountiesClick(Player player, MyBountiesHolder holder, int rawSlot) {
        if (rawSlot == BACK_SLOT) {
            openMain(player);
            return;
        }

        String bountyId = holder.slotToBounty.get(rawSlot);
        if (bountyId != null) {
            openOwnedBounty(player, bountyId);
        }
    }

    private void handleOwnedBountyClick(Player player, OwnedBountyHolder holder, int rawSlot) {
        if (rawSlot == BACK_SLOT) {
            openMyBounties(player);
            return;
        }

        if (rawSlot != ACCEPT_SLOT && rawSlot != CANCEL_SLOT) {
            return;
        }

        plugin.getStorage().find(holder.bountyId).ifPresentOrElse(bounty -> {
            if (!bounty.getOwnerUuid().equals(player.getUniqueId())) {
                player.sendMessage(BountyPlugin.PREFIX + "Это не твоя ставка.");
                openMyBounties(player);
                return;
            }
            if (bounty.isTaken()) {
                player.sendMessage(BountyPlugin.PREFIX + "Эту ставку уже взяли, забрать ресурсы нельзя.");
                player.closeInventory();
                return;
            }

            giveItems(player, bounty.getReward());
            plugin.getStorage().remove(bounty.getId());
            plugin.getStorage().save();
            player.closeInventory();
            player.sendMessage(BountyPlugin.PREFIX + "Ставка на §e" + bounty.getTargetName() + "§f отменена, ресурсы возвращены.");
        }, () -> {
            player.sendMessage(BountyPlugin.PREFIX + "Эта ставка уже недоступна.");
            openMyBounties(player);
        });
    }

    private void placeRewardPreview(Inventory inventory, List<ItemStack> reward) {
        int slot = 0;
        for (ItemStack item : reward) {
            if (slot >= REWARD_LIMIT) {
                break;
            }
            inventory.setItem(slot, item.clone());
            slot++;
        }
    }

    private List<ItemStack> collectReward(Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < REWARD_LIMIT; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }
        return items;
    }

    private void clearRewardSlots(Inventory inventory) {
        for (int slot = 0; slot < REWARD_LIMIT; slot++) {
            inventory.setItem(slot, null);
        }
    }

    private void returnRewardItems(Player player, Inventory inventory) {
        List<ItemStack> items = collectReward(inventory);
        clearRewardSlots(inventory);
        giveItems(player, items);
    }

    private void giveItems(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
            overflow.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    private void fillBottomControls(Inventory inventory) {
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ", "");
        for (int slot = REWARD_LIMIT; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack head(UUID ownerUuid, String ownerName, String displayName, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (rawMeta instanceof SkullMeta meta) {
            OfflinePlayer offlinePlayer = ownerUuid == null
                    ? Bukkit.getOfflinePlayer(ownerName)
                    : Bukkit.getOfflinePlayer(ownerUuid);
            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(color(displayName));
            meta.setLore(colorLore(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack named(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(displayName));
            meta.setLore(colorLore(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<String> colorLore(String... lines) {
        List<String> colored = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                colored.add(color(line));
            }
        }
        return colored;
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private abstract static class MenuHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void attach(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    private static final class MainHolder extends MenuHolder {
    }

    private static final class WantedListHolder extends MenuHolder {
        private final Map<Integer, String> slotToBounty = new HashMap<>();
    }

    private static final class BountyDetailsHolder extends MenuHolder {
        private final String bountyId;

        private BountyDetailsHolder(String bountyId) {
            this.bountyId = bountyId;
        }
    }

    private static final class DepositHolder extends MenuHolder {
        private final String targetName;
        private final UUID targetUuid;
        private boolean finished;

        private DepositHolder(String targetName, UUID targetUuid) {
            this.targetName = targetName;
            this.targetUuid = targetUuid;
        }
    }

    private static final class MyBountiesHolder extends MenuHolder {
        private final Map<Integer, String> slotToBounty = new HashMap<>();
    }

    private static final class OwnedBountyHolder extends MenuHolder {
        private final String bountyId;

        private OwnedBountyHolder(String bountyId) {
            this.bountyId = bountyId;
        }
    }
}
