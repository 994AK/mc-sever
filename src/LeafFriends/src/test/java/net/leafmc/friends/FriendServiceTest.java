package net.leafmc.friends;

import java.util.List;
import java.util.UUID;

public final class FriendServiceTest {
    public static void main(String[] args) {
        acceptsAndRemovesMutualFriendship();
        denyAndCancelClearPendingRequests();
        requestExpiryAndCooldownAreEnforced();
        blacklistAndSettingsRejectRequestsAndMessages();
        blacklistBlocksMessageAndTeleportSurfaces();
        friendListSortsOnlineFirst();
        hiddenOnlineStatusLooksOfflineToFriends();
        trustedTeleportersRequireFriendshipAndClearOnRelationshipChanges();
    }

    private static void acceptsAndRemovesMutualFriendship() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService service = new FriendService(List.of(), clock, 10_000L, 1_000L);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        TestSupport.check(service.sendRequest(alice, "Alice", bob, "Bob").ok(), "request sent");
        TestSupport.check(service.acceptRequest(bob, alice).ok(), "request accepted");
        TestSupport.check(service.areFriends(alice, bob), "friendship is mutual");
        TestSupport.check(service.removeFriend(alice, bob).ok(), "remove succeeds");
        TestSupport.check(!service.areFriends(alice, bob), "friendship removed from both sides");
    }

    private static void denyAndCancelClearPendingRequests() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService service = new FriendService(List.of(), clock, 10_000L, 0L);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        TestSupport.check(service.sendRequest(alice, "Alice", bob, "Bob").ok(), "request sent before deny");
        TestSupport.check(service.incomingRequests(bob).size() == 1, "incoming request visible before deny");
        TestSupport.check(service.denyRequest(bob, alice).ok(), "deny succeeds");
        TestSupport.check(!service.areFriends(alice, bob), "deny does not create friendship");
        TestSupport.check(service.incomingRequests(bob).isEmpty(), "deny clears incoming request");
        TestSupport.check(service.denyRequest(bob, alice).status() == FriendService.Status.NO_REQUEST, "deny missing request returns no request");

        TestSupport.check(service.sendRequest(alice, "Alice", bob, "Bob").ok(), "request sent before cancel");
        TestSupport.check(service.cancelRequest(alice, bob).ok(), "cancel succeeds");
        TestSupport.check(!service.areFriends(alice, bob), "cancel does not create friendship");
        TestSupport.check(service.incomingRequests(bob).isEmpty(), "cancel clears incoming request");
        TestSupport.check(service.cancelRequest(alice, bob).status() == FriendService.Status.NO_REQUEST, "cancel missing request returns no request");
    }

    private static void requestExpiryAndCooldownAreEnforced() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService service = new FriendService(List.of(), clock, 1_000L, 5_000L);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        TestSupport.check(service.sendRequest(alice, "Alice", bob, "Bob").ok(), "first request sent");
        TestSupport.check(service.sendRequest(alice, "Alice", bob, "Bob").status() == FriendService.Status.REQUEST_PENDING, "duplicate pending rejected");
        clock.advance(1_001L);
        TestSupport.check(service.acceptRequest(bob, alice).status() == FriendService.Status.NO_REQUEST, "expired request removed");
        TestSupport.check(service.sendRequest(alice, "Alice", bob, "Bob").status() == FriendService.Status.COOLDOWN, "cooldown still enforced after expiry");
        clock.advance(5_000L);
        TestSupport.check(service.sendRequest(alice, "Alice", bob, "Bob").ok(), "request allowed after cooldown");
    }

    private static void blacklistAndSettingsRejectRequestsAndMessages() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService service = new FriendService(List.of(), clock, 10_000L, 0L);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        service.ensureProfile(bob, "Bob").settings().setReceiveRequests(false);
        TestSupport.check(service.sendRequest(alice, "Alice", bob, "Bob").status() == FriendService.Status.REQUESTS_DISABLED, "request toggle blocks requests");
        service.ensureProfile(bob, "Bob").settings().setReceiveRequests(true);
        TestSupport.check(service.sendRequest(alice, "Alice", bob, "Bob").ok(), "request sent after toggle on");
        TestSupport.check(service.acceptRequest(bob, alice).ok(), "friendship created");
        service.ensureProfile(bob, "Bob").settings().setReceiveMessages(false);
        TestSupport.check(service.canMessage(alice, bob).status() == FriendService.Status.MESSAGES_DISABLED, "message toggle blocks friend chat");
        TestSupport.check(service.block(alice, bob).ok(), "block succeeds");
        TestSupport.check(!service.areFriends(alice, bob), "block removes friendship");
        TestSupport.check(service.sendRequest(bob, "Bob", alice, "Alice").status() == FriendService.Status.BLOCKED, "blocked player cannot request");
    }

    private static void blacklistBlocksMessageAndTeleportSurfaces() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService service = new FriendService(List.of(), clock, 10_000L, 0L);
        TeleportRequestService teleports = new TeleportRequestService(service, clock, 10_000L, 0L, java.util.Set.of());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        service.sendRequest(alice, "Alice", bob, "Bob");
        service.acceptRequest(bob, alice);
        TestSupport.check(service.block(alice, bob).ok(), "block succeeds");
        TestSupport.check(service.canMessage(bob, alice).status() == FriendService.Status.BLOCKED, "blocked player cannot friend-message");
        TestSupport.check(teleports.request(bob, alice, true, true, "world", "world").status() == TeleportRequestService.Status.BLOCKED, "blocked player cannot friend-teleport");
    }

    private static void friendListSortsOnlineFirst() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService service = new FriendService(List.of(), clock, 10_000L, 0L);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID carol = UUID.randomUUID();

        service.ensureProfile(alice, "Alice");
        service.ensureProfile(bob, "Bob");
        service.ensureProfile(carol, "Carol");
        service.sendRequest(alice, "Alice", carol, "Carol");
        service.acceptRequest(carol, alice);
        service.sendRequest(alice, "Alice", bob, "Bob");
        service.acceptRequest(bob, alice);
        service.markOnline(carol, "Carol");

        List<FriendService.FriendSummary> friends = service.listFriends(alice);
        TestSupport.check(friends.size() == 2, "two friends listed");
        TestSupport.check(friends.get(0).name().equals("Carol"), "online friend sorted first");
        TestSupport.check(friends.get(0).online(), "online status visible");
    }

    private static void hiddenOnlineStatusLooksOfflineToFriends() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService service = new FriendService(List.of(), clock, 10_000L, 0L);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        service.sendRequest(alice, "Alice", bob, "Bob");
        service.acceptRequest(bob, alice);
        service.markOnline(bob, "Bob");
        service.profile(bob).orElseThrow().settings().setShowOnlineStatus(false);

        List<FriendService.FriendSummary> friends = service.listFriends(alice);
        TestSupport.check(friends.size() == 1, "hidden-status friend still listed");
        TestSupport.check(!friends.get(0).online(), "hidden-status friend appears offline");
        TestSupport.check(friends.get(0).lastSeenMillis() == 0L, "hidden-status friend hides last seen");
    }

    private static void trustedTeleportersRequireFriendshipAndClearOnRelationshipChanges() {
        MutableClock clock = new MutableClock(1_000L);
        FriendService service = new FriendService(List.of(), clock, 10_000L, 0L);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        service.ensureProfile(alice, "Alice");
        service.ensureProfile(bob, "Bob");
        TestSupport.check(service.setTrustedTeleporter(bob, alice, true).status() == FriendService.Status.NOT_FRIENDS, "non-friend trust rejected");
        service.sendRequest(alice, "Alice", bob, "Bob");
        service.acceptRequest(bob, alice);

        TestSupport.check(service.setTrustedTeleporter(bob, alice, true).ok(), "friend trust enabled");
        TestSupport.check(service.isTrustedTeleporter(bob, alice), "trust is visible");
        TestSupport.check(service.setTrustedTeleporter(bob, alice, false).ok(), "friend trust disabled");
        TestSupport.check(!service.isTrustedTeleporter(bob, alice), "trust is removed");

        TestSupport.check(service.setTrustedTeleporter(bob, alice, true).ok(), "trust enabled before remove");
        TestSupport.check(service.removeFriend(alice, bob).ok(), "friendship removed");
        TestSupport.check(!service.isTrustedTeleporter(bob, alice), "remove clears target trust");

        service.sendRequest(alice, "Alice", bob, "Bob");
        service.acceptRequest(bob, alice);
        TestSupport.check(service.setTrustedTeleporter(bob, alice, true).ok(), "trust enabled before block");
        TestSupport.check(service.block(alice, bob).ok(), "block removes relationship");
        TestSupport.check(!service.isTrustedTeleporter(bob, alice), "block clears target trust");
    }
}
