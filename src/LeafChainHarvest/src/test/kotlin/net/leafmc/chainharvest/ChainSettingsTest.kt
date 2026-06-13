package net.leafmc.chainharvest

import java.nio.file.Files
import java.util.UUID
import java.util.logging.Logger
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration

object ChainSettingsTest {
    @JvmStatic
    fun main(args: Array<String>) {
        createsSettingsFileFromDefaults()
        persistsActionAndMaterialToggles()
        appliesFarmAndOffPresets()
        keepsPlayerPresetsIndependent()
    }

    private fun createsSettingsFileFromDefaults() {
        val file = tempSettingsFile()
        val store = SettingsStore(file)
        val settings = store.load(YamlConfiguration(), Logger.getLogger("test"))

        check(file.exists()) { "settings.yml is seeded on first load" }
        check(!settings.action(FarmAction.COLLECT).enabled) { "collect defaults to disabled" }
        check(settings.ores.blocks.isEmpty()) { "ore chain defaults to no enabled blocks" }
        check(settings.ores.allBlocks.contains(Material.DIAMOND_ORE)) { "diamond ore remains available in the menu catalog" }
        check(!settings.ores.allBlocks.contains(Material.STONE)) { "stone never enters ore catalog" }
    }

    private fun persistsActionAndMaterialToggles() {
        val file = tempSettingsFile()
        val store = SettingsStore(file)
        val config = YamlConfiguration()
        val logger = Logger.getLogger("test")
        store.load(config, logger)

        val enabledAction = store.setActionEnabled(FarmAction.SOW, true, config, logger)
        check(enabledAction.action(FarmAction.SOW).enabled) { "sow toggle persists true" }

        val enabledMaterial = store.setMaterialEnabled(MaterialGroup.ORES, Material.ANCIENT_DEBRIS, true, config, logger)
        check(enabledMaterial.ores.blocks.contains(Material.ANCIENT_DEBRIS)) { "material toggle persists true" }

        val reloaded = store.load(config, logger)
        check(reloaded.action(FarmAction.SOW).enabled) { "sow stays enabled after reload" }
        check(reloaded.ores.blocks.contains(Material.ANCIENT_DEBRIS)) { "ancient debris stays enabled after reload" }
    }

    private fun appliesFarmAndOffPresets() {
        val file = tempSettingsFile()
        val store = SettingsStore(file)
        val config = YamlConfiguration()
        val logger = Logger.getLogger("test")
        store.load(config, logger)

        val farm = store.applyPreset(ChainPreset.FARM, config, logger)
        check(farm.action(FarmAction.COLLECT).enabled) { "farm preset enables collect" }
        check(farm.action(FarmAction.SOW).enabled) { "farm preset enables sow" }
        check(farm.action(FarmAction.TILL).enabled) { "farm preset enables till" }
        check(farm.action(FarmAction.FERTILIZE).enabled) { "farm preset enables fertilize" }
        check(farm.crops.enabledBlocks.containsAll(farm.crops.allBlocks)) { "farm preset enables all crop materials" }
        check(farm.trees.blocks.isEmpty()) { "farm preset does not enable trees" }

        val off = store.applyPreset(ChainPreset.OFF, config, logger)
        check(FarmAction.entries.none { off.action(it).enabled }) { "off preset disables all actions" }
        check(MaterialGroup.entries.all { off.enabledMaterialsFor(it).isEmpty() }) { "off preset disables all materials" }
    }

    private fun keepsPlayerPresetsIndependent() {
        val file = tempSettingsFile()
        val store = SettingsStore(file)
        val playerA = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val playerB = UUID.fromString("00000000-0000-0000-0000-000000000002")

        check(!store.loadPlayerSettings(playerA).actionEnabled(FarmAction.SOW)) { "player prefs default to disabled" }

        val aSettings = store.applyPlayerPreset(playerA, "A", ChainPreset.FARM)
        val bSettings = store.loadPlayerSettings(playerB)

        check(aSettings.actionEnabled(FarmAction.SOW)) { "player A farm preset enables sow for A" }
        check(aSettings.groupEnabled(MaterialGroup.CROPS)) { "player A farm preset enables crops for A" }
        check(!bSettings.actionEnabled(FarmAction.SOW)) { "player B stays disabled when A enables farm" }
        check(!bSettings.groupEnabled(MaterialGroup.CROPS)) { "player B crop group stays disabled when A enables farm" }
    }

    private fun tempSettingsFile() = Files.createTempDirectory("leafchain-settings").resolve("settings.yml").toFile()
}
