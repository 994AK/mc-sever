package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class GomokuRoom {
    private final JavaPlugin plugin;
    private final ArenaConfig config;
    private final MatchController match = new MatchController();
    private final BoardRenderer renderer;
    private final PieceAnimator animator;
    private final StatsService statsService;
    private final AppearanceCatalog appearanceCatalog;
    private final AppearanceUnlockService appearanceUnlocks;
    private final RoomChatService roomChat;
    private final Map<Stone, SeatLease> leases = new EnumMap<>(Stone.class);
    private final Map<UUID, SpectatorSession> spectators = new LinkedHashMap<>();
    private final List<MatchMove> moveHistory = new ArrayList<>();
    private MatchAppearance appearance;
    private boolean open;
    private boolean moveInProgress;
    private BukkitTask pendingReset;
    private String activeMatchId;
    private String lastResult = "none";

    public GomokuRoom(
        JavaPlugin plugin,
        ArenaConfig config,
        StatsService statsService,
        AppearanceCatalog appearanceCatalog,
        AppearanceUnlockService appearanceUnlocks,
        RoomChatService roomChat
    ) {
        this.plugin = plugin;
        this.config = config;
        this.statsService = statsService;
        this.appearanceCatalog = appearanceCatalog;
        this.appearanceUnlocks = appearanceUnlocks;
        this.roomChat = roomChat;
        this.renderer = new BoardRenderer(plugin, config);
        this.animator = new PieceAnimator(plugin, config);
        this.appearance = defaultAppearance(appearanceCatalog.defaultBoardTheme());
        this.open = config.enabled() && config.openByDefault();
    }

    public ArenaConfig config() {
        return config;
    }

    public MatchController match() {
        return match;
    }

    public RoomState state() {
        if (!config.enabled()) {
            return RoomState.DISABLED;
        }
        return switch (match.state()) {
            case IDLE -> open ? RoomState.OPEN : RoomState.READY;
            case WAITING_FOR_WHITE -> RoomState.WAITING;
            case PLAYING -> RoomState.PLAYING;
            case ENDED -> RoomState.ENDED;
        };
    }

    public boolean enabled() {
        return config.enabled() && config.world() != null;
    }

    public boolean containsParticipant(UUID playerId) {
        return leases.values().stream().anyMatch(lease -> lease.playerId().equals(playerId));
    }

    public boolean containsSpectator(UUID playerId) {
        return spectators.containsKey(playerId);
    }

    public Optional<SeatLease> lease(Stone side) {
        return Optional.ofNullable(leases.get(side));
    }

    public int spectatorCount() {
        return spectators.size();
    }

    public String lastResult() {
        return lastResult;
    }

    public MatchAppearance appearance() {
        return appearance;
    }

    public void open() {
        open = true;
    }

    public void close() {
        open = false;
    }

    public String join(Player player) {
        expireDisconnectedLeases();
        if (!enabled()) {
            return "§c房间 " + config.id() + " 不可用: " + unavailableReason();
        }
        if (!open) {
            return "§e房间 " + config.id() + " 暂未开放加入。";
        }
        if (containsSpectator(player.getUniqueId())) {
            removeSpectator(player.getUniqueId(), false);
        }
        JoinResult result = match.join(player.getUniqueId());
        switch (result.status()) {
            case JOINED_BLACK -> {
                leases.put(Stone.BLACK, new SeatLease(Stone.BLACK, player.getUniqueId(), player.getName(), System.currentTimeMillis()));
                appearance = appearance.withSkin(Stone.BLACK, appearanceUnlocks.firstAvailableSkinExcept(player.getUniqueId(), null, Stone.BLACK));
                roomChat.enter(config.id(), player);
                teleportToSeat(player, Stone.BLACK);
                broadcast("§0" + player.getName() + " §7加入黑方，等待白方。棋子: §f" + appearance.blackSkin().displayName());
                player.sendTitle("§0黑方", "§7等待白方加入", 10, 50, 10);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8F, 0.9F);
                return "";
            }
            case JOINED_WHITE -> {
                leases.put(Stone.WHITE, new SeatLease(Stone.WHITE, player.getUniqueId(), player.getName(), System.currentTimeMillis()));
                appearance = appearance.withSkin(Stone.WHITE, appearanceUnlocks.firstAvailableSkinExcept(player.getUniqueId(), appearance.blackSkin(), Stone.WHITE));
                activeMatchId = config.id() + "-" + System.currentTimeMillis();
                roomChat.enter(config.id(), player);
                teleportToSeat(player, Stone.WHITE);
                broadcast("§f" + player.getName() + " §7加入白方。棋子: §f" + appearance.whiteSkin().displayName() + "§7。对局开始，黑方先手。");
                announceMatchStart();
                notifyTurn();
                return "";
            }
            case ALREADY_JOINED -> {
                if (result.side() != Stone.EMPTY) {
                    teleportToSeat(player, result.side());
                }
                return "§e你已经在房间 " + config.id() + " 的对局里。";
            }
            case MATCH_FULL -> {
                return "§c房间 " + config.id() + " 已有两名参赛者。";
            }
            case MATCH_ENDED -> {
                return "§e房间 " + config.id() + " 上一局已经结束，请等待重置。";
            }
        }
        return "";
    }

    public String spectate(Player player) {
        if (!enabled()) {
            return "§c房间 " + config.id() + " 不可用: " + unavailableReason();
        }
        if (containsParticipant(player.getUniqueId())) {
            return "§e你已经是房间 " + config.id() + " 的参赛者，不能同时观战。";
        }
        if (!spectators.containsKey(player.getUniqueId()) && config.spectatorCapacity() > 0 && spectators.size() >= config.spectatorCapacity()) {
            return "§c房间 " + config.id() + " 观众位已满。";
        }
        spectators.put(player.getUniqueId(), new SpectatorSession(player.getUniqueId(), player.getName(), System.currentTimeMillis(), player.getLocation()));
        roomChat.enter(config.id(), player);
        Location spawn = config.spectatorSpawnLocation();
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.sendTitle("§6观战", "§7房间 " + config.id(), 5, 35, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6F, 1.4F);
        broadcast("§7" + player.getName() + " 进入观战。当前观众: §f" + spectators.size());
        return "";
    }

    public boolean leave(Player player) {
        UUID playerId = player.getUniqueId();
        if (removeSpectator(playerId, true)) {
            player.sendMessage("§7已离开五子棋观战。");
            return true;
        }
        Stone side = match.sideFor(playerId);
        if (side == Stone.EMPTY) {
            return false;
        }
        if (match.state() == GameState.PLAYING) {
            forfeit(side, "leave");
            roomChat.leave(playerId, true);
        } else {
            clearSeatLease(side, false);
            broadcast("§e" + player.getName() + " 离开房间 " + config.id() + "，席位已释放。");
        }
        return true;
    }

    public void handleMove(Player player, GridCell cell) {
        expireDisconnectedLeases();
        if (containsSpectator(player.getUniqueId())) {
            player.sendMessage("§e你正在观战，不能落子。");
            return;
        }
        if (!enabled()) {
            player.sendMessage("§c房间 " + config.id() + " 不可用: " + unavailableReason());
            return;
        }
        if (moveInProgress) {
            player.sendMessage("§e上一枚棋子还在落子中，请稍等。");
            return;
        }

        MoveResult result = match.play(player.getUniqueId(), cell.row(), cell.column());
        if (!result.accepted()) {
            sendMoveRejection(player, result.status());
            return;
        }

        moveInProgress = true;
        PieceSkin skin = appearance.skinFor(result.stone());
        moveHistory.add(new MatchMove(
            moveHistory.size() + 1,
            player.getUniqueId(),
            player.getName(),
            result.stone(),
            result.row(),
            result.column(),
            System.currentTimeMillis()
        ));
        animator.animate(result.stone(), skin, result.row(), result.column(), () -> {
            renderer.renderMove(result.row(), result.column(), skin);
            moveInProgress = false;
            broadcast("§f" + player.getName() + " §7落子 §e" + formatCell(result.row(), result.column()) + "§7。");
            if (result.winner() != Stone.EMPTY || result.draw()) {
                if (result.winner() != Stone.EMPTY) {
                    renderer.animateWinningLine(result.winningLine(), skin);
                }
                finishScored(result.winner(), result.draw(), result.draw() ? "draw" : "win");
            } else {
                notifyTurn();
            }
        });
    }

    public void init() {
        reset(false, false);
    }

    public void refresh() {
        renderer.renderBoard(match.board(), appearance);
    }

    public void reset(boolean releaseSpectators, boolean announce) {
        cancelPendingReset();
        moveInProgress = false;
        animator.cleanup();
        if (announce) {
            broadcast("§7房间已重置。");
        }
        clearParticipantChannels();
        match.reset();
        leases.clear();
        moveHistory.clear();
        resetSeatSkins();
        activeMatchId = null;
        lastResult = "reset";
        if (releaseSpectators) {
            clearSpectators(true);
        }
        renderer.renderEmpty(appearance.boardTheme());
    }

    public String releaseSeat(Stone side, boolean force, boolean scoredForfeit) {
        if (side == Stone.EMPTY || !leases.containsKey(side)) {
            return "§e该席位没有参赛者。";
        }
        if (match.state() == GameState.PLAYING && !force && !scoredForfeit) {
            return "§c房间正在对局中，释放席位需要 stop/forfeit/reset。";
        }
        if (match.state() == GameState.PLAYING && scoredForfeit) {
            forfeit(side, "admin-forfeit");
            return "";
        }
        clearSeatLease(side, false);
        return "";
    }

    public void stopUnscored(String reason) {
        broadcast("§e房间被管理中止，本局不计入统计。");
        reset(false, false);
        lastResult = reason;
    }

    public void forfeit(Stone loser, String reason) {
        Stone winner = loser.opposite();
        finishScored(winner, false, reason);
    }

    public void markDisconnected(Player player) {
        SeatLease lease = leaseFor(player.getUniqueId()).orElse(null);
        if (lease == null) {
            return;
        }
        long recoveryMillis = Math.max(0L, config.disconnectRecoveryTicks()) * 50L;
        lease.markDisconnected(System.currentTimeMillis() + recoveryMillis);
        broadcast("§e" + player.getName() + " 掉线，席位保留 " + Math.max(0L, config.disconnectRecoveryTicks() / 20L) + " 秒。");
    }

    public void markOnline(Player player) {
        SeatLease lease = leaseFor(player.getUniqueId()).orElse(null);
        if (lease == null) {
            return;
        }
        lease.markOnline(player.getName());
        roomChat.enter(config.id(), player);
        teleportToSeat(player, lease.side());
        player.sendMessage("§a已回到五子棋房间 " + config.id() + " 的" + lease.side().displayName() + "席位。");
    }

    public Optional<SeatLease> leaseFor(UUID playerId) {
        return leases.values().stream().filter(lease -> lease.playerId().equals(playerId)).findFirst();
    }

    public List<String> inspectLines() {
        return List.of(
            "§6[五子棋] §e房间: §f" + config.id(),
            "§7状态: §f" + state() + " §7开放: §f" + open,
            "§7黑方: §f" + leaseText(Stone.BLACK),
            "§7白方: §f" + leaseText(Stone.WHITE),
            "§7当前回合: §f" + match.currentTurn().displayName(),
            "§7棋盘主题: §f" + appearance.boardTheme().displayName(),
            "§7黑方棋子: §f" + appearance.blackSkin().displayName(),
            "§7白方棋子: §f" + appearance.whiteSkin().displayName(),
            "§7观众: §f" + spectators.size() + "/" + config.spectatorCapacity(),
            "§7最后结果: §f" + lastResult,
            "§7配置: §f" + (config.enabled() ? "ok" : config.error())
        );
    }

    public String statusLine() {
        return switch (state()) {
            case DISABLED -> "§c房间 " + config.id() + " 不可用: " + unavailableReason();
            case READY -> "§7房间 §e" + config.id() + " §7已就绪但未开放。";
            case OPEN -> "§7房间 §e" + config.id() + " §7空闲。输入 §f/gomoku join " + config.id() + " §7加入黑方。";
            case WAITING -> "§7房间 §e" + config.id() + " §7等待白方加入。";
            case PLAYING -> "§7房间 §e" + config.id() + " §7进行中，当前回合: §f" + match.currentTurn().displayName();
            case ENDED -> match.draw()
                ? "§e房间 " + config.id() + " 已结束: 平局。"
                : "§a房间 " + config.id() + " 已结束: " + match.winner().displayName() + "获胜。";
            case RESETTING -> "§7房间 §e" + config.id() + " §7正在重置。";
        };
    }

    public void shutdown() {
        cancelPendingReset();
        animator.cleanup();
        renderer.cleanup();
        roomChat.clearRoom(config.id(), false);
    }

    public void purgeBlocks() {
        cancelPendingReset();
        moveInProgress = false;
        animator.cleanup();
        renderer.cleanup();
        clearParticipantChannels();
        clearSpectators(true);
        match.reset();
        leases.clear();
        moveHistory.clear();
        resetSeatSkins();
        activeMatchId = null;
        lastResult = "deleted";
        renderer.clearRoom();
        roomChat.clearRoom(config.id(), true);
    }

    public String selectBoardTheme(Player player, BoardTheme theme) {
        RoomState currentState = state();
        if (currentState == RoomState.PLAYING || currentState == RoomState.ENDED || currentState == RoomState.RESETTING) {
            return "§e房间正在对局或展示结果，棋盘主题会影响下一局，请等重置后再切换。";
        }
        if (!appearanceUnlocks.canUseTheme(player.getUniqueId(), theme)) {
            return "§e该棋盘主题需要先兑换: " + theme.displayName() + "，需要积分 " + theme.cost();
        }
        String preferenceResult = appearanceUnlocks.selectTheme(player.getUniqueId(), theme.id());
        if (!preferenceResult.isBlank()) {
            return preferenceResult;
        }
        appearance = appearance.withTheme(theme);
        renderer.renderEmpty(appearance.boardTheme());
        broadcast("§7" + player.getName() + " 将棋盘主题切换为 §f" + theme.displayName() + "§7。");
        return "§a已选择本房间棋盘主题: " + theme.displayName();
    }

    public String selectPieceSkin(Player player, PieceSkin skin) {
        Stone side = match.sideFor(player.getUniqueId());
        if (side == Stone.EMPTY) {
            return "§e你还不是房间 " + config.id() + " 的参赛者；已选择后会作为下一次加入的默认棋子。";
        }
        RoomState currentState = state();
        if (currentState == RoomState.PLAYING || currentState == RoomState.ENDED || currentState == RoomState.RESETTING) {
            return "§e本局已经开始，不能切换当前棋子皮肤。";
        }
        if (!appearanceUnlocks.canUseSkin(player.getUniqueId(), skin)) {
            return "§e该棋子皮肤需要先兑换: " + skin.displayName() + "，需要积分 " + skin.cost();
        }
        PieceSkin other = appearance.skinFor(side.opposite());
        if (appearanceUnlocks.sameMaterial(skin, other)) {
            return "§e本局对方已经使用这个棋子外观，请换一个。";
        }
        String preferenceResult = appearanceUnlocks.selectSkin(player.getUniqueId(), skin.id());
        if (!preferenceResult.isBlank()) {
            return preferenceResult;
        }
        appearance = appearance.withSkin(side, skin);
        broadcast("§7" + player.getName() + " 将" + side.displayName() + "棋子切换为 §f" + skin.displayName() + "§7。");
        return "§a已选择本局棋子皮肤: " + skin.displayName();
    }

    private void finishScored(Stone winner, boolean draw, String reason) {
        if (match.state() != GameState.ENDED) {
            match.forceEnd(winner, draw);
        }
        lastResult = draw ? "draw" : winner.displayName() + ":" + reason;
        if (winner != Stone.EMPTY) {
            broadcast("§a" + winner.displayName() + "获胜！§7棋盘将在 " + resetDelaySeconds() + " 秒后自动重置。");
            celebrateWin(winner);
        } else {
            broadcast("§e平局。§7棋盘将在 " + resetDelaySeconds() + " 秒后自动重置。");
        }
        recordStats(winner, draw, reason);
        scheduleAutoReset("§7展示结束，房间 " + config.id() + " 已自动重置。");
    }

    private void recordStats(Stone winner, boolean draw, String reason) {
        SeatLease black = leases.get(Stone.BLACK);
        SeatLease white = leases.get(Stone.WHITE);
        if (black == null || white == null) {
            return;
        }
        String matchId = activeMatchId == null ? config.id() + "-" + System.currentTimeMillis() : activeMatchId;
        statsService.record(new MatchRecord(
            matchId,
            config.id(),
            black.playerId(),
            black.playerName(),
            white.playerId(),
            white.playerName(),
            winner,
            draw,
            reason,
            System.currentTimeMillis()
        ), List.copyOf(moveHistory));
    }

    private void clearSeatLease(Stone side, boolean render) {
        boolean resetAllLeases = match.state() == GameState.PLAYING || match.state() == GameState.ENDED;
        SeatLease removed = leases.remove(side);
        if (removed != null) {
            roomChat.leave(removed.playerId(), true);
        }
        if (resetAllLeases) {
            clearParticipantChannels();
            leases.clear();
        }
        match.reset();
        moveHistory.clear();
        resetSeatSkins();
        activeMatchId = null;
        if (render) {
            renderer.renderEmpty(appearance.boardTheme());
        }
    }

    private boolean removeSpectator(UUID playerId, boolean teleportExit) {
        SpectatorSession session = spectators.remove(playerId);
        if (session == null) {
            return false;
        }
        if (teleportExit) {
            Player player = Bukkit.getPlayer(playerId);
            Location exit = config.spectatorExitLocation();
            if (player != null && exit != null) {
                player.teleport(exit);
            }
        }
        roomChat.leave(playerId, teleportExit);
        return true;
    }

    private void clearSpectators(boolean teleportExit) {
        for (UUID playerId : List.copyOf(spectators.keySet())) {
            removeSpectator(playerId, teleportExit);
        }
    }

    private void expireDisconnectedLeases() {
        long now = System.currentTimeMillis();
        for (SeatLease lease : List.copyOf(leases.values())) {
            if (!lease.recoveryExpired(now)) {
                continue;
            }
            if (match.state() == GameState.PLAYING && config.forfeitOnDisconnectTimeout()) {
                forfeit(lease.side(), "disconnect-timeout");
            } else {
                clearSeatLease(lease.side(), true);
                broadcast("§e" + lease.playerName() + " 的席位因掉线超时已释放。");
            }
            break;
        }
    }

    private void sendMoveRejection(Player player, MoveStatus status) {
        switch (status) {
            case WRONG_TURN -> player.sendMessage("§e还没轮到你。当前回合: " + match.currentTurn().displayName());
            case OCCUPIED -> player.sendMessage("§e这个格子已经有棋子。");
            case NOT_PLAYING -> player.sendMessage("§e你不是房间 " + config.id() + " 的参赛者。");
            case NOT_STARTED -> player.sendMessage("§e五子棋还没开始，需要两名玩家加入。");
            case ENDED -> player.sendMessage("§e这局五子棋已经结束，请等待重置。");
            case OUT_OF_BOUNDS -> player.sendMessage("§e这个位置不在棋盘内。");
            case ACCEPTED -> {
            }
        }
    }

    private void notifyTurn() {
        UUID playerId = match.playerFor(match.currentTurn()).orElse(null);
        Player player = playerId == null ? null : Bukkit.getPlayer(playerId);
        broadcast("§7当前回合: §f" + match.currentTurn().displayName());
        if (player != null) {
            player.sendMessage("§a轮到你落子，右键点击棋盘空格。");
            player.sendTitle("§a轮到你", "§7右键点击棋盘空格落子", 5, 35, 10);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, 1.2F);
        }
    }

    private void announceMatchStart() {
        Player black = participant(Stone.BLACK);
        Player white = participant(Stone.WHITE);
        if (black != null) {
            black.sendTitle("§6对局开始", "§0你是黑方，先手", 10, 50, 10);
            black.playSound(black.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9F, 0.8F);
        }
        if (white != null) {
            white.sendTitle("§6对局开始", "§f你是白方，后手", 10, 50, 10);
            white.playSound(white.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9F, 1.1F);
        }
    }

    private Player participant(Stone side) {
        UUID playerId = match.playerFor(side).orElse(null);
        return playerId == null ? null : Bukkit.getPlayer(playerId);
    }

    private void teleportToSeat(Player player, Stone side) {
        Location seat = config.seatLocation(side);
        if (seat != null) {
            player.teleport(seat);
            player.sendMessage("§7已传送到房间 " + config.id() + " 的" + side.displayName() + "位置。");
        }
    }

    private void celebrateWin(Stone winner) {
        Player winnerPlayer = participant(winner);
        if (winnerPlayer != null) {
            winnerPlayer.sendTitle("§a胜利", "§7" + winner.displayName() + "五连成功", 10, 70, 20);
        }
        Player loser = participant(winner.opposite());
        if (loser != null) {
            loser.sendTitle("§c落败", "§7" + winner.displayName() + "五连成功", 10, 70, 20);
        }

        Location center = config.centeredLocation(config.geometry().boardPoint(7, 7));
        if (center == null || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        int count = Math.max(0, config.fireworkCount());
        for (int index = 0; index < count; index++) {
            int offsetIndex = index;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location fireworkLocation = center.clone().add((offsetIndex % 3 - 1) * 2.0D, 2.0D, (offsetIndex / 3) * 2.0D);
                LeafGomokuPlugin.spawnFirework(fireworkLocation, winner);
            }, index * 12L);
        }
        world.spawnParticle(Particle.FIREWORK, center.clone().add(0.0D, 1.0D, 0.0D), 40, 1.2D, 0.8D, 1.2D, 0.05D);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
    }

    private void scheduleAutoReset(String message) {
        if (config.autoResetTicks() <= 0) {
            return;
        }
        cancelPendingReset();
        pendingReset = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingReset = null;
            broadcast(message);
            reset(true, false);
        }, config.autoResetTicks());
    }

    private void cancelPendingReset() {
        if (pendingReset != null) {
            pendingReset.cancel();
            pendingReset = null;
        }
    }

    private void broadcast(String message) {
        roomChat.broadcast(config.id(), message);
    }

    private void clearParticipantChannels() {
        for (SeatLease lease : List.copyOf(leases.values())) {
            roomChat.leave(lease.playerId(), true);
        }
    }

    private void resetSeatSkins() {
        appearance = defaultAppearance(appearance.boardTheme());
    }

    private MatchAppearance defaultAppearance(BoardTheme theme) {
        return new MatchAppearance(theme, appearanceCatalog.defaultBlackSkin(), appearanceCatalog.defaultWhiteSkin());
    }

    private String leaseText(Stone side) {
        SeatLease lease = leases.get(side);
        if (lease == null) {
            return "空";
        }
        return lease.playerName() + (lease.disconnected() ? " (掉线保留)" : "");
    }

    private String unavailableReason() {
        if (!config.enabled()) {
            return config.error();
        }
        if (config.world() == null) {
            return "世界未加载: " + config.worldName();
        }
        return "未知原因";
    }

    private String formatCell(int row, int column) {
        return "(" + (row + 1) + ", " + (column + 1) + ")";
    }

    private int resetDelaySeconds() {
        return Math.max(1, config.autoResetTicks() / 20);
    }
}
