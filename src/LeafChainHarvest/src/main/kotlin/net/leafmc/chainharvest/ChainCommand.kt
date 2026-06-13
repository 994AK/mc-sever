package net.leafmc.chainharvest

import java.util.Locale
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ChainCommand(
    private val plugin: LeafChainHarvestPlugin,
    private val gui: ChainAdminGui,
) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            plugin.messageList("help").forEach(sender::sendMessage)
            return true
        }

        when (args[0].lowercase(Locale.ROOT)) {
            "menu", "gui" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.message("player-only"))
                    return true
                }
                if (!sender.hasPermission("leafchain.menu")) {
                    sender.sendMessage(plugin.message("no-permission"))
                    return true
                }
                gui.openMain(sender)
                sender.sendMessage(plugin.message("menu-opened"))
            }
            "reload" -> {
                if (!sender.hasPermission("leafchain.reload")) {
                    sender.sendMessage(plugin.message("no-permission"))
                    return true
                }
                plugin.reloadSettings()
                sender.sendMessage(plugin.message("reloaded"))
            }
            "preset" -> {
                if (!sender.hasPermission("leafchain.admin")) {
                    sender.sendMessage(plugin.message("no-permission"))
                    return true
                }
                val preset = ChainPreset.parse(args.getOrNull(1))
                if (preset == null) {
                    sender.sendMessage(plugin.message("preset-usage"))
                    return true
                }
                plugin.applyPreset(preset)
                sender.sendMessage(plugin.message("preset-applied", mapOf("name" to preset.displayName)))
            }
            "self", "me" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.message("player-only"))
                    return true
                }
                if (!sender.hasPermission("leafchain.use")) {
                    sender.sendMessage(plugin.message("no-permission"))
                    return true
                }
                val preset = ChainPreset.parse(args.getOrNull(1))
                if (preset == null) {
                    sender.sendMessage(plugin.message("self-preset-usage"))
                    return true
                }
                plugin.applyPlayerPreset(sender, preset)
                sender.sendMessage(plugin.message("self-preset-applied", mapOf("name" to preset.displayName)))
            }
            else -> plugin.messageList("help").forEach(sender::sendMessage)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return ChainCommandFormat.subcommands(sender)
                .filter { it.startsWith(args[0].lowercase(Locale.ROOT)) }
        }
        if (args.size == 2 && args[0].equals("preset", ignoreCase = true) && sender.hasPermission("leafchain.admin")) {
            return ChainPreset.entries.map { it.key }
                .filter { it.startsWith(args[1].lowercase(Locale.ROOT)) }
        }
        if (args.size == 2 && args[0].equals("self", ignoreCase = true) && sender.hasPermission("leafchain.use")) {
            return ChainPreset.entries.map { it.key }
                .filter { it.startsWith(args[1].lowercase(Locale.ROOT)) }
        }
        return emptyList()
    }
}

object ChainCommandFormat {
    fun subcommands(sender: CommandSender): List<String> {
        val result = mutableListOf<String>()
        if (sender.hasPermission("leafchain.menu")) {
            result += "menu"
        }
        if (sender.hasPermission("leafchain.use")) {
            result += "self"
        }
        if (sender.hasPermission("leafchain.reload")) {
            result += "reload"
        }
        if (sender.hasPermission("leafchain.admin")) {
            result += "preset"
        }
        return result
    }

    fun helpLines(prefix: String): List<String> = listOf(
        "${prefix}命令：",
        "§7/leafchain self farm §f自己开启农作物收集/播种/耕地/施肥",
        "§7/leafchain self chain §f自己开启木头和矿物连锁",
        "§7/leafchain self all|off §f自己全部开启或关闭",
        "§7/leafchain menu §f打开管理菜单",
        "§7/leafchain preset farm §f一键开启农作物收集/播种/耕地/施肥",
        "§7/leafchain preset chain §f一键开启木头和矿物连锁",
        "§7/leafchain preset all|off §f全部开启或关闭",
        "§7/leafchain reload §f重载配置",
    )
}
