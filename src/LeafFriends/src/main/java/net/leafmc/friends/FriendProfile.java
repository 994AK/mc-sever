package net.leafmc.friends;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class FriendProfile {
    private final UUID id;
    private String latestName;
    private final Set<UUID> friends = new LinkedHashSet<>();
    private final Set<UUID> blacklist = new LinkedHashSet<>();
    private FriendSettings settings = new FriendSettings();
    private long lastSeenMillis;

    public FriendProfile(UUID id, String latestName) {
        this.id = Objects.requireNonNull(id, "id");
        this.latestName = normalizeName(latestName);
    }

    public UUID id() {
        return id;
    }

    public String latestName() {
        return latestName;
    }

    public void setLatestName(String latestName) {
        this.latestName = normalizeName(latestName);
    }

    public Set<UUID> friends() {
        return friends;
    }

    public Set<UUID> blacklist() {
        return blacklist;
    }

    public FriendSettings settings() {
        return settings;
    }

    public void setSettings(FriendSettings settings) {
        this.settings = settings == null ? new FriendSettings() : settings;
    }

    public long lastSeenMillis() {
        return lastSeenMillis;
    }

    public void setLastSeenMillis(long lastSeenMillis) {
        this.lastSeenMillis = Math.max(0L, lastSeenMillis);
    }

    private static String normalizeName(String latestName) {
        if (latestName == null || latestName.isBlank()) {
            return "Unknown";
        }
        return latestName.trim();
    }
}
