package net.leafmc.soulbind;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.block.Container;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public final class SoulbindListener implements Listener {
    private static final int OUTSIDE_SLOT = -999;
    private final LeafSoulbindPlugin plugin;

    public SoulbindListener(LeafSoulbindPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (!plugin.rule("block-player-drop")) {
            return;
        }
        Player player = event.getPlayer();
        if (hasBypass(player)) {
            return;
        }
        ItemStack item = event.getItemDrop().getItemStack();
        if (!plugin.soulbind().containsProtectedItemDeep(item)) {
            return;
        }
        event.setCancelled(true);
        plugin.send(player, "messages.drop-blocked", Map.of());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.rule("block-inventory-drop")) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || hasBypass(player)) {
            return;
        }
        ItemStack candidate = switch (event.getAction()) {
            case DROP_ALL_CURSOR, DROP_ONE_CURSOR -> event.getCursor();
            case DROP_ALL_SLOT, DROP_ONE_SLOT -> event.getCurrentItem();
            default -> event.getRawSlot() == OUTSIDE_SLOT ? event.getCursor() : null;
        };
        if (!plugin.soulbind().containsProtectedItemDeep(candidate)) {
            return;
        }
        event.setCancelled(true);
        plugin.send(player, "messages.drop-blocked", Map.of());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (!plugin.rule("block-dispenser-dropper")) {
            return;
        }
        if (!plugin.soulbind().containsProtectedItemDeep(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        plugin.getLogger().info(org.bukkit.ChatColor.stripColor(plugin.text("messages.dispense-blocked", Map.of())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerBreak(BlockBreakEvent event) {
        if (!plugin.rule("block-container-break-when-protected-inside")) {
            return;
        }
        Player player = event.getPlayer();
        if (hasBypass(player)) {
            return;
        }
        BlockState state = event.getBlock().getState();
        if (!(state instanceof Container container)) {
            return;
        }
        if (!plugin.soulbind().containsProtectedItemDeep(container.getInventory().getContents())) {
            return;
        }
        event.setCancelled(true);
        plugin.send(player, "messages.container-break-blocked", Map.of());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.rule("prevent-non-owner-pickup")) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || hasBypass(player)) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        if (!plugin.soulbind().isBound(item)) {
            return;
        }
        if (plugin.soulbind().owner(item).filter(player.getUniqueId()::equals).isPresent()) {
            return;
        }
        event.setCancelled(true);
        plugin.send(player, "messages.pickup-denied", Map.of("owner", plugin.soulbind().ownerName(item)));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.rule("block-death-drop") || event.getKeepInventory()) {
            return;
        }
        List<ItemStack> protectedItems = new ArrayList<>();
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (!plugin.soulbind().containsProtectedItemDeep(item)) {
                continue;
            }
            protectedItems.add(item.clone());
            iterator.remove();
        }
        if (protectedItems.isEmpty()) {
            return;
        }
        plugin.pendingReturns().add(event.getEntity().getUniqueId(), protectedItems);
        plugin.savePendingReturns();
        plugin.send(event.getEntity(), "messages.death-kept", Map.of("count", String.valueOf(SoulbindService.countItems(protectedItems))));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.rule("return-on-respawn")) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> recoverIfPossible(event.getPlayer(), false), 2L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.rule("return-on-join")) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> recoverIfPossible(event.getPlayer(), true), 20L);
    }

    private void recoverIfPossible(Player player, boolean quietWhenEmpty) {
        LeafSoulbindPlugin.ReturnResult result = plugin.returnPendingItems(player);
        if (result.returnedItems() <= 0 && result.remainingItems() <= 0) {
            if (!quietWhenEmpty) {
                plugin.send(player, "messages.recover-empty", Map.of());
            }
            return;
        }
        if (result.remainingItems() > 0) {
            plugin.send(player, "messages.recover-partial", Map.of(
                "returned", String.valueOf(result.returnedItems()),
                "remaining", String.valueOf(result.remainingItems())
            ));
            return;
        }
        plugin.send(player, "messages.recovered", Map.of("count", String.valueOf(result.returnedItems())));
    }

    private boolean hasBypass(Player player) {
        return plugin.rule("allow-permission-bypass") && player.hasPermission("leafsoulbind.bypass");
    }
}
