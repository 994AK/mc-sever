package net.leafmc.gomoku;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public final class PlacementToolService {
    private final LeafGomokuPlugin plugin;
    private final Map<UUID, PendingPlacement> pendingRooms = new ConcurrentHashMap<>();
    private final Map<UUID, PreviewPlacement> previews = new ConcurrentHashMap<>();

    public PlacementToolService(LeafGomokuPlugin plugin) {
        this.plugin = plugin;
    }

    public String begin(Player admin, String roomId) {
        return begin(admin, roomId, GomokuBoard.DEFAULT_SIZE);
    }

    public String begin(Player admin, String roomId, int boardSize) {
        return begin(admin, roomId, boardSize, "");
    }

    public String begin(Player admin, String roomId, int boardSize, String templateId) {
        String normalized = ArenaConfig.normalizeRoomId(roomId);
        if (!ArenaConfig.isValidRoomId(normalized)) {
            return "§c房间 id 只能使用小写字母、数字、下划线或短横线，最长 32 位。";
        }
        if (!GomokuBoard.isValidSize(boardSize)) {
            return plugin.boardSizeError();
        }
        if (plugin.rooms().room(normalized).isPresent()) {
            return "§c房间已存在: " + normalized;
        }
        String normalizedTemplate = EnvironmentCatalog.normalizeId(templateId);
        RoomEnvironmentTemplate template = normalizedTemplate.isBlank()
            ? plugin.environments().defaultTemplate()
            : plugin.environments().template(normalizedTemplate).orElse(null);
        if (template == null) {
            return "§c找不到环境模板: " + templateId;
        }
        UUID playerId = admin.getUniqueId();
        pendingRooms.put(playerId, new PendingPlacement(normalized, boardSize, template.id()));
        previews.remove(playerId);
        return "§a已准备放置五子棋房间 " + normalized + "，棋盘大小 §f" + boardSize + "x" + boardSize + "§a。右键目标地面先预览，再执行 §f/gomoku admin confirm §a确认创建。";
    }

    public boolean handleRightClick(Player admin, Block clickedBlock, BlockFace face) {
        PendingPlacement placement = pendingRooms.get(admin.getUniqueId());
        if (placement == null) {
            return false;
        }
        if (!admin.hasPermission(GomokuPermission.ADMIN_SETUP.node())) {
            admin.sendMessage("§c你没有权限创建五子棋房间。");
            return true;
        }
        Block anchorBlock = clickedBlock.getRelative(face == null ? BlockFace.UP : face);
        Location anchor = anchorBlock.getLocation();
        anchor.setYaw(admin.getLocation().getYaw());
        anchor.setPitch(0.0F);
        ArenaConfig config;
        try {
            config = plugin.createRoomConfig(placement.roomId(), anchor, placement.boardSize(), placement.templateId());
        } catch (IllegalArgumentException error) {
            admin.sendMessage("§c" + error.getMessage());
            return true;
        }
        previews.put(admin.getUniqueId(), new PreviewPlacement(config));
        admin.sendMessage(plugin.worldEditPreview().show(admin, config));
        BoardGeometry.BoardRegion region = config.geometry().boardRegion();
        admin.sendMessage("§a预览房间 §f" + config.id() + " §a完成，棋盘 §f" + config.boardSize() + "x" + config.boardSize()
            + "§a，选区 §7" + region.minimum() + " -> " + region.maximum() + "§a。");
        admin.sendMessage("§7确认创建: §f/gomoku admin confirm §7；取消: §f/gomoku admin cancel §7；也可以重新右键调整锚点。");
        return true;
    }

    public String confirm(Player admin) {
        UUID playerId = admin.getUniqueId();
        PreviewPlacement preview = previews.remove(playerId);
        if (preview == null) {
            return "§e还没有可确认的棋盘预览。先执行 /gomoku admin place <room> size <大小>，再右键地面。";
        }
        pendingRooms.remove(playerId);
        String saved = plugin.saveRoomConfig(preview.config());
        if (!saved.isBlank()) {
            return saved;
        }
        ArenaConfig config = preview.config();
        return "§a已创建房间 " + config.id() + "，棋盘大小 §f" + config.boardSize() + "x" + config.boardSize() + "§a。";
    }

    public String cancel(Player admin) {
        UUID playerId = admin.getUniqueId();
        boolean hadPending = pendingRooms.remove(playerId) != null;
        boolean hadPreview = previews.remove(playerId) != null;
        return hadPending || hadPreview ? "§7已取消五子棋房间创建预览。" : "§e当前没有待取消的五子棋房间创建。";
    }

    public void cancel(UUID playerId) {
        pendingRooms.remove(playerId);
        previews.remove(playerId);
    }

    private record PendingPlacement(String roomId, int boardSize, String templateId) {
    }

    private record PreviewPlacement(ArenaConfig config) {
    }
}
