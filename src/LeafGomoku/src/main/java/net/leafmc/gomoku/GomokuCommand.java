package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class GomokuCommand implements TabExecutor {
    private final LeafGomokuPlugin plugin;

    public GomokuCommand(LeafGomokuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (List.of("menugomoku", "gomokumenu", "wzq", "wuziqi").contains(label.toLowerCase(Locale.ROOT))) {
            Player player = requirePlayer(sender);
            if (player == null || !require(sender, GomokuPermission.GUI, "§c你没有五子棋菜单权限。")) {
                return true;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.gui().openLobby(player));
            return true;
        }
        if (args.length == 0 && sender instanceof Player player && sender.hasPermission(GomokuPermission.GUI.node())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.gui().openLobby(player));
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join" -> {
                Player player = requirePlayer(sender);
                if (player == null || !require(sender, GomokuPermission.PLAY, "§c你没有五子棋权限。")) {
                    return true;
                }
                send(player, plugin.join(player, arg(args, 1)));
                return true;
            }
            case "spectate", "watch" -> {
                Player player = requirePlayer(sender);
                if (player == null || !require(sender, GomokuPermission.SPECTATE, "§c你没有五子棋观战权限。")) {
                    return true;
                }
                send(player, plugin.spectate(player, arg(args, 1)));
                return true;
            }
            case "leave" -> {
                Player player = requirePlayer(sender);
                if (player != null) {
                    send(player, plugin.leave(player));
                }
                return true;
            }
            case "invite", "邀战", "邀请" -> {
                Player player = requirePlayer(sender);
                if (player == null || !require(sender, GomokuPermission.PLAY, "§c你没有五子棋权限。")) {
                    return true;
                }
                String action = arg(args, 1).toLowerCase(Locale.ROOT);
                if (List.of("accept", "yes", "agree", "同意").contains(action)) {
                    send(player, plugin.acceptInvite(player, arg(args, 2)));
                } else if (List.of("deny", "reject", "no", "拒绝").contains(action)) {
                    send(player, plugin.denyInvite(player, arg(args, 2)));
                } else if (args.length < 2) {
                    sender.sendMessage("§e用法: /gomoku invite <玩家> [room] §7或 §f/gomoku invite accept");
                } else {
                    send(player, plugin.invite(player, args[1], arg(args, 2)));
                }
                return true;
            }
            case "undo", "huiqi", "悔棋" -> {
                Player player = requirePlayer(sender);
                if (player == null || !require(sender, GomokuPermission.PLAY, "§c你没有五子棋权限。")) {
                    return true;
                }
                String action = arg(args, 1).toLowerCase(Locale.ROOT);
                if (List.of("accept", "yes", "agree", "同意").contains(action)) {
                    send(player, plugin.acceptUndo(player, arg(args, 2)));
                } else if (List.of("deny", "reject", "no", "拒绝").contains(action)) {
                    send(player, plugin.denyUndo(player, arg(args, 2)));
                } else {
                    send(player, plugin.requestUndo(player, arg(args, 1)));
                }
                return true;
            }
            case "gui", "menu", "lobby" -> {
                Player player = requirePlayer(sender);
                if (player == null || !require(sender, GomokuPermission.GUI, "§c你没有五子棋菜单权限。")) {
                    return true;
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.gui().openLobby(player));
                return true;
            }
            case "status" -> {
                sender.sendMessage(plugin.statusLine(arg(args, 1)));
                return true;
            }
            case "leaderboard", "top" -> {
                if (!require(sender, GomokuPermission.LEADERBOARD, "§c你没有五子棋排行榜权限。")) {
                    return true;
                }
                sender.sendMessage(plugin.leaderboardLine(arg(args, 1)));
                return true;
            }
            case "stats" -> {
                if (!require(sender, GomokuPermission.STATS, "§c你没有五子棋统计权限。")) {
                    return true;
                }
                OfflinePlayer target = args.length >= 2 ? plugin.offlinePlayer(args[1]) : sender instanceof Player player ? player : null;
                if (target == null) {
                    sender.sendMessage("§e用法: /gomoku stats <玩家>");
                } else {
                    sender.sendMessage(plugin.statsLine(target));
                }
                return true;
            }
            case "theme" -> {
                Player player = requirePlayer(sender);
                if (player == null || !require(sender, GomokuPermission.APPEARANCE, "§c你没有五子棋外观权限。")) {
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§e用法: /gomoku theme <room> <theme>");
                } else {
                    sender.sendMessage(plugin.selectBoardTheme(player, args[1], args[2]));
                }
                return true;
            }
            case "skin" -> {
                Player player = requirePlayer(sender);
                if (player == null || !require(sender, GomokuPermission.APPEARANCE, "§c你没有五子棋外观权限。")) {
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§e用法: /gomoku skin <skin>");
                } else {
                    sender.sendMessage(plugin.selectPieceSkin(player, args[1]));
                }
                return true;
            }
            case "buy", "exchange" -> {
                Player player = requirePlayer(sender);
                if (player == null || !require(sender, GomokuPermission.APPEARANCE, "§c你没有五子棋外观权限。")) {
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§e用法: /gomoku buy <theme|skin> <id> [room]");
                } else {
                    sender.sendMessage(plugin.buyAppearance(player, args[1], args[2], arg(args, 3)));
                }
                return true;
            }
            case "var" -> {
                if (!require(sender, GomokuPermission.ADMIN_ROOM, "§c你没有五子棋变量查询权限。")) {
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§e用法: /gomoku var <key> [player]");
                    return true;
                }
                OfflinePlayer target = args.length >= 3 ? plugin.offlinePlayer(args[2]) : sender instanceof Player player ? player : null;
                sender.sendMessage("§6[五子棋变量] §f" + args[1] + " §7= §e" + plugin.variables().resolve(args[1], target));
                return true;
            }
            case "admin" -> {
                handleAdmin(sender, args);
                return true;
            }
            case "init" -> {
                if (require(sender, GomokuPermission.ADMIN_SETUP, "§c你没有权限初始化五子棋。")) {
                    sender.sendMessage(plugin.initRoom(arg(args, 1)));
                }
                return true;
            }
            case "reset" -> {
                if (require(sender, GomokuPermission.ADMIN_ROOM, "§c你没有权限重置五子棋。")) {
                    Boolean force = forceFlag(sender, args);
                    if (force == null) {
                        return true;
                    }
                    sender.sendMessage(plugin.resetRoom(arg(args, 1), force));
                }
                return true;
            }
            case "reload" -> {
                if (require(sender, GomokuPermission.ADMIN_RELOAD, "§c你没有权限重载五子棋。")) {
                    plugin.reloadAll();
                    sender.sendMessage("§a五子棋配置、房间和统计已重载。");
                }
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        String action = args.length < 2 ? "help" : args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "create" -> {
                if (!require(sender, GomokuPermission.ADMIN_SETUP, "§c你没有权限创建五子棋房间。")) {
                    return;
                }
                Player player = requirePlayer(sender);
                if (player == null) {
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§e用法: /gomoku admin create <room> [size <" + GomokuBoard.MIN_SIZE + "-" + GomokuBoard.MAX_SIZE + ">]");
                    return;
                }
                RoomCreateOptions options = roomCreateOptions(sender, args, 3);
                if (options == null) {
                    return;
                }
                sender.sendMessage(plugin.createRoomAt(player, args[2], player.getLocation(), options.boardSize(), options.templateId()));
            }
            case "place", "tool" -> {
                if (!require(sender, GomokuPermission.ADMIN_SETUP, "§c你没有权限放置五子棋房间。")) {
                    return;
                }
                Player player = requirePlayer(sender);
                if (player == null) {
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§e用法: /gomoku admin place <room> [size <" + GomokuBoard.MIN_SIZE + "-" + GomokuBoard.MAX_SIZE + ">]");
                    return;
                }
                RoomCreateOptions options = roomCreateOptions(sender, args, 3);
                if (options == null) {
                    return;
                }
                sender.sendMessage(plugin.prepareRoomPlacement(player, args[2], options.boardSize(), options.templateId()));
            }
            case "confirm" -> {
                if (!require(sender, GomokuPermission.ADMIN_SETUP, "§c你没有权限确认创建五子棋房间。")) {
                    return;
                }
                Player player = requirePlayer(sender);
                if (player != null) {
                    sender.sendMessage(plugin.confirmRoomPlacement(player));
                }
            }
            case "cancel" -> {
                if (!require(sender, GomokuPermission.ADMIN_SETUP, "§c你没有权限取消创建五子棋房间。")) {
                    return;
                }
                Player player = requirePlayer(sender);
                if (player != null) {
                    sender.sendMessage(plugin.cancelRoomPlacement(player));
                }
            }
            case "environment", "env" -> {
                if (require(sender, GomokuPermission.ADMIN_ROOM, "§c你没有权限切换房间环境模板。")) {
                    if (args.length < 4) {
                        sender.sendMessage("§e用法: /gomoku admin environment <room> <template> [force]");
                        return;
                    }
                    Boolean force = forceFlag(sender, args);
                    if (force == null) {
                        return;
                    }
                    sender.sendMessage(plugin.setRoomEnvironment(args[2], args[3], force));
                }
            }
            case "init" -> {
                if (require(sender, GomokuPermission.ADMIN_SETUP, "§c你没有权限初始化房间。")) {
                    sender.sendMessage(plugin.initRoom(arg(args, 2)));
                }
            }
            case "open", "close" -> {
                if (require(sender, GomokuPermission.ADMIN_ROOM, "§c你没有权限开关房间。")) {
                    sender.sendMessage(plugin.setOpen(arg(args, 2), action.equals("open")));
                }
            }
            case "refresh" -> {
                if (require(sender, GomokuPermission.ADMIN_ROOM, "§c你没有权限刷新房间。")) {
                    sender.sendMessage(plugin.refreshRoom(arg(args, 2)));
                }
            }
            case "reset" -> {
                if (require(sender, GomokuPermission.ADMIN_ROOM, "§c你没有权限重置房间。")) {
                    Boolean force = forceFlag(sender, args);
                    if (force == null) {
                        return;
                    }
                    sender.sendMessage(plugin.resetRoom(arg(args, 2), force));
                }
            }
            case "delete" -> {
                if (require(sender, GomokuPermission.ADMIN_ROOM, "§c你没有权限删除房间。")) {
                    Boolean force = forceFlag(sender, args);
                    if (force == null) {
                        return;
                    }
                    sender.sendMessage(plugin.deleteRoom(arg(args, 2), force));
                }
            }
            case "inspect" -> {
                if (require(sender, GomokuPermission.ADMIN_ROOM, "§c你没有权限查看房间。")) {
                    for (String line : plugin.inspectRoom(arg(args, 2))) {
                        sender.sendMessage(line);
                    }
                }
            }
            case "stop" -> {
                if (require(sender, GomokuPermission.ADMIN_LIFECYCLE, "§c你没有权限中止房间。")) {
                    sender.sendMessage(plugin.stopRoom(arg(args, 2)));
                }
            }
            case "release" -> {
                if (require(sender, GomokuPermission.ADMIN_LIFECYCLE, "§c你没有权限释放席位。")) {
                    Stone side = sideArg(arg(args, 3));
                    if (side == Stone.EMPTY) {
                        sender.sendMessage("§e用法: /gomoku admin release <room> <black|white> [force]");
                    } else {
                        Boolean force = forceFlag(sender, args);
                        if (force == null) {
                            return;
                        }
                        sender.sendMessage(plugin.releaseSeat(arg(args, 2), side, force, false));
                    }
                }
            }
            case "forfeit" -> {
                if (require(sender, GomokuPermission.ADMIN_LIFECYCLE, "§c你没有权限判负。")) {
                    Stone side = sideArg(arg(args, 3));
                    if (side == Stone.EMPTY) {
                        sender.sendMessage("§e用法: /gomoku admin forfeit <room> <black|white>");
                    } else {
                        sender.sendMessage(plugin.releaseSeat(arg(args, 2), side, true, true));
                    }
                }
            }
            case "reload" -> {
                if (require(sender, GomokuPermission.ADMIN_RELOAD, "§c你没有权限重载五子棋。")) {
                    plugin.reloadAll();
                    sender.sendMessage("§a五子棋配置、房间和统计已重载。");
                }
            }
            case "resetstats" -> {
                if (!require(sender, GomokuPermission.ADMIN_STATS, "§c你没有权限重置统计。")) {
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§e用法: /gomoku admin resetstats <玩家>");
                } else {
                    sender.sendMessage(plugin.resetStats(plugin.offlinePlayer(args[2])));
                }
            }
            case "points", "addpoints", "givepoints" -> {
                if (!require(sender, GomokuPermission.ADMIN_STATS, "§c你没有权限发放五子棋积分。")) {
                    return;
                }
                if (args.length < 4) {
                    sender.sendMessage("§e用法: /gomoku admin points <玩家> <数量> [原因]");
                    return;
                }
                Integer amount = positiveInt(arg(args, 3));
                if (amount == null) {
                    sender.sendMessage("§e积分数量必须是正整数。");
                    return;
                }
                sender.sendMessage(plugin.addPoints(plugin.offlinePlayer(args[2]), amount, joinReason(args, 4)));
            }
            default -> sendAdminHelp(sender);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>(List.of("join", "spectate", "leave", "invite", "undo", "gui", "status", "leaderboard", "stats", "theme", "skin", "buy"));
            if (sender.hasPermission(GomokuPermission.ADMIN_ROOM.node()) || sender.hasPermission(GomokuPermission.ADMIN_STATS.node())) {
                values.add("var");
                values.add("admin");
                values.add("init");
                values.add("reset");
                values.add("reload");
            }
            return filter(values, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return filter(List.of("create", "place", "confirm", "cancel", "environment", "init", "open", "close", "refresh", "reset", "delete", "inspect", "stop", "release", "forfeit", "reload", "resetstats", "points"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("skin")) {
            return filter(plugin.appearances().pieceSkins().stream().map(PieceSkin::id).toList(), args[1]);
        }
        if (args.length == 2 && isInviteCommand(args[0])) {
            List<String> values = new ArrayList<>(List.of("accept", "deny"));
            values.addAll(plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
            return filter(values, args[1]);
        }
        if (args.length == 3 && isInviteCommand(args[0])) {
            return filter(plugin.rooms().rooms().stream().map(room -> room.config().id()).toList(), args[2]);
        }
        if (args.length == 2 && isUndoCommand(args[0])) {
            List<String> values = new ArrayList<>(List.of("accept", "deny"));
            values.addAll(plugin.rooms().rooms().stream().map(room -> room.config().id()).toList());
            return filter(values, args[1]);
        }
        if (args.length == 3 && isUndoCommand(args[0]) && List.of("accept", "deny").contains(args[1].toLowerCase(Locale.ROOT))) {
            return filter(plugin.rooms().rooms().stream().map(room -> room.config().id()).toList(), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            return filter(List.of("theme", "skin"), args[1]);
        }
        if ((args.length == 2 && List.of("join", "spectate", "status", "init", "reset").contains(args[0].toLowerCase(Locale.ROOT)))
            || (args.length == 2 && args[0].equalsIgnoreCase("theme"))
                || (args.length == 3 && args[0].equalsIgnoreCase("admin") && !List.of("create", "place", "confirm", "cancel", "resetstats", "points", "addpoints", "givepoints").contains(args[1].toLowerCase(Locale.ROOT)))) {
            return filter(plugin.rooms().rooms().stream().map(room -> room.config().id()).toList(), args[args.length - 1]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("place"))) {
            List<String> values = new ArrayList<>(List.of("size"));
            values.addAll(sizeSuggestions());
            return filter(values, args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("admin") && (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("place")) && args[3].equalsIgnoreCase("size")) {
            return filter(sizeSuggestions(), args[4]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && (args[1].equalsIgnoreCase("environment") || args[1].equalsIgnoreCase("env"))) {
            return filter(plugin.environments().templateIds(), args[3]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("theme")) {
            return filter(plugin.appearances().boardThemes().stream().map(BoardTheme::id).toList(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("buy") && args[1].equalsIgnoreCase("theme")) {
            return filter(plugin.appearances().boardThemes().stream().map(BoardTheme::id).toList(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("buy") && args[1].equalsIgnoreCase("skin")) {
            return filter(plugin.appearances().pieceSkins().stream().map(PieceSkin::id).toList(), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("buy") && args[1].equalsIgnoreCase("theme")) {
            return filter(plugin.rooms().rooms().stream().map(room -> room.config().id()).toList(), args[3]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && (args[1].equalsIgnoreCase("release") || args[1].equalsIgnoreCase("forfeit"))) {
            return filter(List.of("black", "white"), args[3]);
        }
        return List.of();
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以执行这个命令。");
            return null;
        }
        return player;
    }

    private boolean require(CommandSender sender, GomokuPermission permission, String deniedMessage) {
        if (sender.hasPermission(permission.node())) {
            return true;
        }
        sender.sendMessage(deniedMessage);
        return false;
    }

    private Boolean forceFlag(CommandSender sender, String[] args) {
        if (!hasToken(args, "force")) {
            return false;
        }
        if (!sender.hasPermission(GomokuPermission.ADMIN_FORCE.node())) {
            sender.sendMessage("§c你没有强制操作权限。");
            return null;
        }
        return true;
    }

    private boolean hasToken(String[] args, String token) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUndoCommand(String value) {
        return List.of("undo", "huiqi", "悔棋").contains(value.toLowerCase(Locale.ROOT));
    }

    private boolean isInviteCommand(String value) {
        return List.of("invite", "邀战", "邀请").contains(value.toLowerCase(Locale.ROOT));
    }

    private Stone sideArg(String value) {
        return switch (value == null ? "" : value.toLowerCase(Locale.ROOT)) {
            case "black", "b", "黑", "黑方" -> Stone.BLACK;
            case "white", "w", "白", "白方" -> Stone.WHITE;
            default -> Stone.EMPTY;
        };
    }

    private String arg(String[] args, int index) {
        return args.length > index ? args[index] : "";
    }

    private Integer positiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private RoomCreateOptions roomCreateOptions(CommandSender sender, String[] args, int start) {
        int boardSize = GomokuBoard.DEFAULT_SIZE;
        String templateId = "";
        int index = start;
        while (index < args.length) {
            String value = args[index];
            String normalized = value.toLowerCase(Locale.ROOT);
            if (List.of("size", "s").contains(normalized)) {
                if (index + 1 >= args.length) {
                    sender.sendMessage("§e缺少棋盘大小，例如 size 15 或 size 19。");
                    return null;
                }
                Integer parsed = boardSizeArg(args[index + 1]);
                if (parsed == null) {
                    sender.sendMessage(plugin.boardSizeError());
                    return null;
                }
                boardSize = parsed;
                index += 2;
                continue;
            }
            if (List.of("env", "environment", "template").contains(normalized)) {
                if (index + 1 >= args.length) {
                    sender.sendMessage("§e缺少环境模板 id。");
                    return null;
                }
                templateId = args[index + 1];
                index += 2;
                continue;
            }
            Integer parsed = boardSizeArg(value);
            if (parsed != null) {
                boardSize = parsed;
                index++;
                continue;
            }
            if (integerLike(value)) {
                sender.sendMessage(plugin.boardSizeError());
                return null;
            }
            templateId = value;
            index++;
        }
        return new RoomCreateOptions(boardSize, templateId);
    }

    private Integer boardSizeArg(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return GomokuBoard.isValidSize(parsed) ? parsed : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private boolean integerLike(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    private List<String> sizeSuggestions() {
        return List.of("9", "13", "15", "19", "25");
    }

    private String joinReason(String[] args, int start) {
        if (args.length <= start) {
            return "admin-grant";
        }
        return String.join(" ", java.util.Arrays.copyOfRange(args, start, args.length));
    }

    private void send(CommandSender sender, String message) {
        if (message != null && !message.isBlank()) {
            sender.sendMessage(message);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7用法: §f/gomoku gui §7| §f/gomoku join [room] §7| §f/gomoku invite <玩家> [room] §7| §f/gomoku leave §7| §f/gomoku undo [accept|deny] [room] §7| §f/gomoku status [room]");
        sender.sendMessage("§7查询: §f/gomoku leaderboard [points|wins|winrate] §7| §f/gomoku stats [玩家]");
        sender.sendMessage("§7外观: §f/gomoku skin <skin> §7| §f/gomoku theme <room> <theme> §7| §f/gomoku buy <theme|skin> <id>");
        if (sender.hasPermission(GomokuPermission.ADMIN_ROOM.node())) {
            sendAdminHelp(sender);
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§7管理: §f/gomoku admin place <房间> size 15 §7| §f/gomoku admin confirm/cancel §7| §f/gomoku admin create <房间> size 15");
        sender.sendMessage("§7房间: §f/gomoku admin init/open/close/refresh/reset/delete/inspect/stop/release/forfeit/reload");
        sender.sendMessage("§7统计: §f/gomoku admin points <玩家> <数量> [原因] §7| §f/gomoku admin resetstats <玩家>");
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(normalized)).toList();
    }

    private record RoomCreateOptions(int boardSize, String templateId) {
    }
}
