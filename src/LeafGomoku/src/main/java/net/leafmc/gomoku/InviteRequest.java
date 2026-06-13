package net.leafmc.gomoku;

import java.util.UUID;

public record InviteRequest(
    UUID requestId,
    UUID inviterId,
    String inviterName,
    UUID targetId,
    String targetName,
    String roomId,
    long createdAtMillis,
    long timeoutTicks
) {
    public boolean expired(long nowMillis) {
        return nowMillis - createdAtMillis >= timeoutTicks * 50L;
    }

    public boolean matchesRoom(String value) {
        return value == null || value.isBlank() || roomId.equals(ArenaConfig.normalizeRoomId(value));
    }
}
