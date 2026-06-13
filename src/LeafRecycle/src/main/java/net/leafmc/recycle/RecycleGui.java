package net.leafmc.recycle;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RecycleGui implements Listener {
    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private final LeafRecyclePlugin plugin;

    public RecycleGui(LeafRecyclePlugin plugin) {
        this.plugin = plugin;
    }

    public void openClaim(Player player, int page) {
        if (!player.hasPermission("leafrecycle.use")) {
            player.sendMessage(plugin.prefix() + "你没有权限打开回收站。");
            return;
        }
        ClaimHolder holder = new ClaimHolder(player.getUniqueId(), Math.max(0, page), false);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, plugin.getConfig().getString("gui.claim-title", "§0资源回收站"));
        holder.inventory = inventory;
        fillClaimInventory(holder, inventory);
        player.openInventory(inventory);
    }

    public void openSubmit(Player player) {
        if (!player.hasPermission("leafrecycle.submit")) {
            player.sendMessage(plugin.prefix() + "你没有权限投放回收物。");
            return;
        }
        SubmitHolder holder = new SubmitHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, plugin.getConfig().getString("gui.submit-title", "§0投放到回收站"));
        holder.inventory = inventory;
        fillSubmitControls(inventory);
        player.openInventory(inventory);
    }

    public void openAdmin(Player player, int page) {
        if (!player.hasPermission("leafrecycle.admin")) {
            player.sendMessage(plugin.prefix() + "你没有权限管理回收站。");
            return;
        }
        ClaimHolder holder = new ClaimHolder(player.getUniqueId(), Math.max(0, page), true);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, plugin.getConfig().getString("gui.admin-title", "§0回收站管理"));
        holder.inventory = inventory;
        fillClaimInventory(holder, inventory);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RecycleHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || !holder.owner.equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (holder instanceof SubmitHolder submitHolder) {
            handleSubmitClick(event, player, submitHolder);
            return;
        }
        event.setCancelled(true);
        if (!(holder instanceof ClaimHolder claimHolder)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        GuiAction action = claimHolder.actions.get(slot);
        if (action == null) {
            return;
        }
        handleClaimAction(player, claimHolder, action, event.getClick());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof SubmitHolder)) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 45 && rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SubmitHolder holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player) || holder.closed) {
            return;
        }
        holder.closed = true;
        List<ItemStack> submitted = collectSubmitItems(event.getInventory());
        if (submitted.isEmpty()) {
            return;
        }
        RecycleService.AddResult result = plugin.recycle().addItems(submitted, player.getUniqueId(), player.getName());
        plugin.saveRecycleData();
        for (ItemStack rejected : result.rejectedStacks()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(rejected);
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.sendMessage(plugin.prefix() + "已投放 §a" + result.acceptedItems() + " §f个物品到公共回收站。"
            + (result.rejectedItems() > 0 ? " §c容量不足，已退回 " + result.rejectedItems() + " 个物品。" : ""));
    }

    private void fillClaimInventory(ClaimHolder holder, Inventory inventory) {
        List<RecycleEntry> entries = plugin.recycle().entries();
        int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        if (holder.page > maxPage) {
            holder.page = maxPage;
        }
        int start = holder.page * PAGE_SIZE;
        for (int index = 0; index < PAGE_SIZE; index++) {
            int entryIndex = start + index;
            if (entryIndex >= entries.size()) {
                break;
            }
            RecycleEntry entry = entries.get(entryIndex);
            ItemStack item = entry.item();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : List.of();
                List<String> nextLore = new java.util.ArrayList<>(lore == null ? List.of() : lore);
                nextLore.add("§8----------------");
                nextLore.add("§7来源: §f" + entry.contributorName());
                nextLore.add("§7入池: §f" + formatAge(entry.createdAtMillis()));
                nextLore.add(holder.admin ? "§e左键取出，右键删除" : "§e点击领取，先到先得");
                meta.setLore(nextLore);
                item.setItemMeta(meta);
            }
            inventory.setItem(index, item);
            holder.actions.put(index, new GuiAction(ActionType.CLAIM_ENTRY, entry.id()));
        }
        if (entries.isEmpty()) {
            inventory.setItem(22, item(Material.BARREL, "§7回收站暂时是空的", List.of("§7管理员回收掉落物或玩家投放后会显示在这里。")));
        }

        inventory.setItem(45, item(Material.ARROW, "§e上一页", List.of("§7当前第 " + (holder.page + 1) + " 页")));
        holder.actions.put(45, new GuiAction(ActionType.PREVIOUS_PAGE, null));
        inventory.setItem(49, item(Material.HOPPER, "§a回收站", List.of(
            "§7当前堆叠: §f" + plugin.recycle().size(),
            "§7物品总数: §f" + plugin.recycle().totalItems(),
            holder.admin ? "§7管理模式" : "§7玩家领取模式"
        )));
        holder.actions.put(49, new GuiAction(ActionType.REFRESH, null));
        inventory.setItem(53, item(Material.ARROW, "§e下一页", List.of("§7当前第 " + (holder.page + 1) + " 页")));
        holder.actions.put(53, new GuiAction(ActionType.NEXT_PAGE, null));
        if (!holder.admin) {
            inventory.setItem(47, item(Material.CHEST, "§b投放物品", List.of("§7把你不需要的资源放进公共回收站。")));
            holder.actions.put(47, new GuiAction(ActionType.OPEN_SUBMIT, null));
            Player owner = Bukkit.getPlayer(holder.owner);
            if (owner != null && owner.hasPermission("leafrecycle.admin")) {
                inventory.setItem(51, item(Material.COMPASS, "§c管理模式", List.of("§7查看、取出或删除回收池物品。")));
                holder.actions.put(51, new GuiAction(ActionType.OPEN_ADMIN, null));
            }
        } else {
            inventory.setItem(47, item(Material.CHEST, "§b投放物品", List.of("§7管理员也可以手动投放资源。")));
            holder.actions.put(47, new GuiAction(ActionType.OPEN_SUBMIT, null));
            inventory.setItem(51, item(Material.PLAYER_HEAD, "§a玩家领取模式", List.of("§7切回普通领取视图。")));
            holder.actions.put(51, new GuiAction(ActionType.OPEN_CLAIM, null));
        }
    }

    private void fillSubmitControls(Inventory inventory) {
        for (int slot = 45; slot < SIZE; slot++) {
            inventory.setItem(slot, item(Material.GRAY_STAINED_GLASS_PANE, "§8", List.of()));
        }
        inventory.setItem(49, item(Material.LIME_DYE, "§a完成投放", List.of(
            "§7把要回收的物品放在上方 45 格。",
            "§7关闭界面或点击这里都会提交。"
        )));
        inventory.setItem(53, item(Material.BARRIER, "§c关闭", List.of("§7关闭后自动提交上方物品。")));
    }

    private void handleSubmitClick(InventoryClickEvent event, Player player, SubmitHolder holder) {
        int rawSlot = event.getRawSlot();
        int topSize = event.getInventory().getSize();
        if (rawSlot >= 45 && rawSlot < topSize) {
            event.setCancelled(true);
            if (rawSlot == 49 || rawSlot == 53) {
                Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
            }
            return;
        }
        if (event.isShiftClick() && rawSlot >= topSize) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) {
                return;
            }
            ItemStack moving = clicked.clone();
            int accepted = moveIntoSubmitSlots(holder.inventory, moving);
            if (accepted > 0) {
                clicked.setAmount(clicked.getAmount() - accepted);
                if (clicked.getAmount() <= 0) {
                    event.setCurrentItem(null);
                }
            }
        }
    }

    private int moveIntoSubmitSlots(Inventory inventory, ItemStack moving) {
        int before = moving.getAmount();
        for (int slot = 0; slot < PAGE_SIZE && moving.getAmount() > 0; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                ItemStack placed = moving.clone();
                placed.setAmount(Math.min(moving.getAmount(), moving.getMaxStackSize()));
                inventory.setItem(slot, placed);
                moving.setAmount(moving.getAmount() - placed.getAmount());
                continue;
            }
            if (!existing.isSimilar(moving)) {
                continue;
            }
            int room = existing.getMaxStackSize() - existing.getAmount();
            if (room <= 0) {
                continue;
            }
            int accepted = Math.min(room, moving.getAmount());
            existing.setAmount(existing.getAmount() + accepted);
            moving.setAmount(moving.getAmount() - accepted);
        }
        return before - moving.getAmount();
    }

    private List<ItemStack> collectSubmitItems(Inventory inventory) {
        List<ItemStack> submitted = new java.util.ArrayList<>();
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            submitted.add(item.clone());
            inventory.setItem(slot, null);
        }
        return submitted;
    }

    private void handleClaimAction(Player player, ClaimHolder holder, GuiAction action, ClickType click) {
        switch (action.type()) {
            case CLAIM_ENTRY -> handleEntryClick(player, holder, action.entryId(), click);
            case PREVIOUS_PAGE -> openSameMode(player, holder, Math.max(0, holder.page - 1));
            case NEXT_PAGE -> openSameMode(player, holder, holder.page + 1);
            case REFRESH -> openSameMode(player, holder, holder.page);
            case OPEN_SUBMIT -> openSubmit(player);
            case OPEN_ADMIN -> openAdmin(player, holder.page);
            case OPEN_CLAIM -> openClaim(player, holder.page);
        }
    }

    private void handleEntryClick(Player player, ClaimHolder holder, String entryId, ClickType click) {
        if (holder.admin && click.isRightClick()) {
            if (plugin.recycle().delete(entryId)) {
                plugin.saveRecycleData();
                player.sendMessage(plugin.prefix() + "已删除这一组回收物。");
            } else {
                player.sendMessage(plugin.prefix() + "这一组物品已经不存在。");
            }
            openAdmin(player, holder.page);
            return;
        }

        RecycleEntry entry = plugin.recycle().entry(entryId).orElse(null);
        if (entry == null) {
            player.sendMessage(plugin.prefix() + "这一组物品已经被领取。");
            openSameMode(player, holder, holder.page);
            return;
        }
        ItemStack item = entry.item();
        if (!plugin.hasInventoryRoom(player, item)) {
            player.sendMessage(plugin.prefix() + "背包空间不足，先清出空间再领取。");
            return;
        }
        ItemStack claimed = plugin.recycle().claim(entryId).orElse(null);
        if (claimed == null) {
            player.sendMessage(plugin.prefix() + "这一组物品已经被领取。");
            openSameMode(player, holder, holder.page);
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(claimed);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        plugin.saveRecycleData();
        player.sendMessage(plugin.prefix() + (holder.admin ? "已取出 " : "已领取 ") + describe(claimed) + "。");
        openSameMode(player, holder, holder.page);
    }

    private void openSameMode(Player player, ClaimHolder holder, int page) {
        if (holder.admin) {
            openAdmin(player, page);
        } else {
            openClaim(player, page);
        }
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String describe(ItemStack item) {
        String name = item.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            name = meta.getDisplayName();
        }
        return "§a" + item.getAmount() + "x §f" + name;
    }

    private static String formatAge(long createdAtMillis) {
        long seconds = Math.max(0L, Duration.ofMillis(System.currentTimeMillis() - createdAtMillis).toSeconds());
        if (seconds < 60) {
            return seconds + " 秒前";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " 分钟前";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " 小时前";
        }
        return (hours / 24) + " 天前";
    }

    private enum ActionType {
        CLAIM_ENTRY,
        PREVIOUS_PAGE,
        NEXT_PAGE,
        REFRESH,
        OPEN_SUBMIT,
        OPEN_ADMIN,
        OPEN_CLAIM
    }

    private record GuiAction(ActionType type, String entryId) {
    }

    private abstract static class RecycleHolder implements InventoryHolder {
        final UUID owner;
        Inventory inventory;

        private RecycleHolder(UUID owner) {
            this.owner = owner;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class ClaimHolder extends RecycleHolder {
        private final Map<Integer, GuiAction> actions = new HashMap<>();
        private final boolean admin;
        private int page;

        private ClaimHolder(UUID owner, int page, boolean admin) {
            super(owner);
            this.page = page;
            this.admin = admin;
        }
    }

    private static final class SubmitHolder extends RecycleHolder {
        private boolean closed;

        private SubmitHolder(UUID owner) {
            super(owner);
        }
    }

}
