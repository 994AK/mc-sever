package net.leafmc.gomoku;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class InviteService {
    private static final long DEFAULT_TIMEOUT_TICKS = 600L;

    private final LeafGomokuPlugin plugin;
    private final Map<UUID, InviteRequest> pendingByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();

    public InviteService(LeafGomokuPlugin plugin) {
        this.plugin = plugin;
    }

    public String invite(Player inviter, String targetName, String roomId) {
        if (targetName == null || targetName.isBlank()) {
            return "§e用法: /gomoku invite <玩家> [room]";
        }
        Player target = plugin.getServer().getPlayerExact(targetName);
        if (target == null) {
            target = plugin.getServer().getPlayer(targetName);
        }
        if (target == null) {
            return "§c找不到在线玩家: " + targetName;
        }
        if (target.getUniqueId().equals(inviter.getUniqueId())) {
            return "§e不能邀请自己。";
        }
        if (!target.hasPermission(GomokuPermission.PLAY.node())) {
            return "§e对方没有五子棋游玩权限。";
        }
        Optional<GomokuRoom> active = plugin.rooms().participantRoom(inviter.getUniqueId());
        Optional<GomokuRoom> targetRoom = roomId == null || roomId.isBlank()
            ? active.isPresent() ? active : plugin.resolveRoom("")
            : plugin.resolveRoom(roomId);
        if (targetRoom.isEmpty()) {
            return "§c找不到五子棋房间: " + roomId;
        }
        if (active.isPresent() && active.get() != targetRoom.get()) {
            return "§e你正在房间 " + active.get().config().id() + " 参赛，只能邀请对方加入当前房间。";
        }
        RoomState state = targetRoom.get().state();
        if (state != RoomState.OPEN && state != RoomState.WAITING) {
            return "§e房间 " + targetRoom.get().config().id() + " 当前状态为 " + state + "，不能邀请加入。";
        }
        Optional<GomokuRoom> targetActive = plugin.rooms().participantRoom(target.getUniqueId());
        if (targetActive.isPresent()) {
            return "§e对方已经在房间 " + targetActive.get().config().id() + " 参赛。";
        }

        InviteRequest request = new InviteRequest(
            UUID.randomUUID(),
            inviter.getUniqueId(),
            inviter.getName(),
            target.getUniqueId(),
            target.getName(),
            targetRoom.get().config().id(),
            System.currentTimeMillis(),
            timeoutTicks()
        );
        replacePending(request);

        sendInvitePrompt(target, request);
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.75F, 1.25F);
        return "§a已邀请 " + target.getName() + " 加入房间 " + request.roomId() + "，有效期 " + Math.max(1L, request.timeoutTicks() / 20L) + " 秒。";
    }

    public String accept(Player target, String roomId) {
        InviteRequest request = pendingByTarget.get(target.getUniqueId());
        if (request == null) {
            return "§e当前没有待处理的五子棋邀请。";
        }
        if (!request.matchesRoom(roomId)) {
            return "§e当前待处理邀请是房间 " + request.roomId() + "。";
        }
        if (request.expired(System.currentTimeMillis())) {
            remove(request.targetId());
            return "§e这条五子棋邀请已经超时。";
        }
        String joinResult = plugin.join(target, request.roomId());
        remove(request.targetId());
        if (joinResult != null && !joinResult.isBlank()) {
            return joinResult;
        }
        Player inviter = plugin.getServer().getPlayer(request.inviterId());
        if (inviter != null) {
            inviter.sendMessage("§a" + target.getName() + " 已同意你的五子棋邀请，并加入房间 " + request.roomId() + "。");
        }
        return "§a已接受邀请，正在加入房间 " + request.roomId() + "。";
    }

    private void sendInvitePrompt(Player target, InviteRequest request) {
        String acceptCommand = "/gomoku invite accept " + request.roomId();
        String denyCommand = "/gomoku invite deny " + request.roomId();
        target.sendMessage("§6[五子棋邀请] §f" + request.inviterName() + " §7邀请你加入房间 §e" + request.roomId() + "§7。");
        target.sendMessage(
            Component.text("[同意]", NamedTextColor.GREEN)
                .hoverEvent(HoverEvent.showText(Component.text("点击同意五子棋邀请", NamedTextColor.GREEN)))
                .clickEvent(ClickEvent.runCommand(acceptCommand))
                .append(Component.text("  ", NamedTextColor.GRAY))
                .append(Component.text("[拒绝]", NamedTextColor.RED)
                    .hoverEvent(HoverEvent.showText(Component.text("点击拒绝五子棋邀请", NamedTextColor.RED)))
                    .clickEvent(ClickEvent.runCommand(denyCommand)))
                .append(Component.text("  " + acceptCommand + " / " + denyCommand, NamedTextColor.GRAY))
        );
    }

    public String deny(Player target, String roomId) {
        InviteRequest request = pendingByTarget.get(target.getUniqueId());
        if (request == null) {
            return "§e当前没有待处理的五子棋邀请。";
        }
        if (!request.matchesRoom(roomId)) {
            return "§e当前待处理邀请是房间 " + request.roomId() + "。";
        }
        remove(request.targetId());
        Player inviter = plugin.getServer().getPlayer(request.inviterId());
        if (inviter != null) {
            inviter.sendMessage("§e" + target.getName() + " 已拒绝你的五子棋邀请。");
        }
        return "§7已拒绝房间 " + request.roomId() + " 的五子棋邀请。";
    }

    public Optional<InviteRequest> pendingFor(UUID targetId) {
        InviteRequest request = pendingByTarget.get(targetId);
        if (request == null) {
            return Optional.empty();
        }
        if (request.expired(System.currentTimeMillis())) {
            remove(targetId);
            return Optional.empty();
        }
        return Optional.of(request);
    }

    public void clearFor(UUID playerId) {
        remove(playerId);
        for (InviteRequest request : new HashSet<>(pendingByTarget.values())) {
            if (request.inviterId().equals(playerId)) {
                remove(request.targetId());
            }
        }
    }

    public void clearAll() {
        for (BukkitTask task : new HashSet<>(timeoutTasks.values())) {
            task.cancel();
        }
        timeoutTasks.clear();
        pendingByTarget.clear();
    }

    private void replacePending(InviteRequest request) {
        remove(request.targetId());
        pendingByTarget.put(request.targetId(), request);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> expire(request), request.timeoutTicks());
        timeoutTasks.put(request.targetId(), task);
    }

    private void expire(InviteRequest request) {
        InviteRequest current = pendingByTarget.get(request.targetId());
        if (current == null || !current.requestId().equals(request.requestId())) {
            return;
        }
        remove(request.targetId());
        Player inviter = plugin.getServer().getPlayer(request.inviterId());
        if (inviter != null) {
            inviter.sendMessage("§e你发给 " + request.targetName() + " 的五子棋邀请已超时。");
        }
        Player target = plugin.getServer().getPlayer(request.targetId());
        if (target != null) {
            target.sendMessage("§e来自 " + request.inviterName() + " 的五子棋邀请已超时。");
        }
    }

    private void remove(UUID targetId) {
        pendingByTarget.remove(targetId);
        BukkitTask task = timeoutTasks.remove(targetId);
        if (task != null) {
            task.cancel();
        }
    }

    private long timeoutTicks() {
        long value = plugin.getConfig().getLong("invites.timeout-ticks", DEFAULT_TIMEOUT_TICKS);
        return value > 0L ? value : DEFAULT_TIMEOUT_TICKS;
    }
}
