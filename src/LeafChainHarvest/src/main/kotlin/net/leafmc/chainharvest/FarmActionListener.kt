package net.leafmc.chainharvest

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class FarmActionListener(private val plugin: LeafChainHarvestPlugin) : Listener {
    private val planner = FarmPlanner()

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) {
            return
        }
        val clicked = event.clickedBlock ?: return
        val player = event.player

        if (tryCollect(player, clicked)) {
            event.isCancelled = true
            return
        }
        if (trySow(player, clicked)) {
            event.isCancelled = true
            return
        }
        if (tryTill(player, clicked)) {
            event.isCancelled = true
            return
        }
        if (tryFertilize(player, clicked)) {
            event.isCancelled = true
        }
    }

    private fun tryCollect(player: Player, block: Block): Boolean {
        val settings = plugin.settings()
        val action = settings.action(FarmAction.COLLECT)
        val crops = settings.crops
        if (!crops.enabled ||
            !action.enabled ||
            !plugin.playerActionEnabled(player, FarmAction.COLLECT) ||
            !plugin.playerGroupEnabled(player, MaterialGroup.CROPS) ||
            !player.hasPermission(action.permission)
        ) {
            return false
        }
        if (crops.disableWhenSneaking && player.isSneaking) {
            return false
        }
        if (crops.requireTool && !crops.tools.contains(player.inventory.itemInMainHand.type)) {
            return false
        }
        return collectStandardCrop(player, block, crops) ||
            collectVerticalCrop(player, block, crops) ||
            collectFullBlockCrop(player, block, crops)
    }

    private fun collectStandardCrop(player: Player, block: Block, crops: CropSettings): Boolean {
        if (!crops.standardCrops.contains(block.type)) {
            return false
        }
        val ageable = block.blockData as? Ageable ?: return false
        if (ageable.age != ageable.maximumAge) {
            return false
        }
        if (!canMutate(player, block, ProtectionAction.REPLANT)) {
            return false
        }
        val original = block.blockData.clone()
        val broken = plugin.whileBreakingChain { player.breakBlock(block) }
        if (!broken) {
            return false
        }
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val next = original.clone()
            if (next is Ageable) {
                next.age = 0
            }
            if (plugin.protectionGate().canMutate(player, block, ProtectionAction.REPLANT) != ProtectionDecision.ALLOW) {
                return@Runnable
            }
            if (block.type.isAir && block.canPlace(next)) {
                block.setBlockData(next, true)
            }
        }, crops.replantDelayTicks)
        return true
    }

    private fun collectVerticalCrop(player: Player, block: Block, crops: CropSettings): Boolean {
        if (!crops.verticalCrops.contains(block.type)) {
            return false
        }
        val world = block.world
        val targets = planner.verticalHarvestTargets(
            block.point(),
            block.type,
            BlockTypeLookup { point -> world.getBlockAt(point.x, point.y, point.z).type },
            crops.maxCollectBlocks,
        )
        var harvested = false
        for (point in targets.asReversed()) {
            val target = world.getBlockAt(point.x, point.y, point.z)
            if (target.type == block.type) {
                harvested = plugin.whileBreakingChain { player.breakBlock(target) } || harvested
            }
        }
        return harvested
    }

    private fun collectFullBlockCrop(player: Player, block: Block, crops: CropSettings): Boolean {
        if (!crops.fullBlockCrops.contains(block.type)) {
            return false
        }
        return plugin.whileBreakingChain { player.breakBlock(block) }
    }

    private fun trySow(player: Player, clicked: Block): Boolean {
        val settings = plugin.settings()
        val action = settings.action(FarmAction.SOW)
        if (!settings.crops.enabled ||
            !action.enabled ||
            !plugin.playerActionEnabled(player, FarmAction.SOW) ||
            !plugin.playerGroupEnabled(player, MaterialGroup.CROPS) ||
            !player.hasPermission(action.permission)
        ) {
            return false
        }
        val hand = player.inventory.itemInMainHand
        val rule = MaterialCatalog.seedRuleFor(hand.type) ?: return false
        if (!settings.crops.enabledBlocks.contains(rule.cropBlock)) {
            return false
        }
        val available = availableActions(player, hand)
        if (available <= 0) {
            return false
        }
        val world = clicked.world
        val targets = planner.sowTargets(
            clicked.point(),
            action.radius,
            minOf(action.maxTargets, available),
            rule,
            BlockTypeLookup { point -> world.getBlockAt(point.x, point.y, point.z).type },
        )
        var planted = 0
        for (target in targets) {
            val block = world.getBlockAt(target.target.x, target.target.y, target.target.z)
            if (!canMutate(player, block, ProtectionAction.SOW)) {
                continue
            }
            val data = Bukkit.createBlockData(rule.cropBlock)
            if (data is Ageable) {
                data.age = 0
            }
            if (block.type == Material.AIR && block.canPlace(data)) {
                block.setBlockData(data, true)
                planted++
            }
        }
        if (planted <= 0) {
            return false
        }
        consume(player, hand, planted)
        player.sendMessage(plugin.message("sowed", mapOf("count" to planted.toString())))
        return true
    }

    private fun tryFertilize(player: Player, clicked: Block): Boolean {
        val settings = plugin.settings()
        val action = settings.action(FarmAction.FERTILIZE)
        if (!settings.crops.enabled ||
            !action.enabled ||
            !plugin.playerActionEnabled(player, FarmAction.FERTILIZE) ||
            !plugin.playerGroupEnabled(player, MaterialGroup.CROPS) ||
            !player.hasPermission(action.permission)
        ) {
            return false
        }
        val hand = player.inventory.itemInMainHand
        if (hand.type != Material.BONE_MEAL) {
            return false
        }
        val available = availableActions(player, hand)
        if (available <= 0) {
            return false
        }
        val world = clicked.world
        val targets = planner.fertilizeTargets(
            clicked.point(),
            action.radius,
            minOf(action.maxTargets, available),
            settings.crops.enabledBlocks,
            BlockTypeLookup { point -> world.getBlockAt(point.x, point.y, point.z).type },
        )
        var fertilized = 0
        for (point in targets) {
            val block = world.getBlockAt(point.x, point.y, point.z)
            if (!canMutate(player, block, ProtectionAction.FERTILIZE) || isFullyGrown(block)) {
                continue
            }
            if (block.applyBoneMeal(BlockFace.UP)) {
                fertilized++
            }
        }
        if (fertilized <= 0) {
            return false
        }
        consume(player, hand, fertilized)
        player.sendMessage(plugin.message("fertilized", mapOf("count" to fertilized.toString())))
        return true
    }

    private fun tryTill(player: Player, clicked: Block): Boolean {
        val settings = plugin.settings()
        val action = settings.action(FarmAction.TILL)
        val crops = settings.crops
        if (!crops.enabled ||
            !action.enabled ||
            !plugin.playerActionEnabled(player, FarmAction.TILL) ||
            !plugin.playerGroupEnabled(player, MaterialGroup.CROPS) ||
            !player.hasPermission(action.permission)
        ) {
            return false
        }
        if (crops.disableWhenSneaking && player.isSneaking) {
            return false
        }
        val hand = player.inventory.itemInMainHand
        if (!crops.tools.contains(hand.type)) {
            return false
        }
        val world = clicked.world
        val targets = planner.tillTargets(
            clicked.point(),
            action.radius,
            action.maxTargets,
            BlockTypeLookup { point -> world.getBlockAt(point.x, point.y, point.z).type },
        )
        var tilled = 0
        for (point in targets) {
            val block = world.getBlockAt(point.x, point.y, point.z)
            if (!canMutate(player, block, ProtectionAction.TILL)) {
                continue
            }
            if (MaterialCatalog.tillableBlocks.contains(block.type)) {
                block.type = Material.FARMLAND
                tilled++
            }
        }
        if (tilled <= 0) {
            return false
        }
        player.sendMessage(plugin.message("tilled", mapOf("count" to tilled.toString())))
        return true
    }

    private fun canMutate(player: Player, block: Block, action: ProtectionAction): Boolean {
        val decision = plugin.protectionGate().canMutate(player, block, action)
        if (decision != ProtectionDecision.ALLOW) {
            player.sendMessage(plugin.message("protected"))
            return false
        }
        return true
    }

    private fun isFullyGrown(block: Block): Boolean {
        val ageable = block.blockData as? Ageable ?: return false
        return ageable.age >= ageable.maximumAge
    }

    private fun availableActions(player: Player, hand: ItemStack): Int {
        if (player.gameMode == GameMode.CREATIVE || player.hasPermission("leafchain.bypass-consume")) {
            return Int.MAX_VALUE
        }
        return hand.amount
    }

    private fun consume(player: Player, hand: ItemStack, amount: Int) {
        if (player.gameMode == GameMode.CREATIVE || player.hasPermission("leafchain.bypass-consume")) {
            return
        }
        val next = hand.amount - amount
        if (next <= 0) {
            player.inventory.setItemInMainHand(ItemStack(Material.AIR))
        } else {
            hand.amount = next
        }
    }
}
