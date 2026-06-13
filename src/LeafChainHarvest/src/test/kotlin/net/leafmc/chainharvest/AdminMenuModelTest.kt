package net.leafmc.chainharvest

import org.bukkit.Material

object AdminMenuModelTest {
    @JvmStatic
    fun main(args: Array<String>) {
        paginatesMaterialLists()
        usesItemIconsForMenuMaterials()
    }

    private fun paginatesMaterialLists() {
        val materials = (1..50).map { Material.DIAMOND_ORE }
        check(AdminMenuModel.page(materials, 0).size == AdminMenuModel.pageSize) { "first page should be full" }
        check(AdminMenuModel.page(materials, 1).size == 5) { "second page should contain remaining entries" }
        check(AdminMenuModel.maxPage(materials) == 1) { "max page should be zero based" }
    }

    private fun usesItemIconsForMenuMaterials() {
        val catalog = MaterialCatalog.standardCrops +
            MaterialCatalog.verticalCrops +
            MaterialCatalog.fullBlockCrops +
            MaterialCatalog.treeBlocks +
            MaterialCatalog.ores

        catalog.forEach { material ->
            val icon = AdminMenuModel.iconFor(material)
            check(icon != Material.AIR) { "$material should not render as air" }
        }
        check(AdminMenuModel.iconFor(Material.BEETROOTS) == Material.BEETROOT) { "beetroots block should render as beetroot item" }
        check(AdminMenuModel.iconFor(Material.CARROTS) == Material.CARROT) { "carrots block should render as carrot item" }
        check(AdminMenuModel.iconFor(Material.COCOA) == Material.COCOA_BEANS) { "cocoa block should render as cocoa beans" }
    }
}
