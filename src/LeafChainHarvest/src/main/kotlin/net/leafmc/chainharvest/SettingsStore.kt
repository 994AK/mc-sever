package net.leafmc.chainharvest

import java.io.File
import java.util.UUID
import java.util.logging.Logger
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin

class SettingsStore(
    private val file: File,
) {
    constructor(plugin: Plugin) : this(File(plugin.dataFolder, "settings.yml"))

    fun load(config: FileConfiguration, logger: Logger): LeafChainSettings {
        val state = YamlConfiguration.loadConfiguration(file)
        val cropAll = materials(config, "crops.standard", MaterialCatalog.standardCrops, logger) +
            materials(config, "crops.vertical", MaterialCatalog.verticalCrops, logger) +
            materials(config, "crops.full-block", MaterialCatalog.fullBlockCrops, logger)

        val settings = LeafChainSettings(
            actions = FarmAction.entries.associateWith { action ->
                ActionSettings(
                    enabled = bool(state, "actions.${action.key}", config.getBoolean("actions.${action.key}.enabled", false)),
                    permission = config.getString("actions.${action.key}.permission", defaultPermission(action)) ?: defaultPermission(action),
                    radius = config.getInt("actions.${action.key}.radius", 0).coerceAtLeast(0),
                    maxTargets = config.getInt("actions.${action.key}.max-targets", 0).coerceAtLeast(0),
                )
            },
            crops = CropSettings(
                enabled = config.getBoolean("crops.enabled", true),
                disableWhenSneaking = config.getBoolean("crops.disable-when-sneaking", true),
                replantDelayTicks = config.getLong("crops.replant-delay-ticks", 1L).coerceAtLeast(0L),
                requireTool = config.getBoolean("crops.require-tool", false),
                maxCollectBlocks = config.getInt("crops.max-collect-blocks", 64).coerceAtLeast(1),
                tools = materials(config, "crops.tools", MaterialCatalog.hoes, logger),
                standardCrops = enabledCropSubset(
                    materials(config, "crops.standard", MaterialCatalog.standardCrops, logger),
                    state,
                ),
                verticalCrops = enabledCropSubset(
                    materials(config, "crops.vertical", MaterialCatalog.verticalCrops, logger),
                    state,
                ),
                fullBlockCrops = enabledCropSubset(
                    materials(config, "crops.full-block", MaterialCatalog.fullBlockCrops, logger),
                    state,
                ),
                allBlocks = cropAll,
                enabledBlocks = enabledMaterials(MaterialGroup.CROPS, cropAll, state),
            ),
            trees = chainGroup(config, state, logger, MaterialGroup.TREES, "trees", MaterialCatalog.treeBlocks, MaterialCatalog.axes),
            ores = chainGroup(config, state, logger, MaterialGroup.ORES, "ores", MaterialCatalog.ores, MaterialCatalog.pickaxes),
            protection = ProtectionSettings(
                requireResidenceForFarmActions = config.getBoolean("protection.require-residence-for-farm-actions", true),
                unsafeFallbackWithoutResidence = config.getBoolean("protection.unsafe-fallback-without-residence", false),
            ),
        )

        if (!file.exists()) {
            saveDefaults(settings)
        }
        return settings
    }

    fun setActionEnabled(action: FarmAction, enabled: Boolean, config: FileConfiguration, logger: Logger): LeafChainSettings {
        val state = YamlConfiguration.loadConfiguration(file)
        state.set("actions.${action.key}", enabled)
        save(state)
        return load(config, logger)
    }

    fun setMaterialEnabled(
        group: MaterialGroup,
        material: Material,
        enabled: Boolean,
        config: FileConfiguration,
        logger: Logger,
    ): LeafChainSettings {
        val state = YamlConfiguration.loadConfiguration(file)
        state.set("materials.${group.key}.${material.name}", enabled)
        save(state)
        return load(config, logger)
    }

    fun loadPlayerSettings(playerId: UUID): PlayerChainSettings {
        val state = YamlConfiguration.loadConfiguration(file)
        val root = "players.$playerId"
        return PlayerChainSettings(
            actions = FarmAction.entries.filterTo(linkedSetOf()) { action ->
                state.getBoolean("$root.actions.${action.key}", false)
            },
            groups = MaterialGroup.entries.filterTo(linkedSetOf()) { group ->
                state.getBoolean("$root.groups.${group.key}", false)
            },
        )
    }

    fun applyPlayerPreset(playerId: UUID, playerName: String, preset: ChainPreset): PlayerChainSettings {
        val state = YamlConfiguration.loadConfiguration(file)
        val root = "players.$playerId"
        state.set("$root.name", playerName)
        when (preset) {
            ChainPreset.FARM -> {
                setPlayerActions(state, root, setOf(FarmAction.COLLECT, FarmAction.SOW, FarmAction.TILL, FarmAction.FERTILIZE), true)
                setPlayerGroups(state, root, setOf(MaterialGroup.CROPS), true)
            }
            ChainPreset.CHAIN -> setPlayerGroups(state, root, setOf(MaterialGroup.TREES, MaterialGroup.ORES), true)
            ChainPreset.ALL -> {
                setPlayerActions(state, root, FarmAction.entries.toSet(), true)
                setPlayerGroups(state, root, MaterialGroup.entries.toSet(), true)
            }
            ChainPreset.OFF -> {
                setPlayerActions(state, root, FarmAction.entries.toSet(), false)
                setPlayerGroups(state, root, MaterialGroup.entries.toSet(), false)
            }
        }
        save(state)
        return loadPlayerSettings(playerId)
    }

    fun applyPreset(preset: ChainPreset, config: FileConfiguration, logger: Logger): LeafChainSettings {
        val state = YamlConfiguration.loadConfiguration(file)
        val current = load(config, logger)

        when (preset) {
            ChainPreset.FARM -> {
                setActions(state, setOf(FarmAction.COLLECT, FarmAction.SOW, FarmAction.TILL, FarmAction.FERTILIZE), true)
                setMaterials(state, MaterialGroup.CROPS, current.materialsFor(MaterialGroup.CROPS), true)
            }
            ChainPreset.CHAIN -> {
                setMaterials(state, MaterialGroup.TREES, current.materialsFor(MaterialGroup.TREES), true)
                setMaterials(state, MaterialGroup.ORES, current.materialsFor(MaterialGroup.ORES), true)
            }
            ChainPreset.ALL -> {
                setActions(state, FarmAction.entries.toSet(), true)
                MaterialGroup.entries.forEach { group ->
                    setMaterials(state, group, current.materialsFor(group), true)
                }
            }
            ChainPreset.OFF -> {
                setActions(state, FarmAction.entries.toSet(), false)
                MaterialGroup.entries.forEach { group ->
                    setMaterials(state, group, current.materialsFor(group), false)
                }
            }
        }
        save(state)
        return load(config, logger)
    }

    private fun chainGroup(
        config: FileConfiguration,
        state: YamlConfiguration,
        logger: Logger,
        group: MaterialGroup,
        root: String,
        defaultBlocks: Set<Material>,
        defaultTools: Set<Material>,
    ): ChainGroupSettings {
        val allBlocks = materials(config, "$root.blocks", defaultBlocks, logger)
        return ChainGroupSettings(
            enabled = config.getBoolean("$root.enabled", true),
            permission = config.getString("$root.permission", "leafchain.${group.key.dropLast(1)}") ?: "leafchain.${group.key.dropLast(1)}",
            disableWhenSneaking = config.getBoolean("$root.disable-when-sneaking", true),
            maxBlocks = config.getInt("$root.max-blocks", 64).coerceAtLeast(1),
            diagonals = config.getBoolean("$root.diagonals", false),
            minimumToolDurability = config.getInt("$root.minimum-tool-durability", 1).coerceAtLeast(0),
            tools = materials(config, "$root.tools", defaultTools, logger),
            blocks = enabledMaterials(group, allBlocks, state),
            allBlocks = allBlocks,
        )
    }

    private fun enabledCropSubset(materials: Set<Material>, state: YamlConfiguration): Set<Material> {
        return materials.filterTo(linkedSetOf()) { material ->
            bool(state, "materials.${MaterialGroup.CROPS.key}.${material.name}", false)
        }
    }

    private fun enabledMaterials(group: MaterialGroup, materials: Set<Material>, state: YamlConfiguration): Set<Material> {
        return materials.filterTo(linkedSetOf()) { material ->
            bool(state, "materials.${group.key}.${material.name}", false)
        }
    }

    private fun materials(
        config: FileConfiguration,
        path: String,
        defaults: Set<Material>,
        logger: Logger,
    ): Set<Material> {
        val rawValues = config.getStringList(path)
        if (rawValues.isEmpty()) {
            return defaults.toCollection(linkedSetOf())
        }
        val parsed = linkedSetOf<Material>()
        val unknown = mutableListOf<String>()
        rawValues.forEach { raw ->
            val material = MaterialCatalog.parseMaterial(raw)
            if (material == null) {
                unknown += raw
            } else {
                parsed += material
            }
        }
        if (unknown.isNotEmpty()) {
            logger.warning("Ignored unknown materials at $path: ${unknown.joinToString(", ")}")
        }
        return parsed
    }

    private fun saveDefaults(settings: LeafChainSettings) {
        val state = YamlConfiguration()
        settings.actions.forEach { (action, actionSettings) ->
            state.set("actions.${action.key}", actionSettings.enabled)
        }
        MaterialGroup.entries.forEach { group ->
            settings.materialsFor(group).forEach { material ->
                state.set("materials.${group.key}.${material.name}", settings.enabledMaterialsFor(group).contains(material))
            }
        }
        save(state)
    }

    private fun save(state: YamlConfiguration) {
        file.parentFile?.mkdirs()
        state.save(file)
    }

    private fun setActions(state: YamlConfiguration, actions: Set<FarmAction>, enabled: Boolean) {
        actions.forEach { action -> state.set("actions.${action.key}", enabled) }
    }

    private fun setMaterials(state: YamlConfiguration, group: MaterialGroup, materials: Set<Material>, enabled: Boolean) {
        materials.forEach { material -> state.set("materials.${group.key}.${material.name}", enabled) }
    }

    private fun setPlayerActions(state: YamlConfiguration, root: String, actions: Set<FarmAction>, enabled: Boolean) {
        actions.forEach { action -> state.set("$root.actions.${action.key}", enabled) }
    }

    private fun setPlayerGroups(state: YamlConfiguration, root: String, groups: Set<MaterialGroup>, enabled: Boolean) {
        groups.forEach { group -> state.set("$root.groups.${group.key}", enabled) }
    }

    private fun bool(config: YamlConfiguration, path: String, defaultValue: Boolean): Boolean {
        return if (config.contains(path)) config.getBoolean(path) else defaultValue
    }

    private fun defaultPermission(action: FarmAction): String = when (action) {
        FarmAction.COLLECT -> "leafchain.crop.collect"
        FarmAction.SOW -> "leafchain.crop.sow"
        FarmAction.TILL -> "leafchain.crop.till"
        FarmAction.FERTILIZE -> "leafchain.crop.fertilize"
    }
}

enum class ChainPreset(val key: String, val displayName: String) {
    FARM("farm", "农作物全套"),
    CHAIN("chain", "木头矿物连锁"),
    ALL("all", "全部功能"),
    OFF("off", "全部关闭");

    companion object {
        fun parse(raw: String?): ChainPreset? {
            return entries.firstOrNull { it.key == raw?.lowercase(java.util.Locale.ROOT) }
        }
    }
}
