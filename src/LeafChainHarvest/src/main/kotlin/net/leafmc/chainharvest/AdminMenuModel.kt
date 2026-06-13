package net.leafmc.chainharvest

import org.bukkit.Material

object AdminMenuModel {
    const val pageSize = 45

    fun iconFor(material: Material): Material {
        val icon = when (material) {
            Material.CARROTS -> Material.CARROT
            Material.POTATOES -> Material.POTATO
            Material.BEETROOTS -> Material.BEETROOT
            Material.COCOA -> Material.COCOA_BEANS
            else -> material
        }
        return if (icon == Material.AIR) Material.PAPER else icon
    }

    fun actionIcon(action: FarmAction): Material = when (action) {
        FarmAction.COLLECT -> Material.GOLDEN_HOE
        FarmAction.SOW -> Material.WHEAT_SEEDS
        FarmAction.FERTILIZE -> Material.BONE_MEAL
    }

    fun groupIcon(group: MaterialGroup): Material = when (group) {
        MaterialGroup.CROPS -> Material.HAY_BLOCK
        MaterialGroup.TREES -> Material.CHERRY_LOG
        MaterialGroup.ORES -> Material.DIAMOND_ORE
    }

    fun page(materials: Collection<Material>, page: Int): List<Material> {
        val sorted = materials.sortedBy { it.name }
        val safePage = page.coerceAtLeast(0)
        val start = safePage * pageSize
        if (start >= sorted.size) {
            return emptyList()
        }
        return sorted.subList(start, minOf(sorted.size, start + pageSize))
    }

    fun maxPage(materials: Collection<Material>): Int {
        if (materials.isEmpty()) {
            return 0
        }
        return (materials.size - 1) / pageSize
    }
}
