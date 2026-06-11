package net.leafmc.gomoku;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

public final class RoomRegistry {
    private final JavaPlugin plugin;
    private final StatsService statsService;
    private final AppearanceCatalog appearanceCatalog;
    private final AppearanceUnlockService appearanceUnlocks;
    private final RoomChatService roomChat;
    private final Map<String, GomokuRoom> rooms = new LinkedHashMap<>();

    public RoomRegistry(
        JavaPlugin plugin,
        StatsService statsService,
        AppearanceCatalog appearanceCatalog,
        AppearanceUnlockService appearanceUnlocks,
        RoomChatService roomChat
    ) {
        this.plugin = plugin;
        this.statsService = statsService;
        this.appearanceCatalog = appearanceCatalog;
        this.appearanceUnlocks = appearanceUnlocks;
        this.roomChat = roomChat;
    }

    public void replaceAll(List<ArenaConfig> configs) {
        shutdown();
        rooms.clear();
        for (ArenaConfig config : configs) {
            rooms.put(config.id(), new GomokuRoom(plugin, config, statsService, appearanceCatalog, appearanceUnlocks, roomChat));
        }
    }

    public void put(ArenaConfig config) {
        GomokuRoom old = rooms.remove(config.id());
        if (old != null) {
            old.shutdown();
        }
        rooms.put(config.id(), new GomokuRoom(plugin, config, statsService, appearanceCatalog, appearanceUnlocks, roomChat));
    }

    public Optional<GomokuRoom> room(String roomId) {
        return Optional.ofNullable(rooms.get(ArenaConfig.normalizeRoomId(roomId)));
    }

    public Optional<GomokuRoom> defaultRoom() {
        GomokuRoom main = rooms.get(ArenaConfig.MAIN_ROOM_ID);
        if (main != null) {
            return Optional.of(main);
        }
        return rooms.values().stream().findFirst();
    }

    public Collection<GomokuRoom> rooms() {
        return List.copyOf(rooms.values());
    }

    public Optional<GomokuRoom> participantRoom(UUID playerId) {
        return rooms.values().stream().filter(room -> room.containsParticipant(playerId)).findFirst();
    }

    public Optional<GomokuRoom> spectatorRoom(UUID playerId) {
        return rooms.values().stream().filter(room -> room.containsSpectator(playerId)).findFirst();
    }

    public Optional<MappedCell> mapBoardBlock(Block block) {
        BlockPoint point = point(block);
        World world = block.getWorld();
        for (GomokuRoom room : rooms.values()) {
            ArenaConfig config = room.config();
            if (!sameWorld(config, world)) {
                continue;
            }
            Optional<GridCell> cell = config.geometry().mapBoardCell(point)
                .or(() -> config.geometry().mapPieceCell(point));
            if (cell.isPresent()) {
                return Optional.of(new MappedCell(room, cell.get()));
            }
        }
        return Optional.empty();
    }

    public boolean isProtected(Block block) {
        BlockPoint point = point(block);
        World world = block.getWorld();
        for (GomokuRoom room : rooms.values()) {
            ArenaConfig config = room.config();
            if (!sameWorld(config, world)) {
                continue;
            }
            if (config.geometry().protects(point)
                || point.equals(config.blackEmitter())
                || point.equals(config.whiteEmitter())) {
                return true;
            }
        }
        return false;
    }

    public void markDisconnected(org.bukkit.entity.Player player) {
        participantRoom(player.getUniqueId()).ifPresent(room -> room.markDisconnected(player));
    }

    public void markOnline(org.bukkit.entity.Player player) {
        participantRoom(player.getUniqueId()).ifPresent(room -> room.markOnline(player));
    }

    public void shutdown() {
        for (GomokuRoom room : rooms.values()) {
            room.shutdown();
        }
    }

    public void remove(String roomId) {
        GomokuRoom room = rooms.remove(ArenaConfig.normalizeRoomId(roomId));
        if (room != null) {
            room.shutdown();
        }
    }

    private boolean sameWorld(ArenaConfig config, World world) {
        return config.enabled() && world != null && world.getName().equals(config.worldName());
    }

    private BlockPoint point(Block block) {
        return new BlockPoint(block.getX(), block.getY(), block.getZ());
    }

    public record MappedCell(GomokuRoom room, GridCell cell) {
    }
}
