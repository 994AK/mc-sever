package net.leafmc.chainharvest

import org.bukkit.Material

object ChainPlannerTest {
    @JvmStatic
    fun main(args: Array<String>) {
        plansAdjacentMatchingBlocksOnly()
        honorsMaxBlocks()
        skipsDisabledOrUnlistedStartBlocks()
    }

    private fun plansAdjacentMatchingBlocksOnly() {
        val start = BlockPoint(0, 64, 0)
        val blocks = mapOf(
            start to Material.DIAMOND_ORE,
            start.relative(1, 0, 0) to Material.DIAMOND_ORE,
            start.relative(2, 0, 0) to Material.DIAMOND_ORE,
            start.relative(0, 0, 1) to Material.STONE,
        )
        val result = ChainPlanner().plan(start, Material.DIAMOND_ORE, oreSettings(max = 64), blocks::get)
        check(result == listOf(start.relative(1, 0, 0), start.relative(2, 0, 0))) {
            "planner should follow only matching ore blocks"
        }
    }

    private fun honorsMaxBlocks() {
        val start = BlockPoint(0, 64, 0)
        val blocks = (0..5).associate { offset -> start.relative(offset, 0, 0) to Material.DIAMOND_ORE }
        val result = ChainPlanner().plan(start, Material.DIAMOND_ORE, oreSettings(max = 2), blocks::get)
        check(result.size == 2) { "planner should stop at max extra blocks" }
    }

    private fun skipsDisabledOrUnlistedStartBlocks() {
        val start = BlockPoint(0, 64, 0)
        val result = ChainPlanner().plan(start, Material.STONE, oreSettings(max = 64), mapOf(start to Material.STONE)::get)
        check(result.isEmpty()) { "stone should never chain mine" }
    }

    private fun oreSettings(max: Int) = ChainGroupSettings(
        enabled = true,
        permission = "leafchain.ore",
        disableWhenSneaking = true,
        maxBlocks = max,
        diagonals = false,
        minimumToolDurability = 1,
        tools = MaterialCatalog.pickaxes,
        blocks = setOf(Material.DIAMOND_ORE),
        allBlocks = setOf(Material.DIAMOND_ORE),
    )
}
