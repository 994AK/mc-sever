package net.leafmc.soulbind;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeafSoulbindPlugin extends JavaPlugin {
    private SoulbindService service;
    private PendingReturnStore pendingReturns;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadServices();

        SoulbindCommand command = new SoulbindCommand(this);
        PluginCommand pluginCommand = getCommand("soul");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(new SoulbindListener(this), this);
    }

    @Override
    public void onDisable() {
        savePendingReturns();
    }

    public SoulbindService soulbind() {
        return service;
    }

    public PendingReturnStore pendingReturns() {
        return pendingReturns;
    }

    public String prefix() {
        return text("messages.prefix", Map.of());
    }

    public void reloadAll() {
        savePendingReturns();
        reloadConfig();
        loadServices();
    }

    public boolean rule(String path) {
        return getConfig().getBoolean("rules." + path, true);
    }

    public String text(String path, Map<String, String> placeholders) {
        String message = getConfig().getString(path, "");
        Map<String, String> values = new HashMap<>();
        values.put("prefix", getConfig().getString("messages.prefix", "§d[灵魂绑定] §f"));
        values.putAll(placeholders);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(text(path, placeholders));
    }

    public void sendHelp(CommandSender sender) {
        List<String> lines = getConfig().getStringList("messages.help");
        if (lines.isEmpty()) {
            sender.sendMessage(prefix() + "/soul lock | unlock | bind | info | recover");
            return;
        }
        for (String line : lines) {
            Map<String, String> values = Map.of("prefix", getConfig().getString("messages.prefix", "§d[灵魂绑定] §f"));
            for (Map.Entry<String, String> entry : values.entrySet()) {
                line = line.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    public ReturnResult returnPendingItems(Player player) {
        List<ItemStack> pending = pendingReturns.remove(player.getUniqueId());
        if (pending.isEmpty()) {
            return new ReturnResult(0, 0);
        }

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(pending.toArray(ItemStack[]::new));
        int remaining = SoulbindService.countItems(overflow.values());
        int requested = SoulbindService.countItems(pending);
        if (!overflow.isEmpty()) {
            pendingReturns.replace(player.getUniqueId(), overflow.values().stream().toList());
        }
        savePendingReturns();
        player.updateInventory();
        return new ReturnResult(Math.max(0, requested - remaining), remaining);
    }

    public void savePendingReturns() {
        if (pendingReturns == null) {
            return;
        }
        try {
            pendingReturns.save();
        } catch (IOException exception) {
            getLogger().warning("Could not save LeafSoulbind pending returns: " + exception.getMessage());
        }
    }

    private void loadServices() {
        service = new SoulbindService(this);
        pendingReturns = new PendingReturnStore(new File(getDataFolder(), "pending-returns.yml"));
        try {
            pendingReturns.load();
        } catch (IOException exception) {
            getLogger().warning("Could not load LeafSoulbind pending returns: " + exception.getMessage());
        }
    }

    public record ReturnResult(int returnedItems, int remainingItems) {
    }
}
