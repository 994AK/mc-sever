package net.leafmc.chainharvest

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

class ChainBreakListener(private val plugin: LeafChainHarvestPlugin) : Listener {
    private val planner = ChainPlanner()

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (plugin.isBreakingChain()) {
            return
        }
        val player = event.player
        val start = event.block
        val group = chainSettings(player, start.type) ?: return
        if (group.disableWhenSneaking && player.isSneaking) {
            return
        }
        val tool = player.inventory.itemInMainHand
        if (!group.tools.contains(tool.type) || !hasEnoughDurability(tool, group.minimumToolDurability)) {
            return
        }

        val world = start.world
        val points = planner.plan(
            start = start.point(),
            type = start.type,
            settings = group,
            lookup = BlockTypeLookup { point -> world.getBlockAt(point.x, point.y, point.z).type },
            bypassLimit = player.hasPermission("leafchain.bypass-limit"),
        )

        for (point in points) {
            val block = world.getBlockAt(point.x, point.y, point.z)
            if (block.type != start.type || !hasEnoughDurability(player.inventory.itemInMainHand, group.minimumToolDurability)) {
                continue
            }
            plugin.whileBreakingChain {
                player.breakBlock(block)
            }
        }
    }

    private fun chainSettings(player: Player, type: Material): ChainGroupSettings? {
        val settings = plugin.settings()
        if (settings.trees.enabled && settings.trees.blocks.contains(type) && player.hasPermission(settings.trees.permission)) {
            return settings.trees
        }
        if (settings.ores.enabled && settings.ores.blocks.contains(type) && player.hasPermission(settings.ores.permission)) {
            return settings.ores
        }
        return null
    }

    private fun hasEnoughDurability(item: ItemStack, minimumRemaining: Int): Boolean {
        if (item.type.isAir) {
            return false
        }
        val maxDurability = item.type.maxDurability.toInt()
        if (maxDurability <= 0) {
            return true
        }
        val damageable = item.itemMeta as? Damageable ?: return true
        return maxDurability - damageable.damage > minimumRemaining
    }
}

internal fun Block.point(): BlockPoint = BlockPoint(x, y, z)
