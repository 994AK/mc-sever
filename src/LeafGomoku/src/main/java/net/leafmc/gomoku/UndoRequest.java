package net.leafmc.gomoku;

import java.util.UUID;

public record UndoRequest(
    UUID requestId,
    UUID requesterId,
    String requesterName,
    UUID approverId,
    Stone side,
    int row,
    int column,
    int moveIndex,
    long requestedAtMillis,
    long timeoutTicks
) {
    public boolean matches(MatchMove move) {
        return move != null
            && move.moveIndex() == moveIndex
            && move.playerId().equals(requesterId)
            && move.side() == side
            && move.row() == row
            && move.column() == column;
    }

    public GridCell cell() {
        return new GridCell(row, column);
    }
}
