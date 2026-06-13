package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private ArenaConfig config;
    private final MatchController match;
    private final BoardRenderer renderer;
    private final PieceAnimator animator;
    private final StatsService statsService;
    private final AppearanceCatalog appearanceCatalog;
    private final AppearanceUnlockService appearanceUnlocks;
    private final EnvironmentCatalog environmentCatalog;
    private final RoomChatService roomChat;
    private final InteractionFeedbackService feedback;
    private final Map<Stone, SeatLease> leases = new EnumMap<>(Stone.class);
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final List<MatchMove> moveHistory = new ArrayList<>();
    private MatchAppearance appearance;
    private boolean open;
    private boolean moveInProgress;
    private BukkitTask pendingReset;
    private BukkitTask pendingUndoTimeout;
    private UndoRequest pendingUndoRequest;
    private String activeMatchId;
    private String lastResult = "none";

    public GomokuRoom(
        JavaPlugin plugin,
        ArenaConfig config,
        StatsService statsService,
        AppearanceCatalog appearanceCatalog,
        AppearanceUnlockService appearanceUnlocks,
        EnvironmentCatalog environmentCatalog,
        RoomChatService roomChat
    ) {
        this.plugin = plugin;
        this.config = config;
        this.match = new MatchController(config.boardSize());
        this.statsService = statsService;
        this.appearanceCatalog = appearanceCatalog;
        this.appearanceUnlocks = appearanceUnlocks;
        this.environmentCatalog = environmentCatalog;
        this.roomChat = roomChat;
        this.renderer = new BoardRenderer(plugin, config, environmentTemplate());
        this.animator = new PieceAnimator(plugin, config);
        this.feedback = new InteractionFeedbackService(plugin, environmentTemplate().feedbackProfile());
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
        return spectators.contains(playerId);
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

    public Optional<UndoRequest> pendingUndoRequest() {
        return Optional.ofNullable(pendingUndoRequest);
    }

    public boolean canApprovePendingUndo(UUID playerId) {
        return pendingUndoRequest != null && pendingUndoRequest.approverId().equals(playerId);
    }

    public RoomEnvironmentTemplate environmentTemplate() {
        return environmentCatalog.resolve(config.environmentTemplateId());
    }

    public boolean protects(BlockPoint point) {
        return config.geometry().protects(point);
    }

    public void updateConfig(ArenaConfig updated) {
        this.config = updated;
        renderer.updateArena(updated, environmentTemplate());
        animator.updateArena(updated);
        feedback.updateProfile(environmentTemplate().feedbackProfile());
        renderer.renderEmpty(appearance.boardTheme());
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
        if (!spectators.contains(player.getUniqueId()) && config.spectatorCapacity() > 0 && spectators.size() >= config.spectatorCapacity()) {
            return "§c房间 " + config.id() + " 观众位已满。";
        }
        spectators.add(player.getUniqueId());
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
            String message = "§e你正在观战，不能落子。";
            player.sendMessage(message);
            feedback.rejected(player, config, cell, message);
            return;
        }
        if (!enabled()) {
            String message = "§c房间 " + config.id() + " 不可用: " + unavailableReason();
            player.sendMessage(message);
            feedback.rejected(player, config, cell, message);
            return;
        }
        if (moveInProgress) {
            String message = "§e上一枚棋子还在落子中，请稍等。";
            player.sendMessage(message);
            feedback.rejected(player, config, cell, message);
            return;
        }

        MoveResult result = match.play(player.getUniqueId(), cell.row(), cell.column());
        if (!result.accepted()) {
            String message = moveRejectionMessage(result.status());
            player.sendMessage(message);
            feedback.rejected(player, config, cell, message);
            return;
        }

        cancelPendingUndo("已有玩家继续落子，悔棋请求已取消。", true);
        moveInProgress = true;
        feedback.accepted(player, config, cell);
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
            feedback.landed(config, new GridCell(result.row(), result.column()));
            moveInProgress = false;
            broadcast("§f" + player.getName() + " §7落子 §e" + formatCell(result.row(), result.column()) + "§7。");
            if (result.winner() != Stone.EMPTY || result.draw()) {
                if (result.winner() != Stone.EMPTY) {
                    feedback.winningLine(config, result.winningLine());
                    renderer.animateWinningLine(result.winningLine(), skin);
                }
                finishScored(result.winner(), result.draw(), result.draw() ? "draw" : "win");
            } else {
                notifyTurn();
            }
        });
    }

    public String requestUndo(Player player) {
        expireDisconnectedLeases();
        if (!enabled()) {
            return "§c房间 " + config.id() + " 不可用: " + unavailableReason();
        }
        if (moveInProgress) {
            return "§e上一枚棋子还在落子中，暂时不能悔棋。";
        }
        if (match.state() == GameState.ENDED) {
            return "§e这局五子棋已经结束，不能悔棋。";
        }
        if (match.state() != GameState.PLAYING) {
            return "§e五子棋还没开始，不能悔棋。";
        }
        if (pendingUndoRequest != null) {
            return "§e房间已有待处理悔棋请求，请先等待对方处理。";
        }
        Stone side = match.sideFor(player.getUniqueId());
        if (side == Stone.EMPTY) {
            return "§e你不是房间 " + config.id() + " 的参赛者，不能申请悔棋。";
        }
        MatchMove lastMove = lastMove();
        if (lastMove == null) {
            return "§e当前还没有可悔棋的落子。";
        }
        if (!lastMove.playerId().equals(player.getUniqueId())) {
            return "§e只有最后落子方可以申请悔棋。";
        }
        UUID approverId = match.playerFor(side.opposite()).orElse(null);
        if (approverId == null || approverId.equals(player.getUniqueId())) {
            return "§e找不到可同意悔棋的对手。";
        }

        pendingUndoRequest = new UndoRequest(
            UUID.randomUUID(),
            player.getUniqueId(),
            player.getName(),
            approverId,
            lastMove.side(),
            lastMove.row(),
            lastMove.column(),
            lastMove.moveIndex(),
            System.currentTimeMillis(),
            config.undoRequestTimeoutTicks()
        );
        schedulePendingUndoTimeout(pendingUndoRequest);

        Player approver = Bukkit.getPlayer(approverId);
        broadcast("§e" + player.getName() + " 请求悔棋 §f" + formatCell(lastMove.row(), lastMove.column()) + "§e，等待对方同意。");
        if (approver != null) {
            approver.sendMessage("§e" + player.getName() + " 请求悔棋 §f" + formatCell(lastMove.row(), lastMove.column()) + "§e。输入 §f/gomoku undo accept " + config.id() + " §e同意，或 §f/gomoku undo deny " + config.id() + " §e拒绝。");
            approver.playSound(approver.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.75F, 1.2F);
        }
        return "";
    }

    public String acceptUndo(Player player) {
        UndoRequest request = pendingUndoRequest;
        if (request == null) {
            return "§e当前没有待处理的悔棋请求。";
        }
        if (request.requesterId().equals(player.getUniqueId())) {
            return "§e悔棋必须由对手同意，不能自己同意。";
        }
        if (!request.approverId().equals(player.getUniqueId())) {
            return "§e只有被请求的对手可以同意这次悔棋。";
        }
        if (moveInProgress) {
            return "§e上一枚棋子还在落子中，暂时不能处理悔棋。";
        }
        MatchMove lastMove = lastMove();
        if (!request.matches(lastMove)) {
            cancelPendingUndo("", false);
            return "§e悔棋请求已失效，请重新申请。";
        }

        cancelPendingUndo("", false);
        UndoResult result = match.undoLastMove(request.side(), request.row(), request.column());
        if (!result.accepted()) {
            return "§e悔棋失败: " + undoRejectionMessage(result.status());
        }
        moveHistory.remove(moveHistory.size() - 1);
        renderer.clearMove(request.row(), request.column(), appearance.boardTheme());
        feedback.undoCleared(roomPlayers(), config, request.cell());
        broadcast("§a" + player.getName() + " 已同意 " + request.requesterName() + " 悔棋，撤销 " + formatCell(request.row(), request.column()) + "。");
        notifyTurn();
        return "";
    }

    public String denyUndo(Player player) {
        UndoRequest request = pendingUndoRequest;
        if (request == null) {
            return "§e当前没有待处理的悔棋请求。";
        }
        if (request.requesterId().equals(player.getUniqueId())) {
            return "§e悔棋请求必须由对手处理，不能自己拒绝。";
        }
        if (!request.approverId().equals(player.getUniqueId())) {
            return "§e只有被请求的对手可以拒绝这次悔棋。";
        }
        cancelPendingUndo("", false);
        broadcast("§e" + player.getName() + " 已拒绝 " + request.requesterName() + " 的悔棋请求。");
        return "";
    }

    public void rejectInteraction(Player player, GridCell cell, String message) {
        if (message != null && !message.isBlank()) {
            player.sendMessage(message);
        }
        feedback.rejected(player, config, cell, message);
    }

    public void init() {
        reset(false, false);
    }

    public void refresh() {
        renderer.renderBoard(match.board(), appearance);
    }

    public void reset(boolean releaseSpectators, boolean announce) {
        cancelPendingReset();
        cancelPendingUndo("", false);
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
        cancelPendingUndo("房间被管理中止，悔棋请求已取消。", true);
        broadcast("§e房间被管理中止，本局不计入统计。");
        reset(false, false);
        lastResult = reason;
    }

    public void forfeit(Stone loser, String reason) {
        cancelPendingUndo("本局已判负结束，悔棋请求已取消。", true);
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
            "§7棋盘大小: §f" + config.boardSize() + "x" + config.boardSize(),
            "§7棋盘主题: §f" + appearance.boardTheme().displayName(),
            "§7黑方棋子: §f" + appearance.blackSkin().displayName(),
            "§7白方棋子: §f" + appearance.whiteSkin().displayName(),
            "§7观众: §f" + spectators.size() + "/" + config.spectatorCapacity(),
            "§7悔棋: §f" + undoInspectText(),
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
        };
    }

    public void shutdown() {
        cancelPendingReset();
        cancelPendingUndo("", false);
        animator.cleanup();
        renderer.cleanup();
        feedback.cleanup();
        roomChat.clearRoom(config.id(), false);
    }

    public void purgeBlocks() {
        cancelPendingReset();
        cancelPendingUndo("", false);
        moveInProgress = false;
        animator.cleanup();
        renderer.cleanup();
        feedback.cleanup();
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
        if (currentState == RoomState.PLAYING || currentState == RoomState.ENDED) {
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
        if (currentState == RoomState.PLAYING || currentState == RoomState.ENDED) {
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
        cancelPendingUndo("本局已结束，悔棋请求已取消。", true);
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
        cancelPendingUndo("", false);
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
        if (!spectators.remove(playerId)) {
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
        for (UUID playerId : List.copyOf(spectators)) {
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

    private String moveRejectionMessage(MoveStatus status) {
        return switch (status) {
            case WRONG_TURN -> "§e还没轮到你。当前回合: " + match.currentTurn().displayName();
            case OCCUPIED -> "§e这个格子已经有棋子。";
            case NOT_PLAYING -> "§e你不是房间 " + config.id() + " 的参赛者。";
            case NOT_STARTED -> "§e五子棋还没开始，需要两名玩家加入。";
            case ENDED -> "§e这局五子棋已经结束，请等待重置。";
            case OUT_OF_BOUNDS -> "§e这个位置不在棋盘内。";
            case ACCEPTED -> "";
        };
    }

    private String undoRejectionMessage(UndoStatus status) {
        return switch (status) {
            case OUT_OF_BOUNDS -> "棋盘位置无效";
            case NOT_STARTED -> "对局未开始";
            case ENDED -> "对局已结束";
            case INVALID_STONE -> "悔棋方无效";
            case EMPTY_CELL -> "目标格没有棋子";
            case STONE_MISMATCH -> "目标格棋子不匹配";
            case ACCEPTED -> "";
        };
    }

    private void notifyTurn() {
        UUID playerId = match.playerFor(match.currentTurn()).orElse(null);
        Player player = playerId == null ? null : Bukkit.getPlayer(playerId);
        Player waiting = participant(match.currentTurn().opposite());
        broadcast("§7当前回合: §f" + match.currentTurn().displayName());
        feedback.turnChanged(player, waiting, match.currentTurn());
        feedback.spectatorsTurn(onlineSpectators(), match.currentTurn());
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

        int centerIndex = config.boardSize() / 2;
        Location center = config.centeredLocation(config.geometry().boardPoint(centerIndex, centerIndex));
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
        feedback.startResetCountdown(roomPlayers(), resetDelaySeconds());
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
        feedback.cancelCountdown();
    }

    private void schedulePendingUndoTimeout(UndoRequest request) {
        if (pendingUndoTimeout != null) {
            pendingUndoTimeout.cancel();
        }
        pendingUndoTimeout = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (pendingUndoRequest == null || !pendingUndoRequest.requestId().equals(request.requestId())) {
                return;
            }
            pendingUndoRequest = null;
            pendingUndoTimeout = null;
            broadcast("§e" + request.requesterName() + " 的悔棋请求已超时。");
        }, request.timeoutTicks());
    }

    private void cancelPendingUndo(String message, boolean announce) {
        if (pendingUndoTimeout != null) {
            pendingUndoTimeout.cancel();
            pendingUndoTimeout = null;
        }
        UndoRequest request = pendingUndoRequest;
        pendingUndoRequest = null;
        if (announce && request != null && message != null && !message.isBlank()) {
            broadcast("§e" + message);
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

    private MatchMove lastMove() {
        return moveHistory.isEmpty() ? null : moveHistory.get(moveHistory.size() - 1);
    }

    private String undoInspectText() {
        if (pendingUndoRequest == null) {
            return "无";
        }
        return pendingUndoRequest.requesterName() + " -> " + formatCell(pendingUndoRequest.row(), pendingUndoRequest.column());
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

    private List<Player> onlineSpectators() {
        List<Player> players = new ArrayList<>();
        for (UUID playerId : spectators) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private List<Player> roomPlayers() {
        List<Player> players = new ArrayList<>();
        Player black = participant(Stone.BLACK);
        Player white = participant(Stone.WHITE);
        if (black != null) {
            players.add(black);
        }
        if (white != null) {
            players.add(white);
        }
        players.addAll(onlineSpectators());
        return players;
    }

    private int resetDelaySeconds() {
        return Math.max(1, config.autoResetTicks() / 20);
    }
}
