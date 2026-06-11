package net.leafmc.friends;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TeleportRequestServiceTest {
    public static void main(String[] args) {
        requiresFriendshipAndSettings();
        acceptsBeforeExpiryOnly();
        enforcesCooldownAndAllowedWorlds();
        rejectsOfflineParticipantsAndDuplicatePending();
        revalidatesRelationshipBeforeAccept();
    }

    private static void requiresFriendshipAndSettings() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService friends = new FriendService(List.of(), clock, 10_000L, 0L);
        TeleportRequestService teleports = new TeleportRequestService(friends, clock, 10_000L, 0L, Set.of());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        friends.ensureProfile(alice, "Alice");
        friends.ensureProfile(bob, "Bob");
        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world").status() == TeleportRequestService.Status.NOT_FRIENDS, "non-friend teleport rejected");
        friends.sendRequest(alice, "Alice", bob, "Bob");
        friends.acceptRequest(bob, alice);
        friends.profile(bob).orElseThrow().settings().setReceiveTeleports(false);
        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world").status() == TeleportRequestService.Status.TELEPORTS_DISABLED, "teleport toggle rejected");
    }

    private static void acceptsBeforeExpiryOnly() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService friends = new FriendService(List.of(), clock, 10_000L, 0L);
        TeleportRequestService teleports = new TeleportRequestService(friends, clock, 1_000L, 0L, Set.of());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        makeFriends(friends, alice, bob);

        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world").ok(), "teleport request sent");
        TestSupport.check(teleports.accept(bob, alice, "world", "world").ok(), "accept before expiry works");
        TestSupport.check(teleports.accept(bob, alice, "world", "world").status() == TeleportRequestService.Status.NO_REQUEST, "request cleared after accept");

        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world").ok(), "second request sent");
        clock.advance(1_001L);
        TestSupport.check(teleports.accept(bob, alice, "world", "world").status() == TeleportRequestService.Status.NO_REQUEST, "expired request is cleared");
    }

    private static void enforcesCooldownAndAllowedWorlds() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService friends = new FriendService(List.of(), clock, 10_000L, 0L);
        TeleportRequestService teleports = new TeleportRequestService(friends, clock, 10_000L, 5_000L, Set.of("world"));
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        makeFriends(friends, alice, bob);

        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world_nether").status() == TeleportRequestService.Status.WORLD_BLOCKED, "blocked target world rejected");
        TestSupport.check(teleports.request(alice, bob, true, true, "world_nether", "world").status() == TeleportRequestService.Status.WORLD_BLOCKED, "blocked sender world rejected");
        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world").ok(), "first allowed-world request sent");
        TestSupport.check(teleports.deny(bob, alice).ok(), "deny clears active request");
        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world").status() == TeleportRequestService.Status.COOLDOWN, "cooldown enforced");
    }

    private static void rejectsOfflineParticipantsAndDuplicatePending() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService friends = new FriendService(List.of(), clock, 10_000L, 0L);
        TeleportRequestService teleports = new TeleportRequestService(friends, clock, 10_000L, 0L, Set.of());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        makeFriends(friends, alice, bob);

        TestSupport.check(teleports.request(alice, bob, false, true, "world", "world").status() == TeleportRequestService.Status.NOT_ONLINE, "offline sender rejected");
        TestSupport.check(teleports.request(alice, bob, true, false, "world", "world").status() == TeleportRequestService.Status.NOT_ONLINE, "offline target rejected");
        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world").ok(), "first pending request accepted");
        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world").status() == TeleportRequestService.Status.REQUEST_PENDING, "duplicate pending request rejected");
        TestSupport.check(teleports.accept(bob, alice, "world", "world").ok(), "original pending request remains acceptable");
        TestSupport.check(teleports.accept(bob, alice, "world", "world").status() == TeleportRequestService.Status.NO_REQUEST, "accepted request is cleared");
    }

    private static void revalidatesRelationshipBeforeAccept() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService friends = new FriendService(List.of(), clock, 10_000L, 0L);
        TeleportRequestService teleports = new TeleportRequestService(friends, clock, 10_000L, 0L, Set.of());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        makeFriends(friends, alice, bob);

        TestSupport.check(teleports.request(alice, bob, true, true, "world", "world").ok(), "request sent before relationship changes");
        TestSupport.check(friends.block(bob, alice).ok(), "target blocks sender");
        TestSupport.check(teleports.accept(bob, alice, "world", "world").status() == TeleportRequestService.Status.BLOCKED, "accept revalidates blacklist");
        TestSupport.check(teleports.accept(bob, alice, "world", "world").status() == TeleportRequestService.Status.NO_REQUEST, "invalidated request is cleared");
    }

    private static void makeFriends(FriendService friends, UUID alice, UUID bob) {
        friends.sendRequest(alice, "Alice", bob, "Bob");
        friends.acceptRequest(bob, alice);
    }
}
