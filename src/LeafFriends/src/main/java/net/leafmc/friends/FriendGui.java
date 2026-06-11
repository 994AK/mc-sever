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
    private static final int[] REQUEST_SLOTS = {10, 11, 12, 13, 14, 15, 16};
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

        inventory.setItem(4, item(Material.COMPASS, "§b好友菜单", List.of(
            "§7管理好友、申请、好友传送和隐私设置。",
            "§7好友系统不提供战力或资源加成。"
        )));

        List<FriendProfile> incoming = plugin.friends().incomingRequests(player.getUniqueId());
        for (int index = 0; index < Math.min(incoming.size(), REQUEST_SLOTS.length); index++) {
            FriendProfile request = incoming.get(index);
            int slot = REQUEST_SLOTS[index];
            inventory.setItem(slot, item(Material.PAPER, "§e申请: " + request.latestName(), List.of(
                "§7左键同意",
                "§7右键拒绝"
            )));
            holder.actions.put(slot, new GuiAction(ActionType.REQUEST, request.latestName(), null));
        }
        if (incoming.isEmpty()) {
            inventory.setItem(13, item(Material.GRAY_DYE, "§7暂无好友申请", List.of("§7别人发送申请后会显示在这里。")));
        }

        List<FriendService.FriendSummary> friends = plugin.friends().listFriends(player.getUniqueId());
        for (int index = 0; index < Math.min(friends.size(), FRIEND_SLOTS.length); index++) {
            FriendService.FriendSummary friend = friends.get(index);
            int slot = FRIEND_SLOTS[index];
            Material material = friend.online() ? Material.LIME_DYE : Material.GRAY_DYE;
            inventory.setItem(slot, item(material, (friend.online() ? "§a" : "§7") + friend.name(), List.of(
                friend.online() ? "§a在线" : "§7离线",
                "§7左键发送好友传送请求",
                "§7右键提示好友私聊命令"
            )));
            holder.actions.put(slot, new GuiAction(ActionType.FRIEND, friend.name(), null));
        }
        if (friends.isEmpty()) {
            inventory.setItem(31, item(Material.BARRIER, "§7暂无好友", List.of("§7输入 /friend add 玩家名 添加。")));
        }

        FriendProfile profile = plugin.friends().profile(player.getUniqueId()).orElseThrow();
        addToggle(holder, inventory, 45, FriendService.SettingKey.REQUESTS, profile.settings().receiveRequests(), "好友申请");
        addToggle(holder, inventory, 46, FriendService.SettingKey.TELEPORTS, profile.settings().receiveTeleports(), "好友传送");
        addToggle(holder, inventory, 47, FriendService.SettingKey.MESSAGES, profile.settings().receiveMessages(), "好友私聊");
        addToggle(holder, inventory, 48, FriendService.SettingKey.NOTIFICATIONS, profile.settings().onlineNotifications(), "上线提醒");
        addToggle(holder, inventory, 49, FriendService.SettingKey.STATUS, profile.settings().showOnlineStatus(), "在线状态");
        inventory.setItem(53, item(Material.NETHER_STAR, "§a回到社交菜单", List.of("§7打开 /menusocial")));
        holder.actions.put(53, new GuiAction(ActionType.RUN_COMMAND, null, "menusocial"));

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
        switch (action.type()) {
            case REQUEST -> {
                if (event.getClick() == ClickType.RIGHT) {
                    run(player, "friend deny " + action.name());
                } else {
                    run(player, "friend accept " + action.name());
                }
            }
            case FRIEND -> {
                if (event.getClick() == ClickType.RIGHT) {
                    player.closeInventory();
                    player.sendMessage(plugin.prefix() + "好友私聊：§e/friend msg " + action.name() + " 内容");
                } else {
                    run(player, "friend tp " + action.name());
                }
            }
            case TOGGLE -> run(player, "friend toggle " + action.command());
            case RUN_COMMAND -> run(player, action.command());
        }
    }

    private void addToggle(MenuHolder holder, Inventory inventory, int slot, FriendService.SettingKey key, boolean enabled, String label) {
        inventory.setItem(slot, item(enabled ? Material.LIME_DYE : Material.RED_DYE, (enabled ? "§a" : "§c") + label, List.of(
            "§7当前: " + (enabled ? "§a开启" : "§c关闭"),
            "§7点击切换"
        )));
        holder.actions.put(slot, new GuiAction(ActionType.TOGGLE, label, key.key()));
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
        REQUEST,
        FRIEND,
        TOGGLE,
        RUN_COMMAND
    }

    private record GuiAction(ActionType type, String name, String command) {
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
