package net.leafmc.gomoku;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeafGomokuPlugin extends JavaPlugin {
    private RoomConfigRepository roomConfigRepository;
    private RoomLayoutFactory roomLayoutFactory;
    private AppearanceCatalog appearanceCatalog;
    private EnvironmentCatalog environmentCatalog;
    private AppearanceUnlockService appearanceUnlocks;
    private RoomChatService roomChatService;
    private InviteService inviteService;
    private PlacementToolService placementToolService;
    private WorldEditPreviewService worldEditPreviewService;
    private StatsService statsService;
    private RoomRegistry roomRegistry;
    private VariableService variableService;
    private GomokuGui gomokuGui;
    private Object placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        appearanceCatalog = AppearanceCatalog.load(getConfig(), getLogger());
        environmentCatalog = EnvironmentCatalog.load(getConfig(), getLogger());
        statsService = new StatsService(this);
        statsService.load();
        appearanceUnlocks = new AppearanceUnlockService(appearanceCatalog, statsService);
        roomChatService = new RoomChatService(this);
        inviteService = new InviteService(this);
        worldEditPreviewService = new WorldEditPreviewService(this);
        placementToolService = new PlacementToolService(this);
        roomConfigRepository = new RoomConfigRepository(this);
        roomLayoutFactory = new RoomLayoutFactory();
        roomRegistry = new RoomRegistry(this, statsService, appearanceCatalog, appearanceUnlocks, environmentCatalog, roomChatService);
        reloadRooms();
        runStartupCleanupIfRequested();
        variableService = new VariableService(roomRegistry, statsService);
        gomokuGui = new GomokuGui(this);

        GomokuCommand command = new GomokuCommand(this);
        PluginCommand pluginCommand = getCommand("gomoku");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(new BoardListener(this), this);
        getServer().getPluginManager().registerEvents(roomChatService, this);
        getServer().getPluginManager().registerEvents(gomokuGui, this);
        registerPlaceholderExpansion();
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.getClass().getMethod("unregister").invoke(placeholderExpansion);
            } catch (ReflectiveOperationException ignored) {
            }
            placeholderExpansion = null;
        }
        if (roomRegistry != null) {
            roomRegistry.shutdown();
        }
        if (roomChatService != null) {
            roomChatService.clearAll();
        }
        if (inviteService != null) {
            inviteService.clearAll();
        }
        if (statsService != null) {
            statsService.close();
        }
    }

    public RoomRegistry rooms() {
        return roomRegistry;
    }

    public StatsService stats() {
        return statsService;
    }

    public AppearanceCatalog appearances() {
        return appearanceCatalog;
    }

    public AppearanceUnlockService appearanceUnlocks() {
        return appearanceUnlocks;
    }

    public EnvironmentCatalog environments() {
        return environmentCatalog;
    }

    public PlacementToolService placementTool() {
        return placementToolService;
    }

    public InviteService invites() {
        return inviteService;
    }

    public WorldEditPreviewService worldEditPreview() {
        return worldEditPreviewService;
    }

    public VariableService variables() {
        return variableService;
    }

    public GomokuGui gui() {
        return gomokuGui;
    }

    public void reloadAll() {
        reloadConfig();
        appearanceCatalog = AppearanceCatalog.load(getConfig(), getLogger());
        environmentCatalog = EnvironmentCatalog.load(getConfig(), getLogger());
        statsService.load();
        appearanceUnlocks = new AppearanceUnlockService(appearanceCatalog, statsService);
        if (roomRegistry != null) {
            roomRegistry.shutdown();
        }
        roomRegistry = new RoomRegistry(this, statsService, appearanceCatalog, appearanceUnlocks, environmentCatalog, roomChatService);
        reloadRooms();
        variableService = new VariableService(roomRegistry, statsService);
    }

    public void reloadRooms() {
        List<ArenaConfig> configs = roomConfigRepository.loadRooms(getConfig());
        roomRegistry.replaceAll(configs);
        if (configs.isEmpty()) {
            getLogger().warning("LeafGomoku loaded with no rooms. Create one with /gomoku admin create <id>.");
        }
        for (ArenaConfig config : configs) {
            if (!config.enabled()) {
                getLogger().warning("LeafGomoku room '" + config.id() + "' disabled: " + config.error());
            }
        }
    }

    private void runStartupCleanupIfRequested() {
        File flag = new File(getDataFolder(), "cleanup-old-rooms.flag");
        if (!flag.isFile()) {
            return;
        }
        List<GomokuRoom> oldRooms = List.copyOf(roomRegistry.rooms());
        for (GomokuRoom room : oldRooms) {
            String id = room.config().id();
            getLogger().info("Cleaning old LeafGomoku room blocks and config: " + id);
            room.purgeBlocks();
            roomRegistry.remove(id);
            roomConfigRepository.deleteRoom(id);
        }
        if (!flag.delete()) {
            getLogger().warning("Could not delete cleanup flag: " + flag.getAbsolutePath());
        }
        getLogger().info("LeafGomoku old room cleanup finished. Rooms removed: " + oldRooms.size());
    }

    public String join(Player player, String roomId) {
        Optional<GomokuRoom> target = resolveRoom(roomId);
        if (target.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        Optional<GomokuRoom> active = roomRegistry.participantRoom(player.getUniqueId());
        if (active.isPresent() && active.get() != target.get()) {
            return "§e你已经在房间 " + active.get().config().id() + " 参赛，先 /gomoku leave。";
        }
        roomRegistry.spectatorRoom(player.getUniqueId()).ifPresent(room -> room.leave(player));
        return target.get().join(player);
    }

    public String spectate(Player player, String roomId) {
        Optional<GomokuRoom> target = resolveRoom(roomId);
        if (target.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        Optional<GomokuRoom> active = roomRegistry.participantRoom(player.getUniqueId());
        if (active.isPresent()) {
            return "§e你已经在房间 " + active.get().config().id() + " 参赛，不能观战。";
        }
        roomRegistry.spectatorRoom(player.getUniqueId()).ifPresent(room -> room.leave(player));
        return target.get().spectate(player);
    }

    public String leave(Player player) {
        Optional<GomokuRoom> room = roomRegistry.participantRoom(player.getUniqueId());
        if (room.isPresent()) {
            return room.get().leave(player) ? "" : "§e你不在五子棋房间里。";
        }
        room = roomRegistry.spectatorRoom(player.getUniqueId());
        if (room.isPresent()) {
            return room.get().leave(player) ? "" : "§e你不在五子棋房间里。";
        }
        return "§e你不在五子棋房间里。";
    }

    public String invite(Player inviter, String targetName, String roomId) {
        return inviteService.invite(inviter, targetName, roomId);
    }

    public String acceptInvite(Player target, String roomId) {
        return inviteService.accept(target, roomId);
    }

    public String denyInvite(Player target, String roomId) {
        return inviteService.deny(target, roomId);
    }

    public String requestUndo(Player player, String roomId) {
        Optional<GomokuRoom> room = resolvePlayerRoom(player, roomId);
        if (room.isEmpty()) {
            return roomId == null || roomId.isBlank()
                ? "§e你不在五子棋对局里。"
                : "§c找不到五子棋房间: " + roomId;
        }
        return room.get().requestUndo(player);
    }

    public String acceptUndo(Player player, String roomId) {
        Optional<GomokuRoom> room = resolvePlayerRoom(player, roomId);
        if (room.isEmpty()) {
            return roomId == null || roomId.isBlank()
                ? "§e你不在五子棋对局里。"
                : "§c找不到五子棋房间: " + roomId;
        }
        return room.get().acceptUndo(player);
    }

    public String denyUndo(Player player, String roomId) {
        Optional<GomokuRoom> room = resolvePlayerRoom(player, roomId);
        if (room.isEmpty()) {
            return roomId == null || roomId.isBlank()
                ? "§e你不在五子棋对局里。"
                : "§c找不到五子棋房间: " + roomId;
        }
        return room.get().denyUndo(player);
    }

    public void handleMove(GomokuRoom room, Player player, GridCell cell) {
        room.handleMove(player, cell);
    }

    public Optional<GomokuRoom> resolveRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return roomRegistry.defaultRoom();
        }
        return roomRegistry.room(roomId);
    }

    private Optional<GomokuRoom> resolvePlayerRoom(Player player, String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return roomRegistry.participantRoom(player.getUniqueId());
        }
        return resolveRoom(roomId);
    }

    public String statusLine(String roomId) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        return room.map(GomokuRoom::statusLine).orElse("§c找不到五子棋房间: " + roomId);
    }

    public String createRoom(Player admin, String roomId) {
        return createRoomAt(admin, roomId, admin.getLocation(), GomokuBoard.DEFAULT_SIZE);
    }

    public String createRoomAt(Player admin, String roomId, Location anchor) {
        return createRoomAt(admin, roomId, anchor, GomokuBoard.DEFAULT_SIZE);
    }

    public String createRoomAt(Player admin, String roomId, Location anchor, int boardSize) {
        return createRoomAt(admin, roomId, anchor, boardSize, "");
    }

    public String createRoomAt(Player admin, String roomId, Location anchor, String templateId) {
        return createRoomAt(admin, roomId, anchor, GomokuBoard.DEFAULT_SIZE, templateId);
    }

    public String createRoomAt(Player admin, String roomId, Location anchor, int boardSize, String templateId) {
        String normalized = ArenaConfig.normalizeRoomId(roomId);
        if (!ArenaConfig.isValidRoomId(normalized)) {
            return "§c房间 id 只能使用小写字母、数字、下划线或短横线，最长 32 位。";
        }
        if (!GomokuBoard.isValidSize(boardSize)) {
            return boardSizeError();
        }
        if (roomRegistry.room(normalized).isPresent()) {
            return "§c房间已存在: " + normalized;
        }
        ArenaConfig config;
        try {
            config = createRoomConfig(normalized, anchor, boardSize, templateId);
        } catch (IllegalArgumentException error) {
            return "§c" + error.getMessage();
        }
        String saved = saveRoomConfig(config);
        return saved.isBlank()
            ? "§a已创建房间 " + normalized + "，棋盘大小 §f" + config.boardSize() + "x" + config.boardSize() + "§a，使用锚点 Y=" + anchor.getBlockY() + " 和你的面朝方向生成布局。"
            : saved;
    }

    public String prepareRoomPlacement(Player admin, String roomId) {
        return placementToolService.begin(admin, roomId, GomokuBoard.DEFAULT_SIZE);
    }

    public String prepareRoomPlacement(Player admin, String roomId, int boardSize) {
        return placementToolService.begin(admin, roomId, boardSize);
    }

    public String prepareRoomPlacement(Player admin, String roomId, int boardSize, String templateId) {
        return placementToolService.begin(admin, roomId, boardSize, templateId);
    }

    public String prepareRoomPlacement(Player admin, String roomId, String templateId) {
        return placementToolService.begin(admin, roomId, GomokuBoard.DEFAULT_SIZE, templateId);
    }

    public String confirmRoomPlacement(Player admin) {
        return placementToolService.confirm(admin);
    }

    public String cancelRoomPlacement(Player admin) {
        return placementToolService.cancel(admin);
    }

    public String setRoomEnvironment(String roomId, String templateId, boolean force) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        if (room.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        Optional<RoomEnvironmentTemplate> template = resolveEnvironmentTemplate(templateId);
        if (template.isEmpty()) {
            return "§c找不到环境模板: " + templateId;
        }
        RoomState state = room.get().state();
        if (state != RoomState.OPEN && state != RoomState.READY && !force) {
            return "§c房间当前状态为 " + state + "，切换环境模板请先重置，或使用 force。";
        }
        if (force && (state == RoomState.WAITING || state == RoomState.PLAYING || state == RoomState.ENDED)) {
            room.get().stopUnscored("admin-environment-change");
        }
        ArenaConfig updated = room.get().config().withEnvironmentTemplateId(template.get().id());
        roomConfigRepository.saveRoom(updated);
        room.get().updateConfig(updated);
        return "§a房间 " + updated.id() + " 已切换环境模板: §f" + template.get().displayName();
    }

    public String selectBoardTheme(Player player, String roomId, String themeId) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        if (room.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        Optional<BoardTheme> theme = appearanceCatalog.boardTheme(themeId);
        if (theme.isEmpty()) {
            return "§c找不到棋盘主题: " + themeId;
        }
        return room.get().selectBoardTheme(player, theme.get());
    }

    public String selectPieceSkin(Player player, String skinId) {
        Optional<PieceSkin> skin = appearanceCatalog.pieceSkin(skinId);
        if (skin.isEmpty()) {
            return "§c找不到棋子皮肤: " + skinId;
        }
        Optional<GomokuRoom> active = roomRegistry.participantRoom(player.getUniqueId());
        if (active.isPresent()) {
            return active.get().selectPieceSkin(player, skin.get());
        }
        String result = appearanceUnlocks.selectSkin(player.getUniqueId(), skin.get().id());
        return result.isBlank() ? "§a已选择默认棋子皮肤: " + skin.get().displayName() : result;
    }

    public String buyAppearance(Player player, String type, String appearanceId) {
        return buyAppearance(player, type, appearanceId, "");
    }

    public String buyAppearance(Player player, String type, String appearanceId, String roomId) {
        String normalizedType = type == null ? "" : type.toLowerCase(Locale.ROOT);
        return switch (normalizedType) {
            case "theme", "board", "board_theme", "棋盘" -> {
                String result = appearanceUnlocks.purchaseTheme(player.getUniqueId(), player.getName(), appearanceId);
                if (result.startsWith("§a") && roomId != null && !roomId.isBlank()) {
                    yield result + "\n" + selectBoardTheme(player, roomId, appearanceId);
                }
                yield result;
            }
            case "skin", "piece", "piece_skin", "棋子" -> {
                String result = appearanceUnlocks.purchaseSkin(player.getUniqueId(), player.getName(), appearanceId);
                if (result.startsWith("§a") && roomRegistry.participantRoom(player.getUniqueId()).isPresent()) {
                    yield result + "\n" + selectPieceSkin(player, appearanceId);
                }
                yield result;
            }
            default -> "§e用法: /gomoku buy <theme|skin> <id>";
        };
    }

    public String initRoom(String roomId) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        if (room.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        room.get().init();
        return "§a房间 " + room.get().config().id() + " 已初始化。";
    }

    public String refreshRoom(String roomId) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        if (room.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        room.get().refresh();
        return "§a房间 " + room.get().config().id() + " 已刷新渲染。";
    }

    public String resetRoom(String roomId, boolean force) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        if (room.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        RoomState state = room.get().state();
        if ((state == RoomState.PLAYING || state == RoomState.ENDED) && !force) {
            return "§c房间正在对局或展示结果，重置请使用 force 并具备强制权限。";
        }
        room.get().reset(true, true);
        return "§a房间 " + room.get().config().id() + " 已重置。";
    }

    public String deleteRoom(String roomId, boolean force) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        if (room.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        RoomState state = room.get().state();
        if ((state == RoomState.WAITING || state == RoomState.PLAYING || state == RoomState.ENDED) && !force) {
            return "§c房间存在活跃状态，删除请使用 force 并具备强制权限。";
        }
        String id = room.get().config().id();
        room.get().purgeBlocks();
        roomRegistry.remove(id);
        roomConfigRepository.deleteRoom(id);
        return "§a房间 " + id + " 已删除。";
    }

    public String setOpen(String roomId, boolean open) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        if (room.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        if (open) {
            room.get().open();
        } else {
            room.get().close();
        }
        return "§a房间 " + room.get().config().id() + (open ? " 已开放加入。" : " 已关闭新加入。");
    }

    public String releaseSeat(String roomId, Stone side, boolean force, boolean scoredForfeit) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        if (room.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        String result = room.get().releaseSeat(side, force, scoredForfeit);
        return result.isBlank() ? "§a席位操作完成。" : result;
    }

    public String stopRoom(String roomId) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        if (room.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        room.get().stopUnscored("admin-stop");
        return "§a房间 " + room.get().config().id() + " 已中止，本局不计入统计。";
    }

    public List<String> inspectRoom(String roomId) {
        Optional<GomokuRoom> room = resolveRoom(roomId);
        return room.map(GomokuRoom::inspectLines).orElse(List.of("§c找不到五子棋房间: " + roomId));
    }

    public String leaderboardLine(String metric) {
        List<PlayerStats> rows = statsService.leaderboard(metric, 10);
        if (rows.isEmpty()) {
            return "§e暂无五子棋排行榜数据。";
        }
        StringBuilder builder = new StringBuilder("§6[五子棋排行榜] §7").append(metric == null ? "points" : metric.toLowerCase(Locale.ROOT));
        for (int index = 0; index < rows.size(); index++) {
            PlayerStats row = rows.get(index);
            builder.append("\n§e").append(index + 1).append(". §f")
                .append(row.playerName())
                .append(" §7积分 ").append(row.points())
                .append(" 胜 ").append(row.wins())
                .append(" 胜率 ").append(String.format(Locale.ROOT, "%.1f%%", row.winRate() * 100.0D));
        }
        return builder.toString();
    }

    public String statsLine(OfflinePlayer player) {
        PlayerStats stats = statsService.statsFor(player);
        return "§6[五子棋统计] §f" + stats.playerName()
            + " §7积分 §e" + stats.points()
            + " §7局数 §e" + stats.games()
            + " §7胜/负/平 §e" + stats.wins() + "/" + stats.losses() + "/" + stats.draws()
            + " §7连胜 §e" + stats.currentStreak()
            + " §7最高 §e" + stats.bestStreak();
    }

    public String resetStats(OfflinePlayer player) {
        statsService.resetPlayer(player.getUniqueId());
        return "§a已重置 " + (player.getName() == null ? player.getUniqueId() : player.getName()) + " 的五子棋统计。";
    }

    public String addPoints(OfflinePlayer player, int amount, String reason) {
        PlayerStats stats = statsService.addPoints(player, amount, reason);
        return "§a已给 " + stats.playerName() + " 增加 " + amount + " 五子棋积分，当前积分: " + stats.points();
    }

    private Optional<RoomEnvironmentTemplate> resolveEnvironmentTemplate(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return Optional.of(environmentCatalog.defaultTemplate());
        }
        return environmentCatalog.template(templateId);
    }

    ArenaConfig createRoomConfig(String roomId, Location anchor, int boardSize, String templateId) {
        RoomEnvironmentTemplate template = resolveEnvironmentTemplate(templateId).orElse(null);
        if (template == null) {
            throw new IllegalArgumentException("找不到环境模板: " + templateId);
        }
        ArenaConfig defaults = roomRegistry.defaultRoom().map(GomokuRoom::config).orElse(ArenaConfig.load(getConfig()));
        return roomLayoutFactory.create(roomId, anchor, defaults, boardSize, template.id());
    }

    String saveRoomConfig(ArenaConfig config) {
        if (!config.enabled()) {
            return "§c房间配置不可用: " + config.error();
        }
        if (roomRegistry.room(config.id()).isPresent()) {
            return "§c房间已存在: " + config.id();
        }
        roomConfigRepository.saveRoom(config);
        roomRegistry.put(config);
        return "";
    }

    String boardSizeError() {
        return "§e棋盘大小必须是 " + GomokuBoard.MIN_SIZE + "-" + GomokuBoard.MAX_SIZE + " 之间的整数，例如 size 15 或 size 19。";
    }

    public void markDisconnected(Player player) {
        if (inviteService != null) {
            inviteService.clearFor(player.getUniqueId());
        }
        roomRegistry.markDisconnected(player);
        if (roomRegistry.participantRoom(player.getUniqueId()).isEmpty()) {
            roomRegistry.spectatorRoom(player.getUniqueId()).ifPresent(room -> room.leave(player));
        }
    }

    public void markOnline(Player player) {
        roomRegistry.markOnline(player);
    }

    public static void spawnFirework(Location location, Stone winner) {
        if (location.getWorld() == null) {
            return;
        }
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        Color primary = winner == Stone.BLACK ? Color.fromRGB(25, 25, 25) : Color.WHITE;
        Color fade = winner == Stone.BLACK ? Color.GRAY : Color.fromRGB(255, 230, 120);
        meta.addEffect(FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL_LARGE)
            .withColor(primary)
            .withFade(fade)
            .withTrail()
            .withFlicker()
            .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    @SuppressWarnings("deprecation")
    public OfflinePlayer offlinePlayer(String nameOrUuid) {
        try {
            UUID uuid = UUID.fromString(nameOrUuid);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            return Bukkit.getOfflinePlayer(nameOrUuid);
        }
    }

    private void registerPlaceholderExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        GomokuPlaceholderExpansion expansion = new GomokuPlaceholderExpansion(this, variableService);
        expansion.register();
        placeholderExpansion = expansion;
    }
}
