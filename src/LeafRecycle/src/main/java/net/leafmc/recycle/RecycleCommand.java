package net.leafmc.recycle;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class RecycleCommand implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of("submit", "collect", "admin", "reload", "help");
    private final LeafRecyclePlugin plugin;

    public RecycleCommand(LeafRecyclePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Player player = requirePlayer(sender);
            if (player != null && requirePermission(player, "leafrecycle.use")) {
                plugin.gui().openClaim(player, 0);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "submit", "add", "toufang" -> handleSubmit(sender);
            case "collect", "nearby" -> handleCollect(sender, args);
            case "admin", "manage" -> handleAdmin(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
        }
        return List.of();
    }

    private void handleSubmit(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null && requirePermission(player, "leafrecycle.submit")) {
            plugin.gui().openSubmit(player);
        }
    }

    private void handleCollect(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leafrecycle.admin")) {
            sender.sendMessage(plugin.prefix() + "你没有权限使用这个功能。");
            return;
        }
        if (args.length >= 2) {
            sender.sendMessage(plugin.prefix() + "现在不用填半径啦，直接 §e/recycle collect §f清理所有世界掉落物。");
            return;
        }
        plugin.startGlobalDropCollect(sender);
    }

    private void handleAdmin(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null && requirePermission(player, "leafrecycle.admin")) {
            plugin.gui().openAdmin(player, 0);
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("leafrecycle.reload")) {
            sender.sendMessage("§c你没有回收站重载权限。");
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(plugin.prefix() + "已重载配置和回收池数据。");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage("这个命令只能由玩家执行。");
        return null;
    }

    private boolean requirePermission(Player player, String permission) {
        if (player.hasPermission(permission)) {
            return true;
        }
        player.sendMessage(plugin.prefix() + "你没有权限使用这个功能。");
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.prefix() + "回收站命令：");
        sender.sendMessage("§7/recycle §f打开资源回收站领取 GUI");
        sender.sendMessage("§7/recycle submit §f投放物品到公共回收池");
        sender.sendMessage("§7/recycle collect §f管理员倒计时回收所有世界掉落物");
        sender.sendMessage("§7/recycle admin §f管理员管理回收池");
    }
}
