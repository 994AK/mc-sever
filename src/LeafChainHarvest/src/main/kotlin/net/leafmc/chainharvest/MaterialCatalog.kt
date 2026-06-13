package net.leafmc.chainharvest

import java.util.Locale
import org.bukkit.Material

object MaterialCatalog {
    val hoes: Set<Material> = setOf(
        Material.WOODEN_HOE,
        Material.STONE_HOE,
        Material.IRON_HOE,
        Material.GOLDEN_HOE,
        Material.DIAMOND_HOE,
        Material.NETHERITE_HOE,
    )

    val axes: Set<Material> = setOf(
        Material.WOODEN_AXE,
        Material.STONE_AXE,
        Material.IRON_AXE,
        Material.GOLDEN_AXE,
        Material.DIAMOND_AXE,
        Material.NETHERITE_AXE,
    )

    val pickaxes: Set<Material> = setOf(
        Material.WOODEN_PICKAXE,
        Material.STONE_PICKAXE,
        Material.IRON_PICKAXE,
        Material.GOLDEN_PICKAXE,
        Material.DIAMOND_PICKAXE,
        Material.NETHERITE_PICKAXE,
    )

    val standardCrops: Set<Material> = setOf(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS,
        Material.NETHER_WART,
        Material.COCOA,
    )

    val verticalCrops: Set<Material> = setOf(
        Material.SUGAR_CANE,
        Material.BAMBOO,
        Material.CACTUS,
    )

    val fullBlockCrops: Set<Material> = setOf(
        Material.MELON,
        Material.PUMPKIN,
    )

    val treeBlocks: Set<Material> = setOf(
        Material.OAK_LOG,
        Material.SPRUCE_LOG,
        Material.BIRCH_LOG,
        Material.JUNGLE_LOG,
        Material.ACACIA_LOG,
        Material.DARK_OAK_LOG,
        Material.MANGROVE_LOG,
        Material.CHERRY_LOG,
        Material.PALE_OAK_LOG,
        Material.OAK_WOOD,
        Material.SPRUCE_WOOD,
        Material.BIRCH_WOOD,
        Material.JUNGLE_WOOD,
        Material.ACACIA_WOOD,
        Material.DARK_OAK_WOOD,
        Material.MANGROVE_WOOD,
        Material.CHERRY_WOOD,
        Material.PALE_OAK_WOOD,
        Material.STRIPPED_OAK_LOG,
        Material.STRIPPED_SPRUCE_LOG,
        Material.STRIPPED_BIRCH_LOG,
        Material.STRIPPED_JUNGLE_LOG,
        Material.STRIPPED_ACACIA_LOG,
        Material.STRIPPED_DARK_OAK_LOG,
        Material.STRIPPED_MANGROVE_LOG,
        Material.STRIPPED_CHERRY_LOG,
        Material.STRIPPED_PALE_OAK_LOG,
        Material.STRIPPED_OAK_WOOD,
        Material.STRIPPED_SPRUCE_WOOD,
        Material.STRIPPED_BIRCH_WOOD,
        Material.STRIPPED_JUNGLE_WOOD,
        Material.STRIPPED_ACACIA_WOOD,
        Material.STRIPPED_DARK_OAK_WOOD,
        Material.STRIPPED_MANGROVE_WOOD,
        Material.STRIPPED_CHERRY_WOOD,
        Material.STRIPPED_PALE_OAK_WOOD,
        Material.CRIMSON_STEM,
        Material.WARPED_STEM,
        Material.CRIMSON_HYPHAE,
        Material.WARPED_HYPHAE,
        Material.STRIPPED_CRIMSON_STEM,
        Material.STRIPPED_WARPED_STEM,
        Material.STRIPPED_CRIMSON_HYPHAE,
        Material.STRIPPED_WARPED_HYPHAE,
    )

    val ores: Set<Material> = setOf(
        Material.COAL_ORE,
        Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.COPPER_ORE,
        Material.DEEPSLATE_COPPER_ORE,
        Material.GOLD_ORE,
        Material.DEEPSLATE_GOLD_ORE,
        Material.REDSTONE_ORE,
        Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.DIAMOND_ORE,
        Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE,
        Material.DEEPSLATE_EMERALD_ORE,
        Material.NETHER_QUARTZ_ORE,
        Material.NETHER_GOLD_ORE,
        Material.ANCIENT_DEBRIS,
    )

    val seedRules: List<SeedRule> = listOf(
        SeedRule(Material.WHEAT_SEEDS, Material.WHEAT, setOf(Material.FARMLAND)),
        SeedRule(Material.CARROT, Material.CARROTS, setOf(Material.FARMLAND)),
        SeedRule(Material.POTATO, Material.POTATOES, setOf(Material.FARMLAND)),
        SeedRule(Material.BEETROOT_SEEDS, Material.BEETROOTS, setOf(Material.FARMLAND)),
        SeedRule(Material.NETHER_WART, Material.NETHER_WART, setOf(Material.SOUL_SAND)),
    )

    fun parseMaterial(raw: String?): Material? {
        if (raw.isNullOrBlank()) {
            return null
        }
        val trimmed = raw.trim()
        Material.matchMaterial(trimmed)?.let { return it }
        val key = trimmed.substringAfter(':', trimmed).uppercase(Locale.ROOT)
        return Material.matchMaterial(key)
    }

    fun seedRuleFor(item: Material): SeedRule? = seedRules.firstOrNull { it.seedItem == item }
}

data class SeedRule(
    val seedItem: Material,
    val cropBlock: Material,
    val substrates: Set<Material>,
)
