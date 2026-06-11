package net.leafmc.friends;

import java.util.UUID;

public record FriendRequest(UUID sender, UUID target, long createdAtMillis, long expiresAtMillis) {
    public boolean expired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }
}
