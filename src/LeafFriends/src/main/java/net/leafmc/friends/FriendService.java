package net.leafmc.friends;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FriendService {
    public enum Status {
        OK,
        SELF,
        NOT_FOUND,
        ALREADY_FRIENDS,
        NOT_FRIENDS,
        REQUEST_PENDING,
        REVERSE_REQUEST_PENDING,
        NO_REQUEST,
        REQUEST_EXPIRED,
        REQUESTS_DISABLED,
        MESSAGES_DISABLED,
        TELEPORTS_DISABLED,
        BLOCKED,
        COOLDOWN
    }

    public enum SettingKey {
        REQUESTS("requests"),
        TELEPORTS("teleports"),
        MESSAGES("messages"),
        NOTIFICATIONS("notifications"),
        STATUS("status");

        private final String key;

        SettingKey(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static Optional<SettingKey> parse(String raw) {
            if (raw == null) {
                return Optional.empty();
            }
            String normalized = raw.toLowerCase(Locale.ROOT);
            for (SettingKey key : values()) {
                if (key.key.equals(normalized)) {
                    return Optional.of(key);
                }
            }
            return switch (normalized) {
                case "request", "申请" -> Optional.of(REQUESTS);
                case "tp", "teleport", "传送" -> Optional.of(TELEPORTS);
                case "msg", "message", "私聊" -> Optional.of(MESSAGES);
                case "notify", "notice", "上线提醒" -> Optional.of(NOTIFICATIONS);
                case "online", "presence", "在线状态" -> Optional.of(STATUS);
                default -> Optional.empty();
            };
        }
    }

    public record Result(Status status, String message) {
        public boolean ok() {
            return status == Status.OK;
        }
    }

    public record FriendSummary(UUID id, String name, boolean online, long lastSeenMillis) {
    }

    private record RequestKey(UUID sender, UUID target) {
    }

    private final Map<UUID, FriendProfile> profiles = new HashMap<>();
    private final Set<UUID> onlinePlayers = new HashSet<>();
    private final Map<RequestKey, FriendRequest> requests = new HashMap<>();
    private final Map<RequestKey, Long> lastRequestAttempts = new HashMap<>();
    private final TimeSource timeSource;
    private long requestExpiryMillis;
    private long requestCooldownMillis;

    public FriendService(Collection<FriendProfile> loadedProfiles, TimeSource timeSource, long requestExpiryMillis, long requestCooldownMillis) {
        if (loadedProfiles != null) {
            for (FriendProfile profile : loadedProfiles) {
                profiles.put(profile.id(), profile);
            }
        }
        this.timeSource = timeSource == null ? TimeSource.system() : timeSource;
        configure(requestExpiryMillis, requestCooldownMillis);
    }

    public void configure(long requestExpiryMillis, long requestCooldownMillis) {
        this.requestExpiryMillis = Math.max(1_000L, requestExpiryMillis);
        this.requestCooldownMillis = Math.max(0L, requestCooldownMillis);
    }

    public Collection<FriendProfile> profiles() {
        return profiles.values();
    }

    public Optional<FriendProfile> profile(UUID id) {
        return Optional.ofNullable(profiles.get(id));
    }

    public FriendProfile ensureProfile(UUID id, String name) {
        FriendProfile profile = profiles.computeIfAbsent(id, ignored -> new FriendProfile(id, name));
        if (name != null && !name.isBlank()) {
            profile.setLatestName(name);
        }
        return profile;
    }

    public Optional<FriendProfile> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return profiles.values().stream()
            .filter(profile -> profile.latestName().toLowerCase(Locale.ROOT).equals(normalized))
            .findFirst();
    }

    public void markOnline(UUID id, String name) {
        FriendProfile profile = ensureProfile(id, name);
        profile.setLastSeenMillis(now());
        onlinePlayers.add(id);
    }

    public void markOffline(UUID id) {
        FriendProfile profile = profiles.get(id);
        if (profile != null) {
            profile.setLastSeenMillis(now());
        }
        onlinePlayers.remove(id);
    }

    public boolean isOnline(UUID id) {
        return onlinePlayers.contains(id);
    }

    public Result sendRequest(UUID sender, String senderName, UUID target, String targetName) {
        cleanupExpiredRequests();
        if (sender.equals(target)) {
            return result(Status.SELF, "不能添加自己为好友。");
        }
        FriendProfile senderProfile = ensureProfile(sender, senderName);
        FriendProfile targetProfile = ensureProfile(target, targetName);
        if (areFriends(sender, target)) {
            return result(Status.ALREADY_FRIENDS, "你们已经是好友了。");
        }
        if (isBlockedBetween(sender, target)) {
            return result(Status.BLOCKED, "对方无法接收你的好友申请。");
        }
        if (!targetProfile.settings().receiveRequests()) {
            return result(Status.REQUESTS_DISABLED, "对方已关闭好友申请。");
        }

        long now = now();
        RequestKey key = new RequestKey(sender, target);
        RequestKey reverse = new RequestKey(target, sender);
        if (requests.containsKey(key)) {
            return result(Status.REQUEST_PENDING, "好友申请已经发送过了。");
        }
        if (requests.containsKey(reverse)) {
            return result(Status.REVERSE_REQUEST_PENDING, "对方已经向你发送好友申请，输入 /friend accept " + targetProfile.latestName() + " 同意。");
        }
        Long lastAttempt = lastRequestAttempts.get(key);
        if (lastAttempt != null && now - lastAttempt < requestCooldownMillis) {
            return result(Status.COOLDOWN, "好友申请冷却中，请稍后再试。");
        }
        requests.put(key, new FriendRequest(sender, target, now, now + requestExpiryMillis));
        lastRequestAttempts.put(key, now);
        senderProfile.setLatestName(senderName);
        return result(Status.OK, "好友申请已发送给 " + targetProfile.latestName() + "。");
    }

    public Result acceptRequest(UUID target, UUID sender) {
        cleanupExpiredRequests();
        RequestKey key = new RequestKey(sender, target);
        FriendRequest request = requests.get(key);
        if (request == null) {
            return result(Status.NO_REQUEST, "没有找到这条好友申请。");
        }
        if (request.expired(now())) {
            requests.remove(key);
            return result(Status.REQUEST_EXPIRED, "这条好友申请已过期。");
        }
        if (isBlockedBetween(sender, target)) {
            requests.remove(key);
            return result(Status.BLOCKED, "好友申请已被黑名单规则拦截。");
        }
        FriendProfile senderProfile = profiles.get(sender);
        FriendProfile targetProfile = profiles.get(target);
        if (senderProfile == null || targetProfile == null) {
            requests.remove(key);
            return result(Status.NOT_FOUND, "找不到玩家资料。");
        }
        senderProfile.friends().add(target);
        targetProfile.friends().add(sender);
        requests.remove(key);
        requests.remove(new RequestKey(target, sender));
        return result(Status.OK, "你已和 " + senderProfile.latestName() + " 成为好友。");
    }

    public Result denyRequest(UUID target, UUID sender) {
        cleanupExpiredRequests();
        RequestKey key = new RequestKey(sender, target);
        if (requests.remove(key) == null) {
            return result(Status.NO_REQUEST, "没有找到这条好友申请。");
        }
        return result(Status.OK, "已拒绝好友申请。");
    }

    public Result cancelRequest(UUID sender, UUID target) {
        cleanupExpiredRequests();
        RequestKey key = new RequestKey(sender, target);
        if (requests.remove(key) == null) {
            return result(Status.NO_REQUEST, "没有找到待取消的好友申请。");
        }
        return result(Status.OK, "已取消好友申请。");
    }

    public Result removeFriend(UUID actor, UUID target) {
        FriendProfile actorProfile = profiles.get(actor);
        FriendProfile targetProfile = profiles.get(target);
        if (actorProfile == null || targetProfile == null || !actorProfile.friends().contains(target)) {
            return result(Status.NOT_FRIENDS, "你们还不是好友。");
        }
        actorProfile.friends().remove(target);
        actorProfile.trustedTeleporters().remove(target);
        targetProfile.friends().remove(actor);
        targetProfile.trustedTeleporters().remove(actor);
        return result(Status.OK, "已删除好友 " + targetProfile.latestName() + "。");
    }

    public Result block(UUID actor, UUID target) {
        if (actor.equals(target)) {
            return result(Status.SELF, "不能拉黑自己。");
        }
        FriendProfile actorProfile = ensureProfile(actor, null);
        FriendProfile targetProfile = ensureProfile(target, null);
        actorProfile.blacklist().add(target);
        actorProfile.friends().remove(target);
        actorProfile.trustedTeleporters().remove(target);
        targetProfile.friends().remove(actor);
        targetProfile.trustedTeleporters().remove(actor);
        requests.remove(new RequestKey(actor, target));
        requests.remove(new RequestKey(target, actor));
        return result(Status.OK, "已拉黑 " + targetProfile.latestName() + "。");
    }

    public Result unblock(UUID actor, UUID target) {
        FriendProfile actorProfile = profiles.get(actor);
        if (actorProfile == null || !actorProfile.blacklist().remove(target)) {
            return result(Status.NOT_FOUND, "黑名单里没有这个玩家。");
        }
        return result(Status.OK, "已解除拉黑。");
    }

    public Result canMessage(UUID sender, UUID target) {
        if (isBlockedBetween(sender, target)) {
            return result(Status.BLOCKED, "对方无法接收你的好友私聊。");
        }
        if (!areFriends(sender, target)) {
            return result(Status.NOT_FRIENDS, "只能给好友发送好友私聊。");
        }
        FriendProfile targetProfile = profiles.get(target);
        if (targetProfile == null || !targetProfile.settings().receiveMessages()) {
            return result(Status.MESSAGES_DISABLED, "对方已关闭好友私聊。");
        }
        return result(Status.OK, "可以发送好友私聊。");
    }

    public Result setSetting(UUID actor, SettingKey key, boolean value) {
        FriendProfile profile = ensureProfile(actor, null);
        switch (key) {
            case REQUESTS -> profile.settings().setReceiveRequests(value);
            case TELEPORTS -> profile.settings().setReceiveTeleports(value);
            case MESSAGES -> profile.settings().setReceiveMessages(value);
            case NOTIFICATIONS -> profile.settings().setOnlineNotifications(value);
            case STATUS -> profile.settings().setShowOnlineStatus(value);
        }
        return result(Status.OK, "设置已更新: " + key.key() + "=" + (value ? "on" : "off"));
    }

    public Result setTrustedTeleporter(UUID owner, UUID friend, boolean trusted) {
        FriendProfile ownerProfile = profiles.get(owner);
        FriendProfile friendProfile = profiles.get(friend);
        if (ownerProfile == null || friendProfile == null || !areFriends(owner, friend)) {
            return result(Status.NOT_FRIENDS, "只能给好友设置免确认传送。");
        }
        if (isBlockedBetween(owner, friend)) {
            return result(Status.BLOCKED, "黑名单关系下不能设置免确认传送。");
        }
        if (trusted) {
            ownerProfile.trustedTeleporters().add(friend);
            return result(Status.OK, "已允许 " + friendProfile.latestName() + " 免确认传送到你身边。");
        }
        ownerProfile.trustedTeleporters().remove(friend);
        return result(Status.OK, "已取消 " + friendProfile.latestName() + " 的免确认传送。");
    }

    public boolean isTrustedTeleporter(UUID owner, UUID friend) {
        FriendProfile ownerProfile = profiles.get(owner);
        return ownerProfile != null && ownerProfile.trustedTeleporters().contains(friend);
    }

    public List<FriendSummary> listFriends(UUID viewer) {
        cleanupExpiredRequests();
        FriendProfile viewerProfile = profiles.get(viewer);
        if (viewerProfile == null) {
            return List.of();
        }
        List<FriendSummary> summaries = new ArrayList<>();
        for (UUID friendId : viewerProfile.friends()) {
            FriendProfile friend = profiles.get(friendId);
            if (friend == null) {
                continue;
            }
            boolean visibleOnline = onlinePlayers.contains(friendId) && friend.settings().showOnlineStatus();
            long lastSeen = friend.settings().showOnlineStatus() ? friend.lastSeenMillis() : 0L;
            summaries.add(new FriendSummary(friendId, friend.latestName(), visibleOnline, lastSeen));
        }
        summaries.sort(Comparator
            .comparing(FriendSummary::online).reversed()
            .thenComparing(summary -> summary.name().toLowerCase(Locale.ROOT)));
        return summaries;
    }

    public List<FriendProfile> incomingRequests(UUID target) {
        cleanupExpiredRequests();
        List<FriendProfile> result = new ArrayList<>();
        for (FriendRequest request : requests.values()) {
            if (request.target().equals(target)) {
                FriendProfile sender = profiles.get(request.sender());
                if (sender != null) {
                    result.add(sender);
                }
            }
        }
        result.sort(Comparator.comparing(profile -> profile.latestName().toLowerCase(Locale.ROOT)));
        return result;
    }

    public List<FriendProfile> onlineFriends(UUID id) {
        FriendProfile profile = profiles.get(id);
        if (profile == null) {
            return List.of();
        }
        List<FriendProfile> result = new ArrayList<>();
        for (UUID friendId : profile.friends()) {
            if (onlinePlayers.contains(friendId)) {
                FriendProfile friend = profiles.get(friendId);
                if (friend != null) {
                    result.add(friend);
                }
            }
        }
        return result;
    }

    public boolean areFriends(UUID first, UUID second) {
        FriendProfile firstProfile = profiles.get(first);
        FriendProfile secondProfile = profiles.get(second);
        return firstProfile != null
            && secondProfile != null
            && firstProfile.friends().contains(second)
            && secondProfile.friends().contains(first);
    }

    public boolean isBlockedBetween(UUID first, UUID second) {
        FriendProfile firstProfile = profiles.get(first);
        FriendProfile secondProfile = profiles.get(second);
        return (firstProfile != null && firstProfile.blacklist().contains(second))
            || (secondProfile != null && secondProfile.blacklist().contains(first));
    }

    public boolean canReceiveTeleport(UUID target) {
        FriendProfile profile = profiles.get(target);
        return profile != null && profile.settings().receiveTeleports();
    }

    public boolean hidesOnlineStatus(UUID target) {
        FriendProfile profile = profiles.get(target);
        return profile != null && !profile.settings().showOnlineStatus();
    }

    public List<String> knownNames() {
        return profiles.values().stream()
            .map(FriendProfile::latestName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private void cleanupExpiredRequests() {
        long now = now();
        requests.entrySet().removeIf(entry -> entry.getValue().expired(now));
    }

    private long now() {
        return timeSource.nowMillis();
    }

    private static Result result(Status status, String message) {
        return new Result(status, message);
    }
}
