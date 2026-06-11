package net.leafmc.gomoku;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.OfflinePlayer;

public final class VariableService {
    private final RoomRegistry rooms;
    private final StatsService stats;

    public VariableService(RoomRegistry rooms, StatsService stats) {
        this.rooms = rooms;
        this.stats = stats;
    }

    public String resolve(String rawKey, OfflinePlayer player) {
        String key = normalize(rawKey);
        if (key.equals("room_count")) {
            return Integer.toString(rooms.rooms().size());
        }
        if (key.startsWith("room_")) {
            return resolveRoom(key.substring("room_".length()));
        }
        if (key.startsWith("player_")) {
            return resolvePlayer(key.substring("player_".length()), player);
        }
        if (key.startsWith("top_")) {
            return resolveTop(key.substring("top_".length()));
        }
        return "";
    }

    private String resolveRoom(String rest) {
        String field = roomField(rest);
        if (field.isEmpty()) {
            return "";
        }
        String roomId = rest.substring(0, rest.length() - field.length() - 1);
        Optional<GomokuRoom> room = rooms.room(roomId);
        if (room.isEmpty()) {
            return "";
        }
        return switch (field) {
            case "state" -> room.get().state().name().toLowerCase(Locale.ROOT);
            case "black" -> room.get().lease(Stone.BLACK).map(SeatLease::playerName).orElse("");
            case "white" -> room.get().lease(Stone.WHITE).map(SeatLease::playerName).orElse("");
            case "black_ready" -> Boolean.toString(room.get().lease(Stone.BLACK).map(lease -> !lease.disconnected()).orElse(false));
            case "white_ready" -> Boolean.toString(room.get().lease(Stone.WHITE).map(lease -> !lease.disconnected()).orElse(false));
            case "turn" -> room.get().match().currentTurn().displayName();
            case "theme" -> room.get().appearance().boardTheme().displayName();
            case "black_skin" -> room.get().appearance().blackSkin().displayName();
            case "white_skin" -> room.get().appearance().whiteSkin().displayName();
            case "spectators" -> Integer.toString(room.get().spectatorCount());
            case "spectator_capacity", "capacity" -> Integer.toString(room.get().config().spectatorCapacity());
            case "last_result", "result" -> room.get().lastResult();
            default -> "";
        };
    }

    private String roomField(String rest) {
        for (String field : List.of("spectator_capacity", "last_result", "black_ready", "white_ready", "black_skin", "white_skin", "spectators", "state", "black", "white", "theme", "turn", "capacity", "result")) {
            if (rest.endsWith("_" + field) && rest.length() > field.length() + 1) {
                return field;
            }
        }
        return "";
    }

    private String resolvePlayer(String field, OfflinePlayer player) {
        if (player == null) {
            return "";
        }
        PlayerStats value = stats.statsFor(player);
        return switch (field) {
            case "points" -> Integer.toString(value.points());
            case "wins" -> Integer.toString(value.wins());
            case "losses" -> Integer.toString(value.losses());
            case "draws" -> Integer.toString(value.draws());
            case "games" -> Integer.toString(value.games());
            case "rank" -> Integer.toString(stats.rank(player.getUniqueId()));
            case "streak" -> Integer.toString(value.currentStreak());
            default -> "";
        };
    }

    private String resolveTop(String rest) {
        String[] parts = rest.split("_", 2);
        if (parts.length != 2) {
            return "";
        }
        int index;
        try {
            index = Integer.parseInt(parts[0]) - 1;
        } catch (NumberFormatException error) {
            return "";
        }
        List<PlayerStats> rows = stats.leaderboard("points", index + 1);
        if (index < 0 || index >= rows.size()) {
            return "";
        }
        PlayerStats row = rows.get(index);
        return switch (parts[1]) {
            case "name" -> row.playerName();
            case "points" -> Integer.toString(row.points());
            case "wins" -> Integer.toString(row.wins());
            default -> "";
        };
    }

    private String normalize(String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("%")) {
            key = key.substring(1);
        }
        if (key.startsWith("leafgomoku_")) {
            key = key.substring("leafgomoku_".length());
        }
        if (key.endsWith("%")) {
            key = key.substring(0, key.length() - 1);
        }
        return key;
    }
}
