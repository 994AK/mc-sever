package net.leafmc.gomoku;

import java.util.List;

public record MoveResult(
    MoveStatus status,
    Stone stone,
    int row,
    int column,
    Stone winner,
    boolean draw,
    Stone nextTurn,
    List<GridCell> winningLine
) {
    public static MoveResult rejected(MoveStatus status, Stone nextTurn) {
        return new MoveResult(status, Stone.EMPTY, -1, -1, Stone.EMPTY, false, nextTurn, List.of());
    }

    public MoveResult {
        winningLine = winningLine == null ? List.of() : List.copyOf(winningLine);
    }

    public boolean accepted() {
        return status == MoveStatus.ACCEPTED;
    }

    public boolean ended() {
        return winner != Stone.EMPTY || draw;
    }
}
