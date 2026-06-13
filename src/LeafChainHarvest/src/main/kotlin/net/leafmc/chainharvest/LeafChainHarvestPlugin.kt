package net.leafmc.chainharvest

import org.bukkit.command.PluginCommand
import org.bukkit.plugin.java.JavaPlugin

class LeafChainHarvestPlugin : JavaPlugin() {
    private lateinit var store: SettingsStore
    private lateinit var currentSettings: LeafChainSettings
    private lateinit var mutationGate: ProtectionGate
    private val chainBreaking = ThreadLocal.withInitial { false }

    override fun onEnable() {
        saveDefaultConfig()
        store = SettingsStore(this)
        reloadSettings()
        mutationGate = ProtectionGate({ settings().protection }, createResidenceAdapter())

        val adminGui = ChainAdminGui(this)
        val command = ChainCommand(this, adminGui)
        val pluginCommand: PluginCommand? = getCommand("leafchain")
        pluginCommand?.setExecutor(command)
        pluginCommand?.tabCompleter = command

        server.pluginManager.registerEvents(adminGui, this)
        server.pluginManager.registerEvents(ChainBreakListener(this), this)
        server.pluginManager.registerEvents(FarmActionListener(this), this)
    }

    fun settings(): LeafChainSettings = currentSettings

    fun protectionGate(): ProtectionGate = mutationGate

    fun reloadSettings() {
        reloadConfig()
        currentSettings = store.load(config, logger)
    }

    fun setActionEnabled(action: FarmAction, enabled: Boolean) {
        currentSettings = store.setActionEnabled(action, enabled, config, logger)
    }

    fun setMaterialEnabled(group: MaterialGroup, material: org.bukkit.Material, enabled: Boolean) {
        currentSettings = store.setMaterialEnabled(group, material, enabled, config, logger)
    }

    fun <T> whileBreakingChain(block: () -> T): T {
        val previous = chainBreaking.get()
        chainBreaking.set(true)
        return try {
            block()
        } finally {
            chainBreaking.set(previous)
        }
    }

    fun isBreakingChain(): Boolean = chainBreaking.get()

    fun message(path: String, placeholders: Map<String, String> = emptyMap()): String {
        val prefix = config.getString("messages.prefix", "§a[连锁] §f") ?: "§a[连锁] §f"
        var message = config.getString("messages.$path", "") ?: ""
        message = message.replace("{prefix}", prefix)
        placeholders.forEach { (key, value) -> message = message.replace("{$key}", value) }
        return message
    }

    fun messageList(path: String): List<String> {
        val prefix = config.getString("messages.prefix", "§a[连锁] §f") ?: "§a[连锁] §f"
        val values = config.getStringList("messages.$path")
        return if (values.isEmpty()) {
            ChainCommandFormat.helpLines(prefix)
        } else {
            values.map { it.replace("{prefix}", prefix) }
        }
    }

    private fun createResidenceAdapter(): ProtectionAdapter? {
        if (!server.pluginManager.isPluginEnabled("Residence")) {
            return null
        }
        return try {
            ResidenceProtectionGate()
        } catch (error: LinkageError) {
            logger.warning("Residence is enabled but its API could not be linked: ${error.message}")
            null
        }
    }
}
