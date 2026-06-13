package net.leafmc.recycle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeafRecyclePlugin extends JavaPlugin {
    private final Object saveFileLock = new Object();
    private final Object saveQueueLock = new Object();
    private RecycleStore store;
    private RecycleService service;
    private RecycleGui gui;
    private boolean collectCountdownRunning;
    private boolean asyncSaveRunning;
    private boolean asyncSaveQueued;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadServices();

        RecycleCommand command = new RecycleCommand(this);
        PluginCommand pluginCommand = getCommand("recycle");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        gui = new RecycleGui(this);
        getServer().getPluginManager().registerEvents(gui, this);
    }

    @Override
    public void onDisable() {
        if (service != null) {
            saveRecycleDataNow();
        }
    }

    public RecycleService recycle() {
        return service;
    }

    public RecycleGui gui() {
        return gui;
    }

    public String prefix() {
        return getConfig().getString("messages.prefix", "§a[回收] §f");
    }

    public void reloadAll() {
        saveRecycleDataNow();
        reloadConfig();
        loadServices();
        refreshRecycleViews();
    }

    public void recyclePoolChanged() {
        saveRecycleDataAsync();
        refreshRecycleViews();
    }

    public void saveRecycleDataAsync() {
        synchronized (saveQueueLock) {
            asyncSaveQueued = true;
            if (asyncSaveRunning) {
                return;
            }
            asyncSaveRunning = true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, this::runQueuedSaves);
    }

    private void saveRecycleDataNow() {
        RecycleStore targetStore = store;
        RecycleService targetService = service;
        if (targetStore == null || targetService == null) {
            return;
        }
        synchronized (saveQueueLock) {
            asyncSaveQueued = false;
        }
        saveRecycleDataSnapshot(targetStore, targetService.entries());
    }

    private void runQueuedSaves() {
        while (true) {
            synchronized (saveQueueLock) {
                if (!asyncSaveQueued) {
                    asyncSaveRunning = false;
                    return;
                }
                asyncSaveQueued = false;
            }
            RecycleStore targetStore = store;
            RecycleService targetService = service;
            if (targetStore != null && targetService != null) {
                saveRecycleDataSnapshot(targetStore, targetService.entries());
            }
        }
    }

    private void saveRecycleDataSnapshot(RecycleStore targetStore, List<RecycleEntry> snapshot) {
        try {
            synchronized (saveFileLock) {
                targetStore.save(snapshot);
            }
        } catch (IOException exception) {
            getLogger().warning("Could not save LeafRecycle data: " + exception.getMessage());
        }
    }

    public void startGlobalDropCollect(CommandSender sender) {
        if (collectCountdownRunning) {
            sender.sendMessage(collectMessage("collect.messages.busy", Map.of()));
            return;
        }
        int seconds = Math.max(0, getConfig().getInt("collect.countdown-seconds", 10));
        if (seconds <= 0) {
            collectAllWorldDrops(sender);
            return;
        }

        collectCountdownRunning = true;
        broadcastCollectMessage("collect.messages.start", Map.of("seconds", String.valueOf(seconds)));
        for (int remaining : collectAlertSeconds(seconds)) {
            if (remaining == seconds) {
                continue;
            }
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (collectCountdownRunning) {
                    broadcastCollectMessage("collect.messages.tick", Map.of("seconds", String.valueOf(remaining)));
                }
            }, (long) (seconds - remaining) * 20L);
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!collectCountdownRunning) {
                return;
            }
            collectCountdownRunning = false;
            collectAllWorldDrops(sender);
        }, (long) seconds * 20L);
    }

    private void collectAllWorldDrops(CommandSender sender) {
        List<Item> drops = new ArrayList<>();
        Set<String> worldsWithDrops = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.isDead() || item.getItemStack().getType().isAir()) {
                    continue;
                }
                drops.add(item);
                worldsWithDrops.add(world.getName());
            }
        }

        if (drops.isEmpty()) {
            broadcastCollectMessage("collect.messages.empty", Map.of());
            return;
        }

        int acceptedItems = 0;
        int rejectedItems = 0;
        String contributorName = getConfig().getString("collect.contributor-name", "小扫帚");
        for (Item drop : drops) {
            if (drop.isDead()) {
                continue;
            }
            ItemStack stack = drop.getItemStack().clone();
            RecycleService.AddResult result = service.addItems(List.of(stack), null, contributorName);
            acceptedItems += result.acceptedItems();
            rejectedItems += result.rejectedItems();
            if (result.acceptedItems() <= 0) {
                continue;
            }
            if (result.rejectedStacks().isEmpty()) {
                drop.remove();
            } else {
                drop.setItemStack(result.rejectedStacks().get(0));
            }
        }
        if (acceptedItems > 0) {
            recyclePoolChanged();
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("accepted", String.valueOf(acceptedItems));
        placeholders.put("rejected", String.valueOf(rejectedItems));
        placeholders.put("stacks", String.valueOf(drops.size()));
        placeholders.put("worlds", String.valueOf(worldsWithDrops.size()));
        String message = collectMessage("collect.messages.done", placeholders);
        if (rejectedItems > 0) {
            message += collectMessage("collect.messages.rejected", placeholders);
        }
        broadcastMessage(message);
    }

    public boolean hasInventoryRoom(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        int remaining = item.getAmount();
        for (ItemStack content : inventory.getStorageContents()) {
            if (remaining <= 0) {
                return true;
            }
            if (content == null || content.getType().isAir()) {
                remaining -= item.getMaxStackSize();
                continue;
            }
            if (content.isSimilar(item)) {
                remaining -= Math.max(0, content.getMaxStackSize() - content.getAmount());
            }
        }
        return remaining <= 0;
    }

    private void refreshRecycleViews() {
        if (gui == null || !isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(this, gui::refreshOpenClaimViews);
    }

    private void loadServices() {
        store = new YamlRecycleStore(new File(getDataFolder(), "recycle.yml"));
        List<RecycleEntry> entries;
        try {
            entries = store.load();
        } catch (IOException exception) {
            getLogger().warning("Could not load LeafRecycle data: " + exception.getMessage());
            entries = List.of();
        }
        service = new RecycleService(entries, getConfig().getInt("storage.max-stored-stacks", 270));
    }

    private List<Integer> collectAlertSeconds(int totalSeconds) {
        List<Integer> configured = getConfig().getIntegerList("collect.countdown-alerts");
        if (configured.isEmpty()) {
            configured = List.of(totalSeconds, 5, 3, 2, 1);
        }
        List<Integer> result = new ArrayList<>();
        for (int remaining : configured) {
            if (remaining <= 0 || remaining > totalSeconds || result.contains(remaining)) {
                continue;
            }
            result.add(remaining);
        }
        return result;
    }

    private void broadcastCollectMessage(String path, Map<String, String> placeholders) {
        broadcastMessage(collectMessage(path, placeholders));
    }

    private void broadcastMessage(String message) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().info(org.bukkit.ChatColor.stripColor(message));
            return;
        }
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
    }

    private String collectMessage(String path, Map<String, String> placeholders) {
        String message = getConfig().getString(path, "");
        Map<String, String> values = new HashMap<>();
        values.put("prefix", prefix());
        values.putAll(placeholders);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
}
