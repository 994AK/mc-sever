package net.leafmc.friends;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class YamlFriendStoreTest {
    public static void main(String[] args) throws Exception {
        roundTripsProfiles();
        skipsInvalidUuidEntries();
        loadsMissingSettingsWithDefaults();
    }

    private static void roundTripsProfiles() throws Exception {
        Path dir = Files.createTempDirectory("leaffriends-store");
        YamlFriendStore store = new YamlFriendStore(dir.resolve("friends.yml").toFile());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        FriendProfile profile = new FriendProfile(alice, "Alice");
        profile.friends().add(bob);
        profile.blacklist().add(blocked);
        profile.settings().setReceiveRequests(false);
        profile.settings().setReceiveTeleports(false);
        profile.settings().setReceiveMessages(false);
        profile.settings().setOnlineNotifications(false);
        profile.settings().setShowOnlineStatus(false);
        profile.setLastSeenMillis(12_345L);

        store.save(List.of(profile));
        List<FriendProfile> loaded = store.load();
        TestSupport.check(loaded.size() == 1, "one profile loaded");
        FriendProfile reloaded = loaded.get(0);
        TestSupport.check(reloaded.id().equals(alice), "uuid round trips");
        TestSupport.check(reloaded.latestName().equals("Alice"), "latest name round trips");
        TestSupport.check(reloaded.friends().contains(bob), "friend uuid round trips");
        TestSupport.check(reloaded.blacklist().contains(blocked), "blacklist uuid round trips");
        TestSupport.check(!reloaded.settings().receiveRequests(), "request setting round trips");
        TestSupport.check(!reloaded.settings().receiveTeleports(), "teleport setting round trips");
        TestSupport.check(!reloaded.settings().receiveMessages(), "message setting round trips");
        TestSupport.check(!reloaded.settings().onlineNotifications(), "notification setting round trips");
        TestSupport.check(!reloaded.settings().showOnlineStatus(), "status setting round trips");
        TestSupport.check(reloaded.lastSeenMillis() == 12_345L, "last seen round trips");
    }

    private static void skipsInvalidUuidEntries() throws Exception {
        Path dir = Files.createTempDirectory("leaffriends-store-invalid");
        Path file = dir.resolve("friends.yml");
        Files.writeString(file, """
            profiles:
              not-a-uuid:
                name: Broken
              00000000-0000-0000-0000-000000000001:
                name: Valid
                friends:
                  - also-not-a-uuid
            """);
        List<FriendProfile> loaded = new YamlFriendStore(file.toFile()).load();
        TestSupport.check(loaded.size() == 1, "invalid profile uuid skipped");
        TestSupport.check(loaded.get(0).friends().isEmpty(), "invalid friend uuid skipped");
    }

    private static void loadsMissingSettingsWithDefaults() throws Exception {
        Path dir = Files.createTempDirectory("leaffriends-store-defaults");
        Path file = dir.resolve("friends.yml");
        Files.writeString(file, """
            profiles:
              00000000-0000-0000-0000-000000000001:
                name: Valid
            """);
        FriendProfile loaded = new YamlFriendStore(file.toFile()).load().get(0);
        TestSupport.check(loaded.settings().receiveRequests(), "missing request setting defaults true");
        TestSupport.check(loaded.settings().receiveTeleports(), "missing teleport setting defaults true");
        TestSupport.check(loaded.settings().receiveMessages(), "missing message setting defaults true");
        TestSupport.check(loaded.settings().onlineNotifications(), "missing notification setting defaults true");
        TestSupport.check(loaded.settings().showOnlineStatus(), "missing status setting defaults true");
    }
}
