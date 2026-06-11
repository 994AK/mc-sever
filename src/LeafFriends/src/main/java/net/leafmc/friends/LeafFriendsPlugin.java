package net.leafmc.friends;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeafFriendsPlugin extends JavaPlugin {
    private FriendService friendService;
    private TeleportRequestService teleportRequests;
    private FriendStore store;
    private FriendGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadServices();

        FriendCommand command = new FriendCommand(this);
        PluginCommand pluginCommand = getCommand("friend");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        gui = new FriendGui(this);
        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(new FriendListener(this), this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            friendService.markOnline(player.getUniqueId(), player.getName());
        }
        saveFriendData();
    }

    @Override
    public void onDisable() {
        if (friendService != null) {
            saveFriendData();
        }
    }

    public FriendService friends() {
        return friendService;
    }

    public TeleportRequestService teleports() {
        return teleportRequests;
    }

    public FriendGui gui() {
        return gui;
    }

    public String prefix() {
        return getConfig().getString("messages.prefix", "§b[好友] §f");
    }

    public void reloadAll() {
        reloadConfig();
        loadServices();
        for (Player player : Bukkit.getOnlinePlayers()) {
            friendService.markOnline(player.getUniqueId(), player.getName());
        }
        saveFriendData();
    }

    public void saveFriendData() {
        try {
            store.save(friendService.profiles());
        } catch (IOException exception) {
            getLogger().warning("Could not save LeafFriends data: " + exception.getMessage());
        }
    }

    public Optional<PlayerTarget> resolveTarget(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        Player online = Bukkit.getPlayerExact(rawName);
        if (online != null) {
            friendService.ensureProfile(online.getUniqueId(), online.getName());
            return Optional.of(new PlayerTarget(online.getUniqueId(), online.getName(), online));
        }
        return friendService.findByName(rawName)
            .map(profile -> new PlayerTarget(profile.id(), profile.latestName(), Bukkit.getPlayer(profile.id())));
    }

    public boolean teleportWorldAllowed(String worldName) {
        Set<String> allowed = allowedTeleportWorlds();
        return allowed.isEmpty() || allowed.contains(worldName.toLowerCase(java.util.Locale.ROOT));
    }

    public void handleJoin(Player player) {
        friendService.markOnline(player.getUniqueId(), player.getName());
        notifyFriends(player, true);
        saveFriendData();
    }

    public void handleQuit(Player player) {
        notifyFriends(player, false);
        friendService.markOffline(player.getUniqueId());
        saveFriendData();
    }

    private void loadServices() {
        store = new YamlFriendStore(new File(getDataFolder(), "friends.yml"));
        List<FriendProfile> profiles;
        try {
            profiles = store.load();
        } catch (IOException exception) {
            getLogger().warning("Could not load LeafFriends data: " + exception.getMessage());
            profiles = List.of();
        }
        long requestExpiry = seconds("request-expiry-seconds", 120) * 1_000L;
        long requestCooldown = seconds("request-cooldown-seconds", 15) * 1_000L;
        long teleportExpiry = seconds("teleport-expiry-seconds", 60) * 1_000L;
        long teleportCooldown = seconds("teleport-cooldown-seconds", 30) * 1_000L;
        friendService = new FriendService(profiles, TimeSource.system(), requestExpiry, requestCooldown);
        teleportRequests = new TeleportRequestService(friendService, TimeSource.system(), teleportExpiry, teleportCooldown, allowedTeleportWorlds());
    }

    private void notifyFriends(Player player, boolean online) {
        FriendProfile subject = friendService.profile(player.getUniqueId()).orElse(null);
        if (subject == null || !subject.settings().showOnlineStatus()) {
            return;
        }
        String state = online ? "上线了" : "下线了";
        for (FriendProfile friend : friendService.onlineFriends(player.getUniqueId())) {
            if (!friend.settings().onlineNotifications()) {
                continue;
            }
            Player target = Bukkit.getPlayer(friend.id());
            if (target != null) {
                target.sendMessage(prefix() + "你的好友 §a" + player.getName() + " §f" + state + "。");
            }
        }
    }

    private long seconds(String path, long fallback) {
        return Math.max(1L, getConfig().getLong(path, fallback));
    }

    private Set<String> allowedTeleportWorlds() {
        Set<String> worlds = new HashSet<>();
        for (String name : getConfig().getStringList("allowed-teleport-worlds")) {
            if (name != null && !name.isBlank()) {
                worlds.add(name.toLowerCase(java.util.Locale.ROOT));
            }
        }
        return worlds;
    }

    public record PlayerTarget(UUID id, String name, Player onlinePlayer) {
        public boolean online() {
            return onlinePlayer != null && onlinePlayer.isOnline();
        }
    }
}
