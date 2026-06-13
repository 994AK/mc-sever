package net.leafmc.chainharvest

import org.bukkit.Material

object FarmPlannerTest {
    @JvmStatic
    fun main(args: Array<String>) {
        verticalHarvestLeavesBaseBlock()
        sowTargetsRequireSubstrateAndAir()
        tillTargetsRequireTillableBlockAndAirAbove()
        fertilizeTargetsStayWithinEnabledCrops()
    }

    private fun verticalHarvestLeavesBaseBlock() {
        val base = BlockPoint(0, 64, 0)
        val blocks = mapOf(
            base to Material.SUGAR_CANE,
            base.relative(0, 1, 0) to Material.SUGAR_CANE,
            base.relative(0, 2, 0) to Material.SUGAR_CANE,
        )
        val result = FarmPlanner().verticalHarvestTargets(base.relative(0, 1, 0), Material.SUGAR_CANE, blocks::get, 64)
        check(result == listOf(base.relative(0, 1, 0), base.relative(0, 2, 0))) {
            "vertical harvest should return upper stack only"
        }
    }

    private fun sowTargetsRequireSubstrateAndAir() {
        val center = BlockPoint(0, 64, 0)
        val rule = MaterialCatalog.seedRuleFor(Material.WHEAT_SEEDS) ?: error("missing wheat rule")
        val blocks = mapOf(
            center to Material.FARMLAND,
            center.relative(0, 1, 0) to Material.AIR,
            center.relative(1, 0, 0) to Material.DIRT,
            center.relative(1, 1, 0) to Material.AIR,
        )
        val result = FarmPlanner().sowTargets(center, 1, 10, rule, blocks::get)
        check(result == listOf(SowTarget(center, center.relative(0, 1, 0)))) {
            "sow planner should require valid substrate and empty target"
        }
    }

    private fun fertilizeTargetsStayWithinEnabledCrops() {
        val center = BlockPoint(0, 64, 0)
        val blocks = mapOf(
            center to Material.WHEAT,
            center.relative(1, 0, 0) to Material.OAK_LOG,
            center.relative(0, 0, 1) to Material.CARROTS,
        )
        val result = FarmPlanner().fertilizeTargets(center, 1, 10, setOf(Material.WHEAT, Material.CARROTS), blocks::get)
        check(result.contains(center)) { "wheat can be fertilized" }
        check(result.contains(center.relative(0, 0, 1))) { "carrots can be fertilized" }
        check(!result.contains(center.relative(1, 0, 0))) { "wood is not a crop target" }
    }

    private fun tillTargetsRequireTillableBlockAndAirAbove() {
        val center = BlockPoint(0, 64, 0)
        val blocked = center.relative(1, 0, 0)
        val stone = center.relative(0, 0, 1)
        val blocks = mapOf(
            center to Material.GRASS_BLOCK,
            center.relative(0, 1, 0) to Material.AIR,
            blocked to Material.DIRT,
            blocked.relative(0, 1, 0) to Material.OAK_LOG,
            stone to Material.STONE,
            stone.relative(0, 1, 0) to Material.AIR,
        )
        val result = FarmPlanner().tillTargets(center, 1, 10, blocks::get)
        check(result == listOf(center)) { "till planner should require tillable block with air above" }
    }
}
