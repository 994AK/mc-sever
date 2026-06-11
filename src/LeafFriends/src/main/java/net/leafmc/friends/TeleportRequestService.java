package net.leafmc.friends;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TeleportRequestService {
    public enum Status {
        OK,
        SELF,
        NOT_FRIENDS,
        NOT_ONLINE,
        BLOCKED,
        TELEPORTS_DISABLED,
        WORLD_BLOCKED,
        REQUEST_PENDING,
        NO_REQUEST,
        REQUEST_EXPIRED,
        COOLDOWN
    }

    public record Result(Status status, String message, UUID sender, UUID target) {
        public boolean ok() {
            return status == Status.OK;
        }
    }

    private record RequestKey(UUID sender, UUID target) {
    }

    private record TeleportRequest(UUID sender, UUID target, long createdAtMillis, long expiresAtMillis) {
        boolean expired(long nowMillis) {
            return nowMillis >= expiresAtMillis;
        }
    }

    private final FriendService friendService;
    private final TimeSource timeSource;
    private final Map<RequestKey, TeleportRequest> requests = new HashMap<>();
    private final Map<RequestKey, Long> lastAttempts = new HashMap<>();
    private long requestExpiryMillis;
    private long requestCooldownMillis;
    private Set<String> allowedWorlds;

    public TeleportRequestService(FriendService friendService, TimeSource timeSource, long requestExpiryMillis, long requestCooldownMillis, Set<String> allowedWorlds) {
        this.friendService = friendService;
        this.timeSource = timeSource == null ? TimeSource.system() : timeSource;
        configure(requestExpiryMillis, requestCooldownMillis, allowedWorlds);
    }

    public void configure(long requestExpiryMillis, long requestCooldownMillis, Set<String> allowedWorlds) {
        this.requestExpiryMillis = Math.max(1_000L, requestExpiryMillis);
        this.requestCooldownMillis = Math.max(0L, requestCooldownMillis);
        this.allowedWorlds = allowedWorlds == null ? Set.of() : allowedWorlds.stream()
            .filter(name -> name != null && !name.isBlank())
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Result request(UUID sender, UUID target, boolean senderOnline, boolean targetOnline, String senderWorldName, String targetWorldName) {
        cleanupExpiredRequests();
        if (sender.equals(target)) {
            return result(Status.SELF, "不能向自己发送好友传送。", sender, target);
        }
        if (!senderOnline || !targetOnline) {
            return result(Status.NOT_ONLINE, "双方都在线时才能发送好友传送。", sender, target);
        }
        if (!worldAllowed(senderWorldName) || !worldAllowed(targetWorldName)) {
            return result(Status.WORLD_BLOCKED, "当前世界或目标世界不允许好友传送。", sender, target);
        }
        if (friendService.isBlockedBetween(sender, target)) {
            return result(Status.BLOCKED, "对方无法接收你的好友传送。", sender, target);
        }
        if (!friendService.areFriends(sender, target)) {
            return result(Status.NOT_FRIENDS, "只能向好友发送好友传送。", sender, target);
        }
        if (!friendService.canReceiveTeleport(target)) {
            return result(Status.TELEPORTS_DISABLED, "对方已关闭好友传送。", sender, target);
        }
        long now = now();
        RequestKey key = new RequestKey(sender, target);
        if (requests.containsKey(key)) {
            return result(Status.REQUEST_PENDING, "好友传送请求已经发送过了。", sender, target);
        }
        Long lastAttempt = lastAttempts.get(key);
        if (lastAttempt != null && now - lastAttempt < requestCooldownMillis) {
            return result(Status.COOLDOWN, "好友传送冷却中，请稍后再试。", sender, target);
        }
        requests.put(key, new TeleportRequest(sender, target, now, now + requestExpiryMillis));
        lastAttempts.put(key, now);
        return result(Status.OK, "好友传送请求已发送。", sender, target);
    }

    public Result accept(UUID target, UUID sender, String senderWorldName, String targetWorldName) {
        cleanupExpiredRequests();
        RequestKey key = new RequestKey(sender, target);
        TeleportRequest request = requests.get(key);
        if (request == null) {
            return result(Status.NO_REQUEST, "没有找到这条好友传送请求。", sender, target);
        }
        if (request.expired(now())) {
            requests.remove(key);
            return result(Status.REQUEST_EXPIRED, "这条好友传送请求已过期。", sender, target);
        }
        if (!worldAllowed(senderWorldName) || !worldAllowed(targetWorldName)) {
            requests.remove(key);
            return result(Status.WORLD_BLOCKED, "当前世界或目标世界不允许好友传送。", sender, target);
        }
        if (friendService.isBlockedBetween(sender, target)) {
            requests.remove(key);
            return result(Status.BLOCKED, "黑名单规则已取消这条好友传送。", sender, target);
        }
        if (!friendService.areFriends(sender, target)) {
            requests.remove(key);
            return result(Status.NOT_FRIENDS, "你们已经不是好友，传送请求已取消。", sender, target);
        }
        if (!friendService.canReceiveTeleport(target)) {
            requests.remove(key);
            return result(Status.TELEPORTS_DISABLED, "好友传送已关闭，请重新发送请求。", sender, target);
        }
        requests.remove(key);
        return result(Status.OK, "已接受好友传送请求。", sender, target);
    }

    public Result deny(UUID target, UUID sender) {
        cleanupExpiredRequests();
        RequestKey key = new RequestKey(sender, target);
        if (requests.remove(key) == null) {
            return result(Status.NO_REQUEST, "没有找到这条好友传送请求。", sender, target);
        }
        return result(Status.OK, "已拒绝好友传送请求。", sender, target);
    }

    private boolean worldAllowed(String worldName) {
        return allowedWorlds.isEmpty() || (worldName != null && allowedWorlds.contains(worldName.toLowerCase(Locale.ROOT)));
    }

    private void cleanupExpiredRequests() {
        long now = now();
        requests.entrySet().removeIf(entry -> entry.getValue().expired(now));
    }

    private long now() {
        return timeSource.nowMillis();
    }

    private static Result result(Status status, String message, UUID sender, UUID target) {
        return new Result(status, message, sender, target);
    }
}
