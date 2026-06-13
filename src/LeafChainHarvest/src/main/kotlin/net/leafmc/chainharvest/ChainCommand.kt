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
            else -> plugin.messageList("help").forEach(sender::sendMessage)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.size != 1) {
            return emptyList()
        }
        return ChainCommandFormat.subcommands(sender)
            .filter { it.startsWith(args[0].lowercase(Locale.ROOT)) }
    }
}

object ChainCommandFormat {
    fun subcommands(sender: CommandSender): List<String> {
        val result = mutableListOf<String>()
        if (sender.hasPermission("leafchain.menu")) {
            result += "menu"
        }
        if (sender.hasPermission("leafchain.reload")) {
            result += "reload"
        }
        return result
    }

    fun helpLines(prefix: String): List<String> = listOf(
        "${prefix}命令：",
        "§7/leafchain menu §f打开管理菜单",
        "§7/leafchain reload §f重载配置",
    )
}
