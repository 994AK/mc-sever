package net.leafmc.gomoku;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RoomChatService implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, String> playerRooms = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> roomMembers = new ConcurrentHashMap<>();

    public RoomChatService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enter(String roomId, Player player) {
        String normalized = ArenaConfig.normalizeRoomId(roomId);
        leave(player.getUniqueId(), false);
        playerRooms.put(player.getUniqueId(), normalized);
        roomMembers.computeIfAbsent(normalized, ignored -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
        player.sendMessage("§7已进入五子棋房间频道: §e" + normalized);
    }

    public void leave(UUID playerId, boolean notify) {
        String roomId = playerRooms.remove(playerId);
        if (roomId == null) {
            return;
        }
        Set<UUID> members = roomMembers.get(roomId);
        if (members != null) {
            members.remove(playerId);
            if (members.isEmpty()) {
                roomMembers.remove(roomId);
            }
        }
        if (notify) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§7已离开五子棋房间频道: §e" + roomId);
            }
        }
    }

    public void clearRoom(String roomId, boolean notify) {
        String normalized = ArenaConfig.normalizeRoomId(roomId);
        Set<UUID> members = roomMembers.remove(normalized);
        if (members == null) {
            return;
        }
        for (UUID playerId : members) {
            playerRooms.remove(playerId);
            if (notify) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.sendMessage("§7五子棋房间频道已解散: §e" + normalized);
                }
            }
        }
    }

    public void clearAll() {
        for (String roomId : Set.copyOf(roomMembers.keySet())) {
            clearRoom(roomId, false);
        }
        playerRooms.clear();
        roomMembers.clear();
    }

    public Optional<String> roomFor(UUID playerId) {
        return Optional.ofNullable(playerRooms.get(playerId));
    }

    public boolean isInRoom(UUID playerId, String roomId) {
        return ArenaConfig.normalizeRoomId(roomId).equals(playerRooms.get(playerId));
    }

    public void broadcast(String roomId, String message) {
        Runnable send = () -> sendNow(roomId, message);
        if (Bukkit.isPrimaryThread()) {
            send.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, send);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Optional<String> roomId = roomFor(event.getPlayer().getUniqueId());
        if (roomId.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        event.viewers().removeIf(viewer -> !viewerIsRoomMember(viewer, roomId.get()));
        Component message = event.message();
        String senderName = event.getPlayer().getName();
        UUID senderId = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> sendChatNow(roomId.get(), senderId, senderName, message));
    }

    private boolean viewerIsRoomMember(Audience viewer, String roomId) {
        return viewer instanceof Player player && isInRoom(player.getUniqueId(), roomId);
    }

    private void sendNow(String roomId, String message) {
        String normalized = ArenaConfig.normalizeRoomId(roomId);
        Set<UUID> members = roomMembers.get(normalized);
        if (members == null || members.isEmpty()) {
            return;
        }
        String line = "§6[五子棋:" + normalized + "] " + message;
        for (UUID playerId : Set.copyOf(members)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(line);
            }
        }
    }

    private void sendChatNow(String roomId, UUID senderId, String senderName, Component message) {
        String normalized = ArenaConfig.normalizeRoomId(roomId);
        Set<UUID> members = roomMembers.get(normalized);
        if (members == null || members.isEmpty() || !normalized.equals(playerRooms.get(senderId))) {
            return;
        }
        Component line = Component.text("[五子棋:" + normalized + "] ", NamedTextColor.GOLD)
            .append(Component.text(senderName, NamedTextColor.WHITE))
            .append(Component.text(" » ", NamedTextColor.GRAY))
            .append(message);
        for (UUID playerId : Set.copyOf(members)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(line);
            }
        }
    }
}
