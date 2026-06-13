package net.leafmc.gomoku;

public record UndoResult(
    UndoStatus status,
    Stone stone,
    int row,
    int column,
    Stone nextTurn
) {
    public static UndoResult rejected(UndoStatus status, Stone nextTurn) {
        return new UndoResult(status, Stone.EMPTY, -1, -1, nextTurn);
    }

    public boolean accepted() {
        return status == UndoStatus.ACCEPTED;
    }
}
