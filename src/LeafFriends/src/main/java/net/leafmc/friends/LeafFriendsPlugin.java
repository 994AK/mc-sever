package net.leafmc.friends;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    public void requestTeleport(Player player, PlayerTarget target) {
        if (target.onlinePlayer() == null) {
            if (friendService.hidesOnlineStatus(target.id())) {
                player.sendMessage(prefix() + "如果对方在线且允许，会收到你的好友传送请求。");
            } else {
                player.sendMessage(prefix() + "好友不在线，不能发送好友传送。");
            }
            return;
        }
        if (player.isDead() || target.onlinePlayer().isDead()) {
            player.sendMessage(prefix() + "双方都存活时才能发送好友传送。");
            return;
        }
        if (friendService.isTrustedTeleporter(target.id(), player.getUniqueId())) {
            TeleportRequestService.Result direct = teleportRequests.direct(
                player.getUniqueId(),
                target.id(),
                player.isOnline(),
                target.onlinePlayer().isOnline(),
                player.getWorld().getName(),
                target.onlinePlayer().getWorld().getName()
            );
            if (!direct.ok()) {
                player.sendMessage(prefix() + direct.message());
                return;
            }
            if (player.teleport(target.onlinePlayer().getLocation())) {
                player.sendMessage(prefix() + "已直接传送到可信好友 §a" + target.name() + " §f身边。");
                target.onlinePlayer().sendMessage(prefix() + "可信好友 §a" + player.getName() + " §f已传送到你身边。");
            } else {
                player.sendMessage(prefix() + "传送失败，请稍后再试。");
            }
            return;
        }
        TeleportRequestService.Result result = teleportRequests.request(
            player.getUniqueId(),
            target.id(),
            player.isOnline(),
            target.onlinePlayer().isOnline(),
            player.getWorld().getName(),
            target.onlinePlayer().getWorld().getName()
        );
        if (friendService.hidesOnlineStatus(target.id())) {
            player.sendMessage(prefix() + "如果对方在线且允许，会收到你的好友传送请求。");
        } else {
            player.sendMessage(prefix() + result.message());
        }
        if (result.ok()) {
            sendTeleportPrompt(target.onlinePlayer(), player.getName());
        }
    }

    public void acceptTeleport(Player target, PlayerTarget sender) {
        if (sender.onlinePlayer() == null) {
            target.sendMessage(prefix() + "对方不在线。");
            return;
        }
        if (target.isDead() || sender.onlinePlayer().isDead()) {
            target.sendMessage(prefix() + "双方都存活时才能接受好友传送。");
            return;
        }
        if (!teleportWorldAllowed(target.getWorld().getName())) {
            target.sendMessage(prefix() + "当前世界不允许好友传送。");
            return;
        }
        if (!teleportWorldAllowed(sender.onlinePlayer().getWorld().getName())) {
            target.sendMessage(prefix() + "对方当前世界不允许好友传送。");
            return;
        }
        TeleportRequestService.Result result = teleportRequests.accept(
            target.getUniqueId(),
            sender.id(),
            sender.onlinePlayer().getWorld().getName(),
            target.getWorld().getName()
        );
        if (!result.ok()) {
            target.sendMessage(prefix() + result.message());
            return;
        }
        if (sender.onlinePlayer().teleport(target.getLocation())) {
            sender.onlinePlayer().sendMessage(prefix() + "已传送到好友 §a" + target.getName() + " §f身边。");
            target.sendMessage(prefix() + "已同意 §a" + sender.name() + " §f的好友传送。");
        } else {
            target.sendMessage(prefix() + "传送失败，请稍后再试。");
        }
    }

    public void sendMessageSuggestion(Player player, String friendName) {
        Component line = legacy(prefix() + "§7点击填写好友私聊: ")
            .append(Component.text("/friend msg " + friendName + " ", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.suggestCommand("/friend msg " + friendName + " "))
                .hoverEvent(HoverEvent.showText(Component.text("点击后在聊天栏填写内容", NamedTextColor.GRAY))));
        player.sendMessage(line);
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

    private void sendTeleportPrompt(Player target, String senderName) {
        target.sendMessage(prefix() + "§a" + senderName + " §f请求传送到你身边。");
        Component line = legacy(prefix() + "§7点击操作: ")
            .append(Component.text("[同意]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/friend tpaccept " + senderName))
                .hoverEvent(HoverEvent.showText(Component.text("同意这次好友传送", NamedTextColor.GREEN))))
            .append(Component.text("  "))
            .append(Component.text("[拒绝]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/friend tpdeny " + senderName))
                .hoverEvent(HoverEvent.showText(Component.text("拒绝这次好友传送", NamedTextColor.RED))))
            .append(Component.text("  "))
            .append(Component.text("[好友菜单]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/menufriends"))
                .hoverEvent(HoverEvent.showText(Component.text("打开好友菜单处理请求", NamedTextColor.GRAY))));
        target.sendMessage(line);
    }

    private Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
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
