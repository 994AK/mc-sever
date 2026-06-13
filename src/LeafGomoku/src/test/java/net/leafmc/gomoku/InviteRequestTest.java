package net.leafmc.gomoku;

import java.util.UUID;

public final class InviteRequestTest {
    public static void main(String[] args) {
        matchesRoomAliases();
        expiresFromConfiguredTicks();
    }

    private static void matchesRoomAliases() {
        InviteRequest request = request("main_room", 1000L, 600L);

        TestSupport.check(request.matchesRoom("main_room"), "matches same room");
        TestSupport.check(request.matchesRoom(" MAIN_ROOM "), "normalizes requested room");
        TestSupport.check(request.matchesRoom(""), "blank room accepts current invite");
        TestSupport.check(!request.matchesRoom("other"), "rejects different room");
    }

    private static void expiresFromConfiguredTicks() {
        InviteRequest request = request("main", 1000L, 20L);

        TestSupport.check(!request.expired(1999L), "invite is active before timeout");
        TestSupport.check(request.expired(2000L), "invite expires at timeout");
    }

    private static InviteRequest request(String roomId, long createdAtMillis, long timeoutTicks) {
        return new InviteRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Inviter",
            UUID.randomUUID(),
            "Target",
            roomId,
            createdAtMillis,
            timeoutTicks
        );
    }
}
