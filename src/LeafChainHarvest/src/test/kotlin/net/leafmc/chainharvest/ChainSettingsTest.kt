package net.leafmc.chainharvest

import java.nio.file.Files
import java.util.logging.Logger
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration

object ChainSettingsTest {
    @JvmStatic
    fun main(args: Array<String>) {
        createsSettingsFileFromDefaults()
        persistsActionAndMaterialToggles()
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

    private fun tempSettingsFile() = Files.createTempDirectory("leafchain-settings").resolve("settings.yml").toFile()
}
