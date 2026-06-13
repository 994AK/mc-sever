package net.leafmc.gomoku;

import java.util.UUID;

public final class UndoRequestTest {
    public static void main(String[] args) {
        matchesOriginalMoveOnly();
    }

    private static void matchesOriginalMoveOnly() {
        UUID requester = UUID.randomUUID();
        UUID approver = UUID.randomUUID();
        UndoRequest request = new UndoRequest(
            UUID.randomUUID(),
            requester,
            "BlackPlayer",
            approver,
            Stone.BLACK,
            7,
            8,
            3,
            1000L,
            300L
        );

        MatchMove original = new MatchMove(3, requester, "BlackPlayer", Stone.BLACK, 7, 8, 1000L);
        MatchMove nextMove = new MatchMove(4, approver, "WhitePlayer", Stone.WHITE, 7, 9, 1200L);
        MatchMove sameCellWrongPlayer = new MatchMove(3, approver, "WhitePlayer", Stone.BLACK, 7, 8, 1200L);
        MatchMove samePlayerWrongCell = new MatchMove(3, requester, "BlackPlayer", Stone.BLACK, 8, 8, 1200L);

        TestSupport.check(request.matches(original), "request matches original move");
        TestSupport.check(!request.matches(nextMove), "request rejects later move");
        TestSupport.check(!request.matches(sameCellWrongPlayer), "request rejects wrong requester");
        TestSupport.check(!request.matches(samePlayerWrongCell), "request rejects changed cell");
        TestSupport.check(request.cell().equals(new GridCell(7, 8)), "request exposes grid cell");
    }
}
