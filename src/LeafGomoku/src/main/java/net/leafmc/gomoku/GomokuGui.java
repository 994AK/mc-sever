package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GomokuGui implements Listener {
    private static final int SIZE = 54;
    private static final String TITLE = "§0五子棋大厅";
    private static final int[] ROOM_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private final LeafGomokuPlugin plugin;

    public GomokuGui(LeafGomokuPlugin plugin) {
        this.plugin = plugin;
    }

    public void openLobby(Player player) {
        openLobby(player, 0);
    }

    private void openLobby(Player player, int requestedPage) {
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.inventory = inventory;

        List<GomokuRoom> rooms = new ArrayList<>(plugin.rooms().rooms());
        int totalPages = Math.max(1, (int) Math.ceil((double) rooms.size() / (double) ROOM_SLOTS.length));
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        holder.page = page;
        int start = page * ROOM_SLOTS.length;
        int end = Math.min(rooms.size(), start + ROOM_SLOTS.length);
        inventory.setItem(4, item(
            Material.COMPASS,
            "§a五子棋大厅 §7" + (page + 1) + "/" + totalPages,
            List.of(
                "§7房间数：§f" + rooms.size(),
                "§7本页：§f" + (rooms.isEmpty() ? 0 : start + 1) + "§7-§f" + end,
                "§7点击房间查看详情",
                "§7详情页可选择加入或只观战"
            )
        ));

        if (rooms.isEmpty()) {
            inventory.setItem(22, item(
                Material.BARRIER,
                "§c暂无房间",
                List.of("§7管理员输入 §f/gomoku admin place 房间名 §7后右键锚点")
            ));
        } else {
            for (int index = start; index < end; index++) {
                GomokuRoom room = rooms.get(index);
                int slot = ROOM_SLOTS[index - start];
                inventory.setItem(slot, roomItem(room, index + 1));
                holder.actions.put(slot, new GuiAction(ActionType.OPEN_ROOM, room.config().id(), page));
            }
        }

        if (page > 0) {
            setPageAction(holder, inventory, 45, ActionType.PREV_PAGE, Material.ARROW, "§7上一页", List.of(
                "§7第 §f" + page + " §7页"
            ), page - 1);
        } else {
            inventory.setItem(45, item(Material.GRAY_STAINED_GLASS_PANE, "§8上一页", List.of()));
        }
        setAction(holder, inventory, 46, ActionType.STATS, Material.WRITABLE_BOOK, "§e我的统计", List.of(
            "§7查看积分、胜负和平局。"
        ));
        setAction(holder, inventory, 47, ActionType.LEADERBOARD, Material.GOLD_INGOT, "§6积分榜", List.of(
            "§7查看前 10 名五子棋积分。"
        ));
        setAction(holder, inventory, 48, ActionType.LEAVE, Material.BARRIER, "§c离开房间", List.of(
            "§7离开当前参赛席位或观战点。"
        ));
        setAction(holder, inventory, 49, ActionType.REFRESH, Material.CLOCK, "§a刷新大厅", List.of(
            "§7重新读取房间、席位和观众状态。"
        ));
        if (player.hasPermission(GomokuPermission.ADMIN_SETUP.node()) || player.hasPermission(GomokuPermission.ADMIN_ROOM.node())) {
            setAction(holder, inventory, 50, ActionType.ADMIN_GUIDE, Material.OAK_SIGN, "§c管理命令", List.of(
                "§f/gomoku admin place 房间名",
                "§7右键地面锚点生成房间",
                "§f/gomoku admin init 房间名",
                "§f/gomoku admin inspect 房间名"
            ));
        }
        setAction(holder, inventory, 51, ActionType.APPEARANCE_MENU, Material.AMETHYST_SHARD, "§d我的棋子皮肤", List.of(
            "§7选择默认棋子皮肤，或兑换高级外观。"
        ));
        setAction(holder, inventory, 52, ActionType.MAIN_MENU, Material.NETHER_STAR, "§a回到主菜单", List.of(
            "§7打开服务器模块菜单。"
        ));
        if (page + 1 < totalPages) {
            setPageAction(holder, inventory, 53, ActionType.NEXT_PAGE, Material.SPECTRAL_ARROW, "§7下一页", List.of(
                "§7第 §f" + (page + 2) + " §7页"
            ), page + 1);
        } else {
            inventory.setItem(53, item(Material.GRAY_STAINED_GLASS_PANE, "§8下一页", List.of()));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        GuiAction action = holder.actions.get(slot);
        if (action == null) {
            return;
        }

        switch (action.type()) {
            case OPEN_ROOM -> openRoom(player, action.roomId(), action.page());
            case LEAVE -> run(player, "gomoku leave");
            case STATS -> run(player, "gomoku stats");
            case LEADERBOARD -> run(player, "gomoku leaderboard points");
            case REFRESH -> openLobby(player, action.page());
            case PREV_PAGE, NEXT_PAGE -> openLobby(player, action.page());
            case JOIN_ROOM -> run(player, "gomoku join " + action.roomId());
            case SPECTATE_ROOM -> run(player, "gomoku spectate " + action.roomId());
            case STATUS_ROOM -> run(player, "gomoku status " + action.roomId());
            case REFRESH_ROOM -> openRoom(player, action.roomId(), action.page());
            case BACK_TO_LOBBY -> openLobby(player, action.page());
            case APPEARANCE_MENU -> openSkins(player, "", action.page());
            case OPEN_THEMES -> openThemes(player, action.roomId(), action.page());
            case OPEN_SKINS -> openSkins(player, action.roomId(), action.page());
            case SELECT_THEME -> updateThemeSelection(player, action);
            case SELECT_SKIN -> updateSkinSelection(player, action);
            case BUY_THEME -> buyTheme(player, action);
            case BUY_SKIN -> buySkin(player, action);
            case ADMIN_GUIDE -> {
                player.closeInventory();
                player.sendMessage("§6[五子棋管理] §f/gomoku admin place 房间名 §7- 进入右键锚点放置模式");
                player.sendMessage("§6[五子棋管理] §f/gomoku admin create 房间名 §7- 兼容旧命令，从当前位置生成布局");
                player.sendMessage("§6[五子棋管理] §f/gomoku admin init 房间名 §7- 初始化实体棋盘");
                player.sendMessage("§6[五子棋管理] §f/gomoku admin inspect 房间名 §7- 查看席位、观众和状态");
            }
            case MAIN_MENU -> run(player, "menu");
        }
    }

    private void run(Player player, String command) {
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(command));
    }

    private void updateThemeSelection(Player player, GuiAction action) {
        send(player, plugin.selectBoardTheme(player, action.roomId(), action.value()));
        refreshThemes(player, action);
    }

    private void updateSkinSelection(Player player, GuiAction action) {
        send(player, plugin.selectPieceSkin(player, action.value()));
        refreshSkins(player, action);
    }

    private void buyTheme(Player player, GuiAction action) {
        send(player, plugin.buyAppearance(player, "theme", action.value(), action.roomId()));
        refreshThemes(player, action);
    }

    private void buySkin(Player player, GuiAction action) {
        send(player, plugin.buyAppearance(player, "skin", action.value()));
        refreshSkins(player, action);
    }

    private void refreshThemes(Player player, GuiAction action) {
        Bukkit.getScheduler().runTask(plugin, () -> openThemes(player, action.roomId(), action.page()));
    }

    private void refreshSkins(Player player, GuiAction action) {
        Bukkit.getScheduler().runTask(plugin, () -> openSkins(player, action.roomId(), action.page()));
    }

    private void send(Player player, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        for (String line : message.split("\\R")) {
            if (!line.isBlank()) {
                player.sendMessage(line);
            }
        }
    }

    private void openRoom(Player player, String roomId, int page) {
        GomokuRoom room = plugin.resolveRoom(roomId).orElse(null);
        if (room == null) {
            player.sendMessage("§c找不到五子棋房间: " + roomId);
            openLobby(player, page);
            return;
        }

        MenuHolder holder = new MenuHolder();
        holder.page = page;
        Inventory inventory = Bukkit.createInventory(holder, SIZE, "§0五子棋房间 " + room.config().id());
        holder.inventory = inventory;

        RoomState state = room.state();
        MatchAppearance appearance = room.appearance();
        inventory.setItem(4, roomDetailItem(room));

        if (canJoin(room)) {
            setRoomAction(holder, inventory, 20, ActionType.JOIN_ROOM, Material.BLACK_CONCRETE, "§f加入对局", List.of(
                "§7进入 §f" + room.config().id() + " §7的黑/白参赛席位。",
                "§7不会绕过席位、回合和权限校验。"
            ), room.config().id(), page);
        } else {
            inventory.setItem(20, item(Material.GRAY_DYE, "§8当前不能加入", List.of(
                "§7状态：§f" + stateLabel(state),
                "§7可选择只观战。"
            )));
        }

        if (state != RoomState.DISABLED) {
            setRoomAction(holder, inventory, 22, ActionType.SPECTATE_ROOM, Material.SPYGLASS, "§b只观战", List.of(
                "§7进入观众点，不占黑白席位。",
                "§7观众：§f" + room.spectatorCount() + "§7/§f" + room.config().spectatorCapacity()
            ), room.config().id(), page);
        } else {
            inventory.setItem(22, item(Material.BARRIER, "§c不能观战", List.of("§7房间不可用。")));
        }

        setRoomAction(holder, inventory, 24, ActionType.STATUS_ROOM, Material.PAPER, "§7输出状态", List.of(
            "§7在聊天栏输出房间状态。"
        ), room.config().id(), page);
        setRoomAction(holder, inventory, 29, ActionType.OPEN_THEMES, appearance.boardTheme().primaryMaterial(), "§a棋盘主题", List.of(
            "§7当前：§f" + appearance.boardTheme().displayName(),
            "§7可在开局前切换本房间主题。"
        ), room.config().id(), page);
        setRoomAction(holder, inventory, 31, ActionType.REFRESH_ROOM, Material.CLOCK, "§a刷新本房间", List.of(
            "§7重新读取席位、回合和观众数量。"
        ), room.config().id(), page);
        setRoomAction(holder, inventory, 33, ActionType.OPEN_SKINS, appearance.blackSkin().iconMaterial(), "§d棋子皮肤", List.of(
            "§7黑方：§f" + appearance.blackSkin().displayName(),
            "§7白方：§f" + appearance.whiteSkin().displayName(),
            "§7玩家各自选择，双方不能重复。"
        ), room.config().id(), page);
        setAction(holder, inventory, 40, ActionType.LEAVE, Material.BARRIER, "§c离开五子棋", List.of(
            "§7离开当前参赛席位或观战点。"
        ));

        setPageAction(holder, inventory, 45, ActionType.BACK_TO_LOBBY, Material.ARROW, "§7返回大厅", List.of(
            "§7回到第 §f" + (page + 1) + " §7页。"
        ), page);
        setAction(holder, inventory, 53, ActionType.MAIN_MENU, Material.NETHER_STAR, "§a回到主菜单", List.of(
            "§7打开服务器模块菜单。"
        ));

        player.openInventory(inventory);
    }

    private void openThemes(Player player, String roomId, int page) {
        GomokuRoom room = plugin.resolveRoom(roomId).orElse(null);
        if (room == null) {
            player.sendMessage("§c找不到五子棋房间: " + roomId);
            openLobby(player, page);
            return;
        }
        MenuHolder holder = new MenuHolder();
        holder.page = page;
        Inventory inventory = Bukkit.createInventory(holder, SIZE, "§0棋盘主题 " + room.config().id());
        holder.inventory = inventory;
        inventory.setItem(4, item(room.appearance().boardTheme().primaryMaterial(), "§a棋盘主题", List.of(
            "§7房间：§f" + room.config().id(),
            "§7当前：§f" + room.appearance().boardTheme().displayName()
        )));
        List<BoardTheme> themes = plugin.appearances().boardThemes();
        for (int index = 0; index < Math.min(themes.size(), ROOM_SLOTS.length); index++) {
            BoardTheme theme = themes.get(index);
            boolean unlocked = plugin.appearanceUnlocks().canUseTheme(player.getUniqueId(), theme);
            int slot = ROOM_SLOTS[index];
            inventory.setItem(slot, boardThemeItem(theme, unlocked, room.appearance().boardTheme().id().equals(theme.id())));
            holder.actions.put(slot, new GuiAction(unlocked ? ActionType.SELECT_THEME : ActionType.BUY_THEME, room.config().id(), theme.id(), page));
        }
        setRoomAction(holder, inventory, 45, ActionType.OPEN_ROOM, Material.ARROW, "§7返回房间", List.of(
            "§7回到房间详情。"
        ), room.config().id(), page);
        player.openInventory(inventory);
    }

    private void openSkins(Player player, String roomId, int page) {
        MenuHolder holder = new MenuHolder();
        holder.page = page;
        Inventory inventory = Bukkit.createInventory(holder, SIZE, roomId == null || roomId.isBlank() ? "§0棋子皮肤" : "§0棋子皮肤 " + roomId);
        holder.inventory = inventory;
        GomokuRoom room = roomId == null || roomId.isBlank() ? null : plugin.resolveRoom(roomId).orElse(null);
        inventory.setItem(4, item(Material.AMETHYST_SHARD, "§d棋子皮肤", List.of(
            "§7选择自己的默认棋子皮肤。",
            "§7加入房间后，双方同局不能重复。"
        )));
        List<PieceSkin> skins = plugin.appearances().pieceSkins();
        for (int index = 0; index < Math.min(skins.size(), ROOM_SLOTS.length); index++) {
            PieceSkin skin = skins.get(index);
            boolean unlocked = plugin.appearanceUnlocks().canUseSkin(player.getUniqueId(), skin);
            int slot = ROOM_SLOTS[index];
            inventory.setItem(slot, pieceSkinItem(skin, unlocked, room != null && (
                room.appearance().blackSkin().id().equals(skin.id()) || room.appearance().whiteSkin().id().equals(skin.id())
            )));
            holder.actions.put(slot, new GuiAction(unlocked ? ActionType.SELECT_SKIN : ActionType.BUY_SKIN, roomId == null ? "" : roomId, skin.id(), page));
        }
        if (room != null) {
            setRoomAction(holder, inventory, 45, ActionType.OPEN_ROOM, Material.ARROW, "§7返回房间", List.of(
                "§7回到房间详情。"
            ), room.config().id(), page);
        } else {
            setPageAction(holder, inventory, 45, ActionType.BACK_TO_LOBBY, Material.ARROW, "§7返回大厅", List.of(
                "§7回到大厅。"
            ), page);
        }
        player.openInventory(inventory);
    }

    private ItemStack roomItem(GomokuRoom room, int index) {
        RoomState state = room.state();
        List<String> lore = new ArrayList<>();
        lore.add("§7状态：§f" + stateLabel(state));
        lore.add("§7黑方：§f" + room.lease(Stone.BLACK).map(SeatLease::playerName).orElse("空"));
        lore.add("§7白方：§f" + room.lease(Stone.WHITE).map(SeatLease::playerName).orElse("空"));
        lore.add("§7回合：§f" + room.match().currentTurn().displayName());
        lore.add("§7主题：§f" + room.appearance().boardTheme().displayName());
        lore.add("§7观众：§f" + room.spectatorCount() + "§7/§f" + room.config().spectatorCapacity());
        lore.add("§7结果：§f" + room.lastResult());
        lore.add("");
        lore.add("§f点击查看详情");
        return item(stateMaterial(state), "§e#" + index + " " + room.config().id() + " §7| §f" + stateLabel(state), lore);
    }

    private ItemStack roomDetailItem(GomokuRoom room) {
        RoomState state = room.state();
        List<String> lore = new ArrayList<>();
        lore.add("§7状态：§f" + stateLabel(state));
        lore.add("§7黑方：§f" + room.lease(Stone.BLACK).map(SeatLease::playerName).orElse("空"));
        lore.add("§7白方：§f" + room.lease(Stone.WHITE).map(SeatLease::playerName).orElse("空"));
        lore.add("§7回合：§f" + room.match().currentTurn().displayName());
        lore.add("§7主题：§f" + room.appearance().boardTheme().displayName());
        lore.add("§7黑方棋子：§f" + room.appearance().blackSkin().displayName());
        lore.add("§7白方棋子：§f" + room.appearance().whiteSkin().displayName());
        lore.add("§7观众：§f" + room.spectatorCount() + "§7/§f" + room.config().spectatorCapacity());
        lore.add("§7结果：§f" + room.lastResult());
        lore.add("");
        lore.add(canJoin(room) ? "§a可以加入对局" : "§8当前不可加入");
        lore.add(state == RoomState.DISABLED ? "§c不能观战" : "§b可以只观战");
        return item(stateMaterial(state), "§e" + room.config().id() + " §7详情", lore);
    }

    private boolean canJoin(GomokuRoom room) {
        RoomState state = room.state();
        return state == RoomState.OPEN || state == RoomState.WAITING;
    }

    private void setAction(
        MenuHolder holder,
        Inventory inventory,
        int slot,
        ActionType action,
        Material material,
        String name,
        List<String> lore
    ) {
        inventory.setItem(slot, item(material, name, lore));
        holder.actions.put(slot, new GuiAction(action, "", holder.page));
    }

    private void setPageAction(
        MenuHolder holder,
        Inventory inventory,
        int slot,
        ActionType action,
        Material material,
        String name,
        List<String> lore,
        int page
    ) {
        inventory.setItem(slot, item(material, name, lore));
        holder.actions.put(slot, new GuiAction(action, "", page));
    }

    private void setRoomAction(
        MenuHolder holder,
        Inventory inventory,
        int slot,
        ActionType action,
        Material material,
        String name,
        List<String> lore,
        String roomId,
        int page
    ) {
        inventory.setItem(slot, item(material, name, lore));
        holder.actions.put(slot, new GuiAction(action, roomId, page));
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack boardThemeItem(BoardTheme theme, boolean unlocked, boolean selected) {
        List<String> lore = new ArrayList<>();
        lore.add("§7主格：§f" + theme.primaryMaterial().name());
        lore.add("§7副格：§f" + theme.secondaryMaterial().name());
        lore.add("§7状态：§f" + unlockLabel(theme.unlockedByDefault(), unlocked, theme.cost()));
        lore.add("");
        lore.add(selected ? "§a当前本房间主题" : unlocked ? "§f点击切换本房间主题" : "§e点击消耗积分兑换");
        return item(theme.primaryMaterial(), (selected ? "§a" : unlocked ? "§f" : "§6") + theme.displayName(), lore);
    }

    private ItemStack pieceSkinItem(PieceSkin skin, boolean unlocked, boolean usedInRoom) {
        List<String> lore = new ArrayList<>();
        lore.add("§7类型：§f" + displayTypeLabel(skin.displayType()));
        lore.add("§7材料：§f" + skin.material().name());
        if (skin.displayType() == PieceDisplayType.PLAYER_HEAD && !skin.headOwner().isBlank()) {
            lore.add("§7头颅：§f" + skin.headOwner());
        }
        if (skin.displayType() == PieceDisplayType.ENTITY && !skin.entityType().isBlank()) {
            lore.add("§7实体：§f" + skin.entityType());
        }
        lore.add("§7动画：§f" + animationLabel(skin.animationType()));
        lore.add("§7状态：§f" + unlockLabel(skin.unlockedByDefault(), unlocked, skin.cost()));
        lore.add("");
        lore.add(usedInRoom ? "§a当前房间已有席位使用" : unlocked ? "§f点击选择为你的棋子" : "§e点击消耗积分兑换");
        return item(skin.iconMaterial(), (usedInRoom ? "§a" : unlocked ? "§f" : "§6") + skin.displayName(), lore);
    }

    private String displayTypeLabel(PieceDisplayType type) {
        return switch (type) {
            case BLOCK -> "方块";
            case PLAYER_HEAD -> "头颅";
            case ENTITY -> "静态生物";
        };
    }

    private String animationLabel(PieceAnimationType type) {
        return switch (type) {
            case ARC -> "垂直落子";
            case POP -> "落点弹出";
            case EXPLOSION -> "落点爆炸";
            case WALK -> "实体落子";
        };
    }

    private String unlockLabel(boolean defaultUnlocked, boolean unlocked, int cost) {
        if (defaultUnlocked) {
            return "默认开放";
        }
        if (unlocked) {
            return "已兑换";
        }
        return "需要 " + cost + " 积分";
    }

    private Material stateMaterial(RoomState state) {
        return switch (state) {
            case DISABLED -> Material.BARRIER;
            case READY -> Material.GRAY_CONCRETE;
            case OPEN -> Material.LIME_CONCRETE;
            case WAITING -> Material.YELLOW_CONCRETE;
            case PLAYING -> Material.TARGET;
            case ENDED -> Material.FIREWORK_ROCKET;
            case RESETTING -> Material.CLOCK;
        };
    }

    private String stateLabel(RoomState state) {
        return switch (state) {
            case DISABLED -> "不可用";
            case READY -> "未开放";
            case OPEN -> "空闲";
            case WAITING -> "待白方";
            case PLAYING -> "进行中";
            case ENDED -> "已结束";
            case RESETTING -> "重置中";
        };
    }

    private enum ActionType {
        OPEN_ROOM,
        JOIN_ROOM,
        SPECTATE_ROOM,
        STATUS_ROOM,
        LEAVE,
        STATS,
        LEADERBOARD,
        REFRESH,
        REFRESH_ROOM,
        PREV_PAGE,
        NEXT_PAGE,
        BACK_TO_LOBBY,
        APPEARANCE_MENU,
        OPEN_THEMES,
        OPEN_SKINS,
        SELECT_THEME,
        SELECT_SKIN,
        BUY_THEME,
        BUY_SKIN,
        ADMIN_GUIDE,
        MAIN_MENU
    }

    private record GuiAction(ActionType type, String roomId, String value, int page) {
        private GuiAction(ActionType type, String roomId, int page) {
            this(type, roomId, "", page);
        }
    }

    private static final class MenuHolder implements InventoryHolder {
        private final Map<Integer, GuiAction> actions = new HashMap<>();
        private Inventory inventory;
        private int page;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
