package net.leafmc.gomoku;

import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

public final class BoardListener implements Listener {
    private final LeafGomokuPlugin plugin;

    public BoardListener(LeafGomokuPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (plugin.placementTool().handleRightClick(event.getPlayer(), event.getClickedBlock(), event.getBlockFace())) {
            event.setCancelled(true);
            return;
        }
        Optional<RoomRegistry.MappedCell> mapped = plugin.rooms().mapBoardBlock(event.getClickedBlock());
        if (mapped.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (!event.getPlayer().hasPermission(GomokuPermission.PLAY.node())) {
            event.getPlayer().sendMessage("§c你没有五子棋落子权限。");
            return;
        }
        plugin.handleMove(mapped.get().room(), event.getPlayer(), mapped.get().cell());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (isProtected(event.getBlock()) && !event.getPlayer().hasPermission(GomokuPermission.ADMIN_BYPASS.node())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c这里是五子棋房间区域，不能直接破坏。");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (isProtected(event.getBlockPlaced()) && !event.getPlayer().hasPermission(GomokuPermission.ADMIN_BYPASS.node())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c这里是五子棋房间区域，不能直接放置。");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (GomokuEntityTags.isGomokuEntity(event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.rooms().participantRoom(player.getUniqueId()).isPresent() || plugin.rooms().spectatorRoom(player.getUniqueId()).isPresent()) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (GomokuEntityTags.isGomokuEntity(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.markDisconnected(event.getPlayer());
        plugin.placementTool().cancel(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.markOnline(event.getPlayer());
    }

    private boolean isProtected(Block block) {
        return plugin.rooms().isProtected(block);
    }
}
