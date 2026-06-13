package net.leafmc.chainharvest

import org.bukkit.Material

enum class FarmAction(val key: String, val displayName: String) {
    COLLECT("collect", "收集"),
    SOW("sow", "播种"),
    TILL("till", "耕地"),
    FERTILIZE("fertilize", "施肥"),
}

enum class MaterialGroup(val key: String, val displayName: String) {
    CROPS("crops", "农作物"),
    TREES("trees", "木头"),
    ORES("ores", "矿物"),
}

enum class ProtectionAction {
    REPLANT,
    SOW,
    TILL,
    FERTILIZE,
}

data class LeafChainSettings(
    val actions: Map<FarmAction, ActionSettings>,
    val crops: CropSettings,
    val trees: ChainGroupSettings,
    val ores: ChainGroupSettings,
    val protection: ProtectionSettings,
) {
    fun action(action: FarmAction): ActionSettings = actions[action] ?: ActionSettings(false, "", 0, 0)

    fun materialsFor(group: MaterialGroup): Set<Material> = when (group) {
        MaterialGroup.CROPS -> crops.allBlocks
        MaterialGroup.TREES -> trees.allBlocks
        MaterialGroup.ORES -> ores.allBlocks
    }

    fun enabledMaterialsFor(group: MaterialGroup): Set<Material> = when (group) {
        MaterialGroup.CROPS -> crops.enabledBlocks
        MaterialGroup.TREES -> trees.blocks
        MaterialGroup.ORES -> ores.blocks
    }
}

data class ActionSettings(
    val enabled: Boolean,
    val permission: String,
    val radius: Int,
    val maxTargets: Int,
)

data class PlayerChainSettings(
    val actions: Set<FarmAction>,
    val groups: Set<MaterialGroup>,
) {
    fun actionEnabled(action: FarmAction): Boolean = actions.contains(action)

    fun groupEnabled(group: MaterialGroup): Boolean = groups.contains(group)
}

data class CropSettings(
    val enabled: Boolean,
    val disableWhenSneaking: Boolean,
    val replantDelayTicks: Long,
    val requireTool: Boolean,
    val maxCollectBlocks: Int,
    val tools: Set<Material>,
    val standardCrops: Set<Material>,
    val verticalCrops: Set<Material>,
    val fullBlockCrops: Set<Material>,
    val allBlocks: Set<Material>,
    val enabledBlocks: Set<Material>,
)

data class ChainGroupSettings(
    val enabled: Boolean,
    val permission: String,
    val disableWhenSneaking: Boolean,
    val maxBlocks: Int,
    val diagonals: Boolean,
    val minimumToolDurability: Int,
    val tools: Set<Material>,
    val blocks: Set<Material>,
    val allBlocks: Set<Material>,
)

data class ProtectionSettings(
    val requireResidenceForFarmActions: Boolean,
    val unsafeFallbackWithoutResidence: Boolean,
)
