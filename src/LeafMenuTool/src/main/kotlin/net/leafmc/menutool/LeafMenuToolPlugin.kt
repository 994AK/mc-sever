package net.leafmc.menutool

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin

class LeafMenuToolPlugin : JavaPlugin(), Listener {
    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }
        val item = event.item ?: return
        if (item.type != Material.CLOCK) {
            return
        }
        val meta = item.itemMeta ?: return
        if (!meta.hasDisplayName()) {
            return
        }
        val displayName = meta.displayName() ?: return
        if (PlainTextComponentSerializer.plainText().serialize(displayName) != "菜单钟") {
            return
        }

        event.isCancelled = true
        event.player.performCommand("menu")
    }
}
