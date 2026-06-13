package net.leafmc.friends;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class FriendGui implements Listener {
    private static final int SIZE = 54;
    private static final int[] TELEPORT_REQUEST_SLOTS = {1, 2, 3, 5, 6, 7};
    private static final int[] FRIEND_REQUEST_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] FRIEND_SLOTS = {
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private final LeafFriendsPlugin plugin;

    public FriendGui(LeafFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        plugin.friends().ensureProfile(player.getUniqueId(), player.getName());
        MenuHolder holder = new MenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, plugin.getConfig().getString("gui.title", "§0好友菜单"));
        holder.inventory = inventory;

        List<FriendProfile> incomingFriends = plugin.friends().incomingRequests(player.getUniqueId());
        List<UUID> incomingTeleports = plugin.teleports().incomingRequests(player.getUniqueId());
        List<FriendService.FriendSummary> friends = plugin.friends().listFriends(player.getUniqueId());

        inventory.setItem(4, item(Material.COMPASS, "§b好友菜单", List.of(
            "§7待处理好友申请: §f" + incomingFriends.size(),
            "§7待处理传送请求: §f" + incomingTeleports.size(),
            "§7好友数量: §f" + friends.size()
        )));

        addTeleportRequests(holder, inventory, incomingTeleports);
        addFriendRequests(holder, inventory, incomingFriends);
        addFriendList(holder, inventory, friends);
        addSettings(holder, inventory, player);

        inventory.setItem(53, item(Material.NETHER_STAR, "§a回到社交菜单", List.of("§7打开玩家社交菜单")));
        holder.actions.put(53, new GuiAction(ActionType.RUN_COMMAND, null, "menusocial"));

        player.openInventory(inventory);
    }

    public void openDetails(Player player, UUID friendId) {
        FriendProfile viewer = plugin.friends().ensureProfile(player.getUniqueId(), player.getName());
        FriendProfile friend = plugin.friends().profile(friendId).orElse(null);
        if (friend == null || !plugin.friends().areFriends(player.getUniqueId(), friendId)) {
            player.sendMessage(plugin.prefix() + "找不到这个好友。");
            open(player);
            return;
        }

        MenuHolder holder = new MenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, "§0好友资料: " + friend.latestName());
        holder.inventory = inventory;

        Player onlineFriend = Bukkit.getPlayer(friendId);
        boolean visibleOnline = onlineFriend != null && onlineFriend.isOnline() && friend.settings().showOnlineStatus();
        boolean trustedByFriend = plugin.friends().isTrustedTeleporter(friendId, player.getUniqueId());
        boolean trustsFriend = viewer.trustedTeleporters().contains(friendId);

        inventory.setItem(4, item(Material.PLAYER_HEAD, (visibleOnline ? "§a" : "§7") + friend.latestName(), List.of(
            visibleOnline ? "§a在线" : "§7离线或隐藏在线状态",
            trustedByFriend ? "§a对方允许你免确认传送" : "§7对方需要手动同意你的传送",
            trustsFriend ? "§a你允许对方免确认传送" : "§7你未允许对方免确认传送"
        )));

        inventory.setItem(20, item(Material.ENDER_PEARL, "§a申请传送", List.of(
            trustedByFriend ? "§7点击后直接传送到对方身边。" : "§7点击后向对方发送传送请求。",
            "§7对方可以在聊天按钮或好友菜单里处理。"
        )));
        holder.actions.put(20, new GuiAction(ActionType.REQUEST_TELEPORT, friendId, null));

        inventory.setItem(21, item(Material.WRITABLE_BOOK, "§b好友私聊", List.of(
            "§7点击后自动填入私聊格式。",
            "§7你只需要继续输入内容。"
        )));
        holder.actions.put(21, new GuiAction(ActionType.MESSAGE_HINT, friendId, null));

        inventory.setItem(23, item(trustsFriend ? Material.LIME_DYE : Material.RED_DYE, trustsFriend ? "§a已允许免确认传送" : "§c未允许免确认传送", List.of(
            "§7控制这个好友是否可以直接传送到你身边。",
            trustsFriend ? "§7点击取消授权。" : "§7点击允许对方免确认传送。"
        )));
        holder.actions.put(23, new GuiAction(ActionType.TOGGLE_TRUST, friendId, null));

        inventory.setItem(24, item(Material.REDSTONE_BLOCK, "§c删除好友", List.of(
            "§7点击后解除好友关系。",
            "§7同时清除双方免确认传送授权。"
        )));
        holder.actions.put(24, new GuiAction(ActionType.REMOVE_FRIEND, friendId, null));

        inventory.setItem(25, item(Material.BARRIER, "§4拉黑玩家", List.of(
            "§7点击后拉黑并解除好友。",
            "§7会阻止申请、私聊和好友传送。"
        )));
        holder.actions.put(25, new GuiAction(ActionType.BLOCK_FRIEND, friendId, null));

        inventory.setItem(49, item(Material.ARROW, "§e返回好友列表", List.of("§7回到好友菜单")));
        holder.actions.put(49, new GuiAction(ActionType.BACK, null, null));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner.equals(player.getUniqueId())) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        GuiAction action = holder.actions.get(slot);
        if (action == null) {
            return;
        }
        handleAction(player, action, event.getClick());
    }

    private void addTeleportRequests(MenuHolder holder, Inventory inventory, List<UUID> incomingTeleports) {
        for (int index = 0; index < Math.min(incomingTeleports.size(), TELEPORT_REQUEST_SLOTS.length); index++) {
            UUID senderId = incomingTeleports.get(index);
            FriendProfile sender = plugin.friends().profile(senderId).orElse(null);
            if (sender == null) {
                continue;
            }
            int slot = TELEPORT_REQUEST_SLOTS[index];
            inventory.setItem(slot, item(Material.ENDER_EYE, "§b传送请求: " + sender.latestName(), List.of(
                "§7左键同意",
                "§7右键拒绝"
            )));
            holder.actions.put(slot, new GuiAction(ActionType.TELEPORT_REQUEST, senderId, null));
        }
    }

    private void addFriendRequests(MenuHolder holder, Inventory inventory, List<FriendProfile> incomingFriends) {
        for (int index = 0; index < Math.min(incomingFriends.size(), FRIEND_REQUEST_SLOTS.length); index++) {
            FriendProfile request = incomingFriends.get(index);
            int slot = FRIEND_REQUEST_SLOTS[index];
            inventory.setItem(slot, item(Material.PAPER, "§e好友申请: " + request.latestName(), List.of(
                "§7左键同意",
                "§7右键拒绝"
            )));
            holder.actions.put(slot, new GuiAction(ActionType.FRIEND_REQUEST, request.id(), null));
        }
        if (incomingFriends.isEmpty()) {
            inventory.setItem(13, item(Material.GRAY_DYE, "§7暂无好友申请", List.of("§7别人发送申请后会显示在这里。")));
        }
    }

    private void addFriendList(MenuHolder holder, Inventory inventory, List<FriendService.FriendSummary> friends) {
        for (int index = 0; index < Math.min(friends.size(), FRIEND_SLOTS.length); index++) {
            FriendService.FriendSummary friend = friends.get(index);
            int slot = FRIEND_SLOTS[index];
            Material material = friend.online() ? Material.LIME_DYE : Material.GRAY_DYE;
            inventory.setItem(slot, item(material, (friend.online() ? "§a" : "§7") + friend.name(), List.of(
                friend.online() ? "§a在线" : "§7离线或隐藏在线状态",
                "§7点击查看好友资料和操作"
            )));
            holder.actions.put(slot, new GuiAction(ActionType.OPEN_DETAILS, friend.id(), null));
        }
        if (friends.isEmpty()) {
            inventory.setItem(31, item(Material.BARRIER, "§7暂无好友", List.of("§7让对方在线后使用好友申请按钮添加。")));
        }
    }

    private void addSettings(MenuHolder holder, Inventory inventory, Player player) {
        FriendProfile profile = plugin.friends().profile(player.getUniqueId()).orElseThrow();
        addToggle(holder, inventory, 45, FriendService.SettingKey.REQUESTS, profile.settings().receiveRequests(), "好友申请");
        addToggle(holder, inventory, 46, FriendService.SettingKey.TELEPORTS, profile.settings().receiveTeleports(), "好友传送");
        addToggle(holder, inventory, 47, FriendService.SettingKey.MESSAGES, profile.settings().receiveMessages(), "好友私聊");
        addToggle(holder, inventory, 48, FriendService.SettingKey.NOTIFICATIONS, profile.settings().onlineNotifications(), "上线提醒");
        addToggle(holder, inventory, 50, FriendService.SettingKey.STATUS, profile.settings().showOnlineStatus(), "在线状态");
    }

    private void handleAction(Player player, GuiAction action, ClickType click) {
        switch (action.type()) {
            case FRIEND_REQUEST -> handleFriendRequest(player, action.targetId(), click);
            case TELEPORT_REQUEST -> handleTeleportRequest(player, action.targetId(), click);
            case OPEN_DETAILS -> openDetails(player, action.targetId());
            case REQUEST_TELEPORT -> handleRequestTeleport(player, action.targetId());
            case MESSAGE_HINT -> handleMessageHint(player, action.targetId());
            case TOGGLE_TRUST -> handleToggleTrust(player, action.targetId());
            case REMOVE_FRIEND -> handleRemoveFriend(player, action.targetId());
            case BLOCK_FRIEND -> handleBlockFriend(player, action.targetId());
            case TOGGLE -> run(player, "friend toggle " + action.command());
            case RUN_COMMAND -> run(player, action.command());
            case BACK -> open(player);
        }
    }

    private void handleFriendRequest(Player player, UUID senderId, ClickType click) {
        FriendProfile sender = plugin.friends().profile(senderId).orElse(null);
        if (sender == null) {
            player.sendMessage(plugin.prefix() + "找不到这条好友申请。");
            open(player);
            return;
        }
        FriendService.Result result = click == ClickType.RIGHT
            ? plugin.friends().denyRequest(player.getUniqueId(), senderId)
            : plugin.friends().acceptRequest(player.getUniqueId(), senderId);
        player.sendMessage(plugin.prefix() + result.message());
        if (result.ok()) {
            plugin.saveFriendData();
            Player onlineSender = Bukkit.getPlayer(senderId);
            if (onlineSender != null && click != ClickType.RIGHT) {
                onlineSender.sendMessage(plugin.prefix() + "§a" + player.getName() + " §f已同意你的好友申请。");
            }
        }
        open(player);
    }

    private void handleTeleportRequest(Player player, UUID senderId, ClickType click) {
        LeafFriendsPlugin.PlayerTarget sender = targetFromProfile(senderId);
        if (sender == null) {
            player.sendMessage(plugin.prefix() + "找不到这条好友传送请求。");
            open(player);
            return;
        }
        if (click == ClickType.RIGHT) {
            player.sendMessage(plugin.prefix() + plugin.teleports().deny(player.getUniqueId(), senderId).message());
        } else {
            plugin.acceptTeleport(player, sender);
        }
        open(player);
    }

    private void handleRequestTeleport(Player player, UUID friendId) {
        LeafFriendsPlugin.PlayerTarget target = targetFromProfile(friendId);
        if (target == null) {
            player.sendMessage(plugin.prefix() + "找不到这个好友。");
            open(player);
            return;
        }
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.requestTeleport(player, target));
    }

    private void handleMessageHint(Player player, UUID friendId) {
        FriendProfile friend = plugin.friends().profile(friendId).orElse(null);
        player.closeInventory();
        if (friend == null) {
            player.sendMessage(plugin.prefix() + "找不到这个好友。");
            return;
        }
        plugin.sendMessageSuggestion(player, friend.latestName());
    }

    private void handleToggleTrust(Player player, UUID friendId) {
        boolean next = !plugin.friends().isTrustedTeleporter(player.getUniqueId(), friendId);
        FriendService.Result result = plugin.friends().setTrustedTeleporter(player.getUniqueId(), friendId, next);
        player.sendMessage(plugin.prefix() + result.message());
        if (result.ok()) {
            plugin.saveFriendData();
        }
        openDetails(player, friendId);
    }

    private void handleRemoveFriend(Player player, UUID friendId) {
        FriendService.Result result = plugin.friends().removeFriend(player.getUniqueId(), friendId);
        player.sendMessage(plugin.prefix() + result.message());
        if (result.ok()) {
            plugin.saveFriendData();
        }
        open(player);
    }

    private void handleBlockFriend(Player player, UUID friendId) {
        FriendService.Result result = plugin.friends().block(player.getUniqueId(), friendId);
        player.sendMessage(plugin.prefix() + result.message());
        if (result.ok()) {
            plugin.saveFriendData();
        }
        open(player);
    }

    private LeafFriendsPlugin.PlayerTarget targetFromProfile(UUID targetId) {
        FriendProfile profile = plugin.friends().profile(targetId).orElse(null);
        if (profile == null) {
            return null;
        }
        Player online = Bukkit.getPlayer(targetId);
        String name = online == null ? profile.latestName() : online.getName();
        return new LeafFriendsPlugin.PlayerTarget(targetId, name, online);
    }

    private void addToggle(MenuHolder holder, Inventory inventory, int slot, FriendService.SettingKey key, boolean enabled, String label) {
        inventory.setItem(slot, item(enabled ? Material.LIME_DYE : Material.RED_DYE, (enabled ? "§a" : "§c") + label, List.of(
            "§7当前: " + (enabled ? "§a开启" : "§c关闭"),
            "§7点击切换"
        )));
        holder.actions.put(slot, new GuiAction(ActionType.TOGGLE, null, key.key()));
    }

    private void run(Player player, String command) {
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(command));
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private enum ActionType {
        FRIEND_REQUEST,
        TELEPORT_REQUEST,
        OPEN_DETAILS,
        REQUEST_TELEPORT,
        MESSAGE_HINT,
        TOGGLE_TRUST,
        REMOVE_FRIEND,
        BLOCK_FRIEND,
        TOGGLE,
        RUN_COMMAND,
        BACK
    }

    private record GuiAction(ActionType type, UUID targetId, String command) {
    }

    private static final class MenuHolder implements InventoryHolder {
        private final UUID owner;
        private final Map<Integer, GuiAction> actions = new HashMap<>();
        private Inventory inventory;

        private MenuHolder(UUID owner) {
            this.owner = owner;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
