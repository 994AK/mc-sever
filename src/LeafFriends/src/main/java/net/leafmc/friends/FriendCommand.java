package net.leafmc.friends;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class FriendCommand implements TabExecutor {
    private static final List<CommandSpec> COMMANDS = List.of(
        new CommandSpec("add", "/friend add <玩家> §7添加好友", true, true),
        new CommandSpec("accept", "/friend accept <玩家> §7同意好友申请", true, true),
        new CommandSpec("deny", "/friend deny <玩家> §7拒绝好友申请", true, true),
        new CommandSpec("cancel", "/friend cancel <玩家> §7取消已发送申请", true, true),
        new CommandSpec("remove", "/friend remove <玩家> §7删除好友", true, true),
        new CommandSpec("list", "/friend list §7好友列表", false, true),
        new CommandSpec("msg", "/friend msg <玩家> <内容> §7好友私聊", true, true),
        new CommandSpec("tp", "/friend tp <玩家> §7好友传送请求", true, true),
        new CommandSpec("tpaccept", "/friend tpaccept <玩家> §7同意好友传送", true, true),
        new CommandSpec("tpdeny", "/friend tpdeny <玩家> §7拒绝好友传送", true, true),
        new CommandSpec("toggle", "/friend toggle <requests|teleports|messages|notifications|status> [on|off]", false, true),
        new CommandSpec("block", "/friend block <玩家> §7拉黑玩家", true, true),
        new CommandSpec("unblock", "/friend unblock <玩家> §7解除拉黑", true, true),
        new CommandSpec("gui", "/friend gui §7打开好友菜单", false, true),
        new CommandSpec("reload", "/friend reload §7重载好友系统", false, false)
    );
    private static final List<String> SUBCOMMANDS = COMMANDS.stream().map(CommandSpec::name).toList();
    private static final List<String> TARGET_SUBCOMMANDS = COMMANDS.stream()
        .filter(CommandSpec::targetCompletion)
        .map(CommandSpec::name)
        .toList();

    private final LeafFriendsPlugin plugin;

    public FriendCommand(LeafFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("menufriends")) {
            Player player = requirePlayer(sender);
            if (player != null && requireUse(player)) {
                plugin.gui().open(player);
            }
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player && player.hasPermission("leaffriends.use")) {
                plugin.gui().open(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add" -> handleAdd(sender, args);
            case "accept" -> handleAccept(sender, args);
            case "deny" -> handleDeny(sender, args);
            case "cancel" -> handleCancel(sender, args);
            case "remove", "delete" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "msg", "tell" -> handleMessage(sender, args);
            case "tp", "tpa" -> handleTeleportRequest(sender, args);
            case "tpaccept", "tpyes" -> handleTeleportAccept(sender, args);
            case "tpdeny", "tpno" -> handleTeleportDeny(sender, args);
            case "toggle" -> handleToggle(sender, args);
            case "block" -> handleBlock(sender, args);
            case "unblock" -> handleUnblock(sender, args);
            case "gui", "menu" -> {
                Player player = requirePlayer(sender);
                if (player != null && requireUse(player)) {
                    plugin.gui().open(player);
                }
            }
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefixMatches(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && TARGET_SUBCOMMANDS.contains(sub)) {
            List<String> names = new ArrayList<>(plugin.friends().knownNames());
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return prefixMatches(names.stream().distinct().toList(), args[1]);
        }
        if (args.length == 2 && sub.equals("toggle")) {
            return prefixMatches(settingKeys(), args[1]);
        }
        if (args.length == 3 && sub.equals("toggle")) {
            return prefixMatches(List.of("on", "off"), args[2]);
        }
        return List.of();
    }

    public static List<String> settingKeys() {
        return Arrays.stream(FriendService.SettingKey.values()).map(FriendService.SettingKey::key).toList();
    }

    public static Optional<Boolean> parseToggleValue(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "on", "true", "yes", "1", "开", "开启" -> Optional.of(true);
            case "off", "false", "no", "0", "关", "关闭" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    private void handleAdd(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend add <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        FriendService.Result result = plugin.friends().sendRequest(player.getUniqueId(), player.getName(), target.id(), target.name());
        player.sendMessage(plugin.prefix() + result.message());
        if (result.ok()) {
            plugin.saveFriendData();
            if (target.onlinePlayer() != null) {
                target.onlinePlayer().sendMessage(plugin.prefix() + "§a" + player.getName() + " §f想添加你为好友，输入 §e/friend accept " + player.getName() + " §f同意。");
            }
        }
    }

    private void handleAccept(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend accept <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        FriendService.Result result = plugin.friends().acceptRequest(player.getUniqueId(), target.id());
        player.sendMessage(plugin.prefix() + result.message());
        if (result.ok()) {
            plugin.saveFriendData();
            if (target.onlinePlayer() != null) {
                target.onlinePlayer().sendMessage(plugin.prefix() + "§a" + player.getName() + " §f已同意你的好友申请。");
            }
        }
    }

    private void handleDeny(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend deny <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        sendAndSaveIfOk(player, plugin.friends().denyRequest(player.getUniqueId(), target.id()));
    }

    private void handleCancel(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend cancel <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        sendAndSaveIfOk(player, plugin.friends().cancelRequest(player.getUniqueId(), target.id()));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend remove <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        sendAndSaveIfOk(player, plugin.friends().removeFriend(player.getUniqueId(), target.id()));
    }

    private void handleList(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requireUse(player)) {
            return;
        }
        plugin.friends().ensureProfile(player.getUniqueId(), player.getName());
        List<FriendService.FriendSummary> friends = plugin.friends().listFriends(player.getUniqueId());
        if (friends.isEmpty()) {
            player.sendMessage(plugin.prefix() + "暂无好友。输入 §e/friend add 玩家名 §f添加。");
            return;
        }
        player.sendMessage(plugin.prefix() + "好友列表：");
        for (FriendService.FriendSummary friend : friends) {
            String state = friend.online() ? "§a在线" : "§7离线";
            String lastSeen = friend.online() || friend.lastSeenMillis() <= 0L ? "" : " §8上次在线 " + formatAgo(friend.lastSeenMillis());
            player.sendMessage("§7- §f" + friend.name() + " " + state + lastSeen);
        }
    }

    private void handleMessage(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 3, "/friend msg <玩家> <内容>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        boolean hidesStatus = plugin.friends().hidesOnlineStatus(target.id());
        if (hidesStatus) {
            if (target.onlinePlayer() != null && plugin.friends().canMessage(player.getUniqueId(), target.id()).ok()) {
                String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                target.onlinePlayer().sendMessage("§b[好友私聊] §a" + player.getName() + " §7-> §f你: §f" + message);
            }
            player.sendMessage(plugin.prefix() + "如果对方在线且允许，会收到你的好友私聊。");
            return;
        }
        if (target.onlinePlayer() == null) {
            player.sendMessage(plugin.prefix() + "好友不在线，离线留言请使用 CMI 的 /mail。");
            return;
        }
        FriendService.Result result = plugin.friends().canMessage(player.getUniqueId(), target.id());
        if (!result.ok()) {
            player.sendMessage(plugin.prefix() + result.message());
            return;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        target.onlinePlayer().sendMessage("§b[好友私聊] §a" + player.getName() + " §7-> §f你: §f" + message);
        player.sendMessage("§b[好友私聊] §f你 §7-> §a" + target.name() + "§f: " + message);
    }

    private void handleTeleportRequest(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend tp <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        boolean hidesStatus = plugin.friends().hidesOnlineStatus(target.id());
        if (hidesStatus) {
            if (target.onlinePlayer() != null) {
                if (player.isDead() || target.onlinePlayer().isDead()) {
                    player.sendMessage(plugin.prefix() + "如果对方在线且允许，会收到你的好友传送请求。");
                    return;
                }
                TeleportRequestService.Result result = plugin.teleports().request(
                    player.getUniqueId(),
                    target.id(),
                    player.isOnline(),
                    target.onlinePlayer().isOnline(),
                    player.getWorld().getName(),
                    target.onlinePlayer().getWorld().getName()
                );
                if (result.ok()) {
                    target.onlinePlayer().sendMessage(plugin.prefix() + "§a" + player.getName() + " §f请求传送到你身边，输入 §e/friend tpaccept " + player.getName() + " §f同意。");
                }
            }
            player.sendMessage(plugin.prefix() + "如果对方在线且允许，会收到你的好友传送请求。");
            return;
        }
        if (target.onlinePlayer() == null) {
            player.sendMessage(plugin.prefix() + "好友不在线，不能发送好友传送。");
            return;
        }
        if (player.isDead() || target.onlinePlayer().isDead()) {
            player.sendMessage(plugin.prefix() + "双方都存活时才能发送好友传送。");
            return;
        }
        TeleportRequestService.Result result = plugin.teleports().request(
            player.getUniqueId(),
            target.id(),
            player.isOnline(),
            target.onlinePlayer().isOnline(),
            player.getWorld().getName(),
            target.onlinePlayer().getWorld().getName()
        );
        player.sendMessage(plugin.prefix() + result.message());
        if (result.ok()) {
            target.onlinePlayer().sendMessage(plugin.prefix() + "§a" + player.getName() + " §f请求传送到你身边，输入 §e/friend tpaccept " + player.getName() + " §f同意。");
        }
    }

    private void handleTeleportAccept(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend tpaccept <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        if (target.onlinePlayer() == null) {
            player.sendMessage(plugin.prefix() + "对方不在线。");
            return;
        }
        if (player.isDead() || target.onlinePlayer().isDead()) {
            player.sendMessage(plugin.prefix() + "双方都存活时才能接受好友传送。");
            return;
        }
        if (!plugin.teleportWorldAllowed(player.getWorld().getName())) {
            player.sendMessage(plugin.prefix() + "当前世界不允许好友传送。");
            return;
        }
        if (!plugin.teleportWorldAllowed(target.onlinePlayer().getWorld().getName())) {
            player.sendMessage(plugin.prefix() + "对方当前世界不允许好友传送。");
            return;
        }
        TeleportRequestService.Result result = plugin.teleports().accept(
            player.getUniqueId(),
            target.id(),
            target.onlinePlayer().getWorld().getName(),
            player.getWorld().getName()
        );
        if (!result.ok()) {
            player.sendMessage(plugin.prefix() + result.message());
            return;
        }
        boolean teleported = target.onlinePlayer().teleport(player.getLocation());
        if (teleported) {
            target.onlinePlayer().sendMessage(plugin.prefix() + "已传送到好友 §a" + player.getName() + " §f身边。");
            player.sendMessage(plugin.prefix() + "已同意 §a" + target.name() + " §f的好友传送。");
        } else {
            player.sendMessage(plugin.prefix() + "传送失败，请稍后再试。");
        }
    }

    private void handleTeleportDeny(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend tpdeny <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        player.sendMessage(plugin.prefix() + plugin.teleports().deny(player.getUniqueId(), target.id()).message());
    }

    private void handleToggle(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend toggle <requests|teleports|messages|notifications|status> [on|off]");
        if (player == null) {
            return;
        }
        Optional<FriendService.SettingKey> key = FriendService.SettingKey.parse(args[1]);
        if (key.isEmpty()) {
            player.sendMessage(plugin.prefix() + "未知设置，可用: " + String.join(", ", settingKeys()));
            return;
        }
        FriendProfile profile = plugin.friends().ensureProfile(player.getUniqueId(), player.getName());
        boolean value;
        if (args.length >= 3) {
            Optional<Boolean> parsed = parseToggleValue(args[2]);
            if (parsed.isEmpty()) {
                player.sendMessage(plugin.prefix() + "开关值只能是 on/off。");
                return;
            }
            value = parsed.get();
        } else {
            value = !currentSetting(profile, key.get());
        }
        sendAndSaveIfOk(player, plugin.friends().setSetting(player.getUniqueId(), key.get(), value));
    }

    private void handleBlock(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend block <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        sendAndSaveIfOk(player, plugin.friends().block(player.getUniqueId(), target.id()));
    }

    private void handleUnblock(CommandSender sender, String[] args) {
        Player player = playerWithUsage(sender, args, 2, "/friend unblock <玩家>");
        if (player == null) {
            return;
        }
        LeafFriendsPlugin.PlayerTarget target = targetOrMessage(player, args[1]);
        if (target == null) {
            return;
        }
        sendAndSaveIfOk(player, plugin.friends().unblock(player.getUniqueId(), target.id()));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("leaffriends.reload")) {
            sender.sendMessage("§c你没有好友系统重载权限。");
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(plugin.prefix() + "好友系统已重载。");
    }

    private Player playerWithUsage(CommandSender sender, String[] args, int minArgs, String usage) {
        Player player = requirePlayer(sender);
        if (player == null || !requireUse(player)) {
            return null;
        }
        plugin.friends().ensureProfile(player.getUniqueId(), player.getName());
        if (args.length < minArgs) {
            player.sendMessage(plugin.prefix() + "用法: §e" + usage);
            return null;
        }
        return player;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage("§c这个命令只能由玩家执行。");
        return null;
    }

    private boolean requireUse(Player player) {
        if (player.hasPermission("leaffriends.use")) {
            return true;
        }
        player.sendMessage("§c你没有好友系统权限。");
        return false;
    }

    private LeafFriendsPlugin.PlayerTarget targetOrMessage(Player player, String rawName) {
        Optional<LeafFriendsPlugin.PlayerTarget> target = plugin.resolveTarget(rawName);
        if (target.isPresent()) {
            return target.get();
        }
        player.sendMessage(plugin.prefix() + "找不到玩家资料。对方需要在线，或曾经使用过好友系统。");
        return null;
    }

    private void sendAndSaveIfOk(Player player, FriendService.Result result) {
        player.sendMessage(plugin.prefix() + result.message());
        if (result.ok()) {
            plugin.saveFriendData();
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.prefix() + "好友命令：");
        for (CommandSpec spec : COMMANDS) {
            if (spec.playerHelp() || sender.hasPermission("leaffriends.reload")) {
                sender.sendMessage("§e" + spec.usage());
            }
        }
    }

    private boolean currentSetting(FriendProfile profile, FriendService.SettingKey key) {
        return switch (key) {
            case REQUESTS -> profile.settings().receiveRequests();
            case TELEPORTS -> profile.settings().receiveTeleports();
            case MESSAGES -> profile.settings().receiveMessages();
            case NOTIFICATIONS -> profile.settings().onlineNotifications();
            case STATUS -> profile.settings().showOnlineStatus();
        };
    }

    private String formatAgo(long thenMillis) {
        long millis = Math.max(0L, System.currentTimeMillis() - thenMillis);
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        if (days > 0) {
            return days + "天前";
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + "小时前";
        }
        long minutes = duration.toMinutes();
        return Math.max(1L, minutes) + "分钟前";
    }

    private static List<String> prefixMatches(List<String> values, String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return values.stream()
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private record CommandSpec(String name, String usage, boolean targetCompletion, boolean playerHelp) {
    }
}
