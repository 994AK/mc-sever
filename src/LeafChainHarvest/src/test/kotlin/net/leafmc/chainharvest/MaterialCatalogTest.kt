package net.leafmc.chainharvest

import org.bukkit.Material

object MaterialCatalogTest {
    @JvmStatic
    fun main(args: Array<String>) {
        parsesMaterialNames()
        keepsDefaultOresScoped()
        keepsDefaultTreesScoped()
        mapsSeedsToCropBlocks()
    }

    private fun parsesMaterialNames() {
        check(MaterialCatalog.parseMaterial("diamond_ore") == Material.DIAMOND_ORE) { "lowercase material parses" }
        check(MaterialCatalog.parseMaterial("minecraft:oak_log") == Material.OAK_LOG) { "namespaced material parses" }
        check(MaterialCatalog.parseMaterial("not_a_real_block") == null) { "unknown material rejected" }
    }

    private fun keepsDefaultOresScoped() {
        check(MaterialCatalog.ores.contains(Material.DIAMOND_ORE)) { "diamond ore is enabled by default" }
        check(MaterialCatalog.ores.contains(Material.ANCIENT_DEBRIS)) { "ancient debris is enabled by default" }
        check(!MaterialCatalog.ores.contains(Material.STONE)) { "stone is not an ore" }
        check(!MaterialCatalog.ores.contains(Material.DEEPSLATE)) { "deepslate is not an ore" }
        check(!MaterialCatalog.ores.contains(Material.NETHERRACK)) { "netherrack is not an ore" }
    }

    private fun keepsDefaultTreesScoped() {
        check(MaterialCatalog.treeBlocks.contains(Material.OAK_LOG)) { "oak log is a tree block" }
        check(!MaterialCatalog.treeBlocks.contains(Material.OAK_PLANKS)) { "planks are not tree blocks" }
        check(!MaterialCatalog.treeBlocks.contains(Material.OAK_LEAVES)) { "leaves are not tree blocks" }
    }

    private fun mapsSeedsToCropBlocks() {
        check(MaterialCatalog.seedRuleFor(Material.WHEAT_SEEDS)?.cropBlock == Material.WHEAT) { "wheat seeds plant wheat" }
        check(MaterialCatalog.seedRuleFor(Material.BONE_MEAL) == null) { "bone meal is not a seed" }
    }
}
