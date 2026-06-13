package net.leafmc.chainharvest

import java.util.UUID
import java.util.Locale
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class ChainAdminGui(private val plugin: LeafChainHarvestPlugin) : Listener {
    private val legacy = LegacyComponentSerializer.legacySection()

    fun openMain(player: Player) {
        val holder = MenuHolder(player.uniqueId, 0)
        val inventory = Bukkit.createInventory(holder, 54, legacy.deserialize(plugin.config.getString("gui.title", "§0连锁采集") ?: "§0连锁采集"))
        holder.menuInventory = inventory

        inventory.setItem(4, item(Material.NETHER_STAR, "§a连锁采集管理", listOf(
            "§7默认全部关闭，点亮后生效。",
            "§7点击高亮项表示允许。",
        )))
        addActionToggle(holder, inventory, 10, FarmAction.COLLECT)
        addActionToggle(holder, inventory, 11, FarmAction.SOW)
        addActionToggle(holder, inventory, 12, FarmAction.FERTILIZE)

        addGroupButton(holder, inventory, 20, MaterialGroup.CROPS)
        addGroupButton(holder, inventory, 22, MaterialGroup.TREES)
        addGroupButton(holder, inventory, 24, MaterialGroup.ORES)

        inventory.setItem(49, item(Material.BOOK, "§e说明", listOf(
            "§7农作物、木头、矿物都使用全服设置。",
            "§7石头、深板岩、泥土等基础块不在默认目录中。",
        )))
        player.openInventory(inventory)
    }

    private fun openMaterials(player: Player, group: MaterialGroup, page: Int) {
        val allMaterials = plugin.settings().materialsFor(group)
        val safePage = page.coerceIn(0, AdminMenuModel.maxPage(allMaterials))
        val titleTemplate = plugin.config.getString("gui.materials-title", "§0连锁材料: {group}") ?: "§0连锁材料: {group}"
        val holder = MenuHolder(player.uniqueId, safePage)
        val inventory = Bukkit.createInventory(holder, 54, legacy.deserialize(titleTemplate.replace("{group}", group.displayName)))
        holder.menuInventory = inventory

        val enabled = plugin.settings().enabledMaterialsFor(group)
        AdminMenuModel.page(allMaterials, safePage).forEachIndexed { slot, material ->
            val allowed = enabled.contains(material)
            inventory.setItem(slot, toggleItem(AdminMenuModel.iconFor(material), allowed, displayMaterial(material), listOf("§7点击切换是否允许。")))
            holder.actions[slot] = GuiAction.ToggleMaterial(group, material)
        }

        inventory.setItem(45, item(Material.ARROW, "§e上一页", listOf("§7当前第 ${safePage + 1} 页")))
        holder.actions[45] = GuiAction.Page(group, safePage - 1)
        inventory.setItem(49, item(Material.BARRIER, "§c返回", listOf("§7回到连锁采集管理。")))
        holder.actions[49] = GuiAction.Back
        inventory.setItem(53, item(Material.ARROW, "§e下一页", listOf("§7当前第 ${safePage + 1} 页")))
        holder.actions[53] = GuiAction.Page(group, safePage + 1)
        player.openInventory(inventory)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? MenuHolder ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (player.uniqueId != holder.owner) {
            return
        }
        val slot = event.rawSlot
        if (slot < 0 || slot >= event.inventory.size) {
            return
        }
        when (val action = holder.actions[slot]) {
            is GuiAction.ToggleAction -> {
                val current = plugin.settings().action(action.action).enabled
                plugin.setActionEnabled(action.action, !current)
                player.sendMessage(toggleMessage(action.action.displayName, !current))
                openMain(player)
            }
            is GuiAction.OpenGroup -> openMaterials(player, action.group, 0)
            is GuiAction.ToggleMaterial -> {
                val current = plugin.settings().enabledMaterialsFor(action.group).contains(action.material)
                plugin.setMaterialEnabled(action.group, action.material, !current)
                player.sendMessage(toggleMessage(displayMaterial(action.material), !current))
                openMaterials(player, action.group, holder.page)
            }
            is GuiAction.Page -> openMaterials(player, action.group, action.page)
            GuiAction.Back -> openMain(player)
            null -> return
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val holder = event.inventory.holder as? MenuHolder ?: return
        if (event.rawSlots.any { it in 0 until event.inventory.size }) {
            event.isCancelled = true
        }
        val player = event.whoClicked as? Player ?: return
        if (player.uniqueId != holder.owner) {
            event.isCancelled = true
        }
    }

    private fun addActionToggle(holder: MenuHolder, inventory: Inventory, slot: Int, action: FarmAction) {
        val enabled = plugin.settings().action(action).enabled
        inventory.setItem(slot, toggleItem(AdminMenuModel.actionIcon(action), enabled, action.displayName, listOf("§7点击切换动作。")))
        holder.actions[slot] = GuiAction.ToggleAction(action)
    }

    private fun addGroupButton(holder: MenuHolder, inventory: Inventory, slot: Int, group: MaterialGroup) {
        val settings = plugin.settings()
        val enabled = settings.enabledMaterialsFor(group).size
        val total = settings.materialsFor(group).size
        inventory.setItem(slot, item(AdminMenuModel.groupIcon(group), "§b${group.displayName}", listOf(
            "§7已允许: §a$enabled§7/§f$total",
            "§7点击管理材料。",
        )))
        holder.actions[slot] = GuiAction.OpenGroup(group)
    }

    private fun toggleMessage(target: String, enabled: Boolean): String {
        return plugin.message(
            "toggled",
            mapOf("target" to target, "state" to if (enabled) "§a允许§f" else "§c关闭§f"),
        )
    }

    private fun toggleItem(material: Material, enabled: Boolean, name: String, lore: List<String>): ItemStack {
        val prefix = if (enabled) "§a" else "§c"
        val state = "§7当前: " + if (enabled) "§a允许" else "§c关闭"
        return item(material, "$prefix$name", listOf(state) + lore)
    }

    private fun item(material: Material, name: String, lore: List<String>): ItemStack {
        val stack = try {
            ItemStack(AdminMenuModel.iconFor(material))
        } catch (_: IllegalArgumentException) {
            ItemStack(Material.PAPER)
        }
        val meta: ItemMeta? = stack.itemMeta
        if (meta != null) {
            meta.displayName(legacy.deserialize(name))
            meta.lore(lore.map { legacy.deserialize(it) })
            stack.itemMeta = meta
        }
        return stack
    }

    private fun displayMaterial(material: Material): String {
        return material.name.lowercase(Locale.ROOT).replace('_', ' ')
    }

    private class MenuHolder(
        val owner: UUID,
        val page: Int,
    ) : InventoryHolder {
        val actions: MutableMap<Int, GuiAction> = HashMap()
        lateinit var menuInventory: Inventory

        override fun getInventory(): Inventory = menuInventory
    }

    private sealed interface GuiAction {
        data class ToggleAction(val action: FarmAction) : GuiAction
        data class OpenGroup(val group: MaterialGroup) : GuiAction
        data class ToggleMaterial(val group: MaterialGroup, val material: Material) : GuiAction
        data class Page(val group: MaterialGroup, val page: Int) : GuiAction
        data object Back : GuiAction
    }
}
