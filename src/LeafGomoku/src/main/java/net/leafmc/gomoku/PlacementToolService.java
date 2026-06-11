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
    private final Map<UUID, String> pendingRooms = new ConcurrentHashMap<>();

    public PlacementToolService(LeafGomokuPlugin plugin) {
        this.plugin = plugin;
    }

    public String begin(Player admin, String roomId) {
        String normalized = ArenaConfig.normalizeRoomId(roomId);
        if (!ArenaConfig.isValidRoomId(normalized)) {
            return "§c房间 id 只能使用小写字母、数字、下划线或短横线，最长 32 位。";
        }
        if (plugin.rooms().room(normalized).isPresent()) {
            return "§c房间已存在: " + normalized;
        }
        pendingRooms.put(admin.getUniqueId(), normalized);
        return "§a已准备放置五子棋房间 " + normalized + "。右键目标地面，系统会按你当前面朝方向生成。";
    }

    public boolean handleRightClick(Player admin, Block clickedBlock, BlockFace face) {
        String roomId = pendingRooms.remove(admin.getUniqueId());
        if (roomId == null) {
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
        admin.sendMessage(plugin.createRoomAt(admin, roomId, anchor));
        return true;
    }

    public void cancel(UUID playerId) {
        pendingRooms.remove(playerId);
    }
}
