package net.leafmc.soulbind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SoulbindCommand implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of("lock", "unlock", "bind", "info", "recover", "reload", "help");
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("unlock");
    private final LeafSoulbindPlugin plugin;

    public SoulbindCommand(LeafSoulbindPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && label.equalsIgnoreCase("slock")) {
            handleLock(sender);
            return true;
        }
        if (args.length == 0 && label.equalsIgnoreCase("sunlock")) {
            handleUnlock(sender, false);
            return true;
        }
        if (args.length == 0) {
            plugin.sendHelp(sender);
            return true;
        }

        SoulbindSubcommand subcommand = SoulbindSubcommand.parse(args[0]).orElse(SoulbindSubcommand.HELP);
        switch (subcommand) {
            case LOCK -> handleLock(sender);
            case UNLOCK -> handleUnlock(sender, false);
            case BIND -> handleBind(sender);
            case INFO -> handleInfo(sender);
            case RECOVER -> handleRecover(sender);
            case RELOAD -> handleReload(sender);
            case ADMIN -> handleAdmin(sender, args);
            case HELP -> plugin.sendHelp(sender);
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
        if (args.length == 2 && SoulbindSubcommand.parse(args[0]).orElse(null) == SoulbindSubcommand.ADMIN) {
            return ADMIN_SUBCOMMANDS.stream()
                .filter(value -> value.startsWith(args[1].toLowerCase(Locale.ROOT)))
                .toList();
        }
        return List.of();
    }

    private void handleLock(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "leafsoulbind.lock")) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        SoulbindService.MarkResult result = plugin.soulbind().lock(item);
        switch (result) {
            case CHANGED -> plugin.send(player, "messages.locked", Map.of());
            case ALREADY_LOCKED -> plugin.send(player, "messages.already-locked", Map.of());
            case ALREADY_BOUND -> plugin.send(player, "messages.already-bound", Map.of("owner", plugin.soulbind().ownerName(item)));
            case EMPTY -> plugin.send(player, "messages.empty-hand", Map.of());
        }
        player.updateInventory();
    }

    private void handleBind(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "leafsoulbind.bind")) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        SoulbindService.MarkResult result = plugin.soulbind().bind(item, player.getUniqueId(), player.getName());
        switch (result) {
            case CHANGED -> plugin.send(player, "messages.bound", Map.of("player", player.getName()));
            case ALREADY_BOUND -> plugin.send(player, "messages.already-bound", Map.of("owner", plugin.soulbind().ownerName(item)));
            case ALREADY_LOCKED -> plugin.send(player, "messages.already-locked", Map.of());
            case EMPTY -> plugin.send(player, "messages.empty-hand", Map.of());
        }
        player.updateInventory();
    }

    private void handleUnlock(CommandSender sender, boolean force) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!force && !requirePermission(player, "leafsoulbind.unlock")) {
            return;
        }
        boolean bypass = force || player.hasPermission("leafsoulbind.admin") || player.hasPermission("leafsoulbind.bypass");
        ItemStack item = player.getInventory().getItemInMainHand();
        SoulbindService.UnlockResult result = plugin.soulbind().unlock(item, player.getUniqueId(), bypass);
        switch (result) {
            case CHANGED -> plugin.send(player, "messages.unlocked", Map.of());
            case DENIED -> plugin.send(player, "messages.unlock-denied", Map.of("owner", plugin.soulbind().ownerName(item)));
            case NOT_PROTECTED -> plugin.send(player, "messages.not-protected", Map.of());
        }
        player.updateInventory();
    }

    private void handleInfo(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "leafsoulbind.use")) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.soulbind().isProtected(item)) {
            plugin.send(player, "messages.info-unprotected", Map.of());
            return;
        }
        if (plugin.soulbind().isBound(item)) {
            plugin.send(player, "messages.info-bound", Map.of("owner", plugin.soulbind().ownerName(item)));
        } else {
            plugin.send(player, "messages.info-locked", Map.of());
        }
    }

    private void handleRecover(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null || !requirePermission(player, "leafsoulbind.recover")) {
            return;
        }
        LeafSoulbindPlugin.ReturnResult result = plugin.returnPendingItems(player);
        if (result.returnedItems() <= 0 && result.remainingItems() <= 0) {
            plugin.send(player, "messages.recover-empty", Map.of());
            return;
        }
        if (result.remainingItems() > 0) {
            plugin.send(player, "messages.recover-partial", Map.of(
                "returned", String.valueOf(result.returnedItems()),
                "remaining", String.valueOf(result.remainingItems())
            ));
            return;
        }
        plugin.send(player, "messages.recovered", Map.of("count", String.valueOf(result.returnedItems())));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("leafsoulbind.reload")) {
            plugin.send(sender, "messages.no-permission", Map.of());
            return;
        }
        plugin.reloadAll();
        plugin.send(sender, "messages.reloaded", Map.of());
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leafsoulbind.admin")) {
            plugin.send(sender, "messages.no-permission", Map.of());
            return;
        }
        if (args.length >= 2 && SoulbindSubcommand.parse(args[1]).orElse(null) == SoulbindSubcommand.UNLOCK) {
            handleUnlock(sender, true);
            return;
        }
        sender.sendMessage(plugin.prefix() + "管理员命令：§7/soul admin unlock §f强制解除手持物品锁定/绑定");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        plugin.send(sender, "messages.player-only", Map.of());
        return null;
    }

    private boolean requirePermission(Player player, String permission) {
        if (player.hasPermission(permission) || player.hasPermission("leafsoulbind.admin")) {
            return true;
        }
        plugin.send(player, "messages.no-permission", Map.of());
        return false;
    }
}
