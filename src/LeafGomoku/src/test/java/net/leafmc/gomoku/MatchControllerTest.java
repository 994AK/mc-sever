package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MatchControllerTest {
    public static void main(String[] args) {
        joinsBlackAndWhite();
        usesCustomBoardSize();
        rejectsWrongTurnAndOccupiedCell();
        detectsWinAndLocksBoard();
        detectsDraw();
        undoLastMoveRestoresTurnAndClearsCell();
        rejectsUndoWhenStateOrCellDoesNotMatch();
        forceEndLocksAdministrativeResult();
        resetClearsState();
    }

    private static void joinsBlackAndWhite() {
        MatchController match = new MatchController();
        UUID black = UUID.randomUUID();
        UUID white = UUID.randomUUID();
        TestSupport.check(match.join(black).status() == JoinStatus.JOINED_BLACK, "black joins first");
        TestSupport.check(match.state() == GameState.WAITING_FOR_WHITE, "waiting for white");
        TestSupport.check(match.join(white).status() == JoinStatus.JOINED_WHITE, "white joins second");
        TestSupport.check(match.state() == GameState.PLAYING, "playing after two joins");
        TestSupport.check(match.currentTurn() == Stone.BLACK, "black moves first");
    }

    private static void rejectsWrongTurnAndOccupiedCell() {
        MatchController match = startedMatch();
        UUID black = match.playerFor(Stone.BLACK).orElseThrow();
        UUID white = match.playerFor(Stone.WHITE).orElseThrow();
        TestSupport.check(match.play(white, 0, 0).status() == MoveStatus.WRONG_TURN, "white cannot move first");
        MoveResult first = match.play(black, 0, 0);
        TestSupport.check(first.accepted(), "black move accepted");
        TestSupport.check(match.play(white, 0, 0).status() == MoveStatus.OCCUPIED, "occupied rejected");
        TestSupport.check(match.currentTurn() == Stone.WHITE, "occupied does not switch turn");
    }

    private static void detectsWinAndLocksBoard() {
        MatchController match = startedMatch();
        UUID black = match.playerFor(Stone.BLACK).orElseThrow();
        UUID white = match.playerFor(Stone.WHITE).orElseThrow();
        for (int column = 0; column < 4; column++) {
            TestSupport.check(match.play(black, 0, column).accepted(), "black accepted " + column);
            TestSupport.check(match.play(white, 1, column).accepted(), "white accepted " + column);
        }
        MoveResult win = match.play(black, 0, 4);
        TestSupport.check(win.winner() == Stone.BLACK, "black wins");
        TestSupport.check(win.winningLine().equals(List.of(
            new GridCell(0, 0),
            new GridCell(0, 1),
            new GridCell(0, 2),
            new GridCell(0, 3),
            new GridCell(0, 4)
        )), "win result exposes winning line");
        TestSupport.check(match.state() == GameState.ENDED, "match ended after win");
        TestSupport.check(match.play(white, 1, 4).status() == MoveStatus.ENDED, "ended match rejects moves");
    }

    private static void detectsDraw() {
        MatchController match = startedMatch();
        UUID black = match.playerFor(Stone.BLACK).orElseThrow();
        UUID white = match.playerFor(Stone.WHITE).orElseThrow();
        List<GridCell> blackCells = new ArrayList<>();
        List<GridCell> whiteCells = new ArrayList<>();
        int size = match.board().size();
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                GridCell cell = new GridCell(row, column);
                if (((row / 2) + column) % 2 == 0) {
                    blackCells.add(cell);
                } else {
                    whiteCells.add(cell);
                }
            }
        }

        for (int index = 0; index < whiteCells.size(); index++) {
            GridCell blackCell = blackCells.get(index);
            MoveResult blackResult = match.play(black, blackCell.row(), blackCell.column());
            TestSupport.check(blackResult.accepted(), "draw black accepted at " + blackCell + ": " + blackResult.status());

            GridCell whiteCell = whiteCells.get(index);
            MoveResult whiteResult = match.play(white, whiteCell.row(), whiteCell.column());
            TestSupport.check(whiteResult.accepted(), "draw white accepted at " + whiteCell + ": " + whiteResult.status());
        }
        GridCell finalBlack = blackCells.get(blackCells.size() - 1);
        MoveResult finalMove = match.play(black, finalBlack.row(), finalBlack.column());
        TestSupport.check(finalMove.accepted(), "final draw move accepted: " + finalMove.status());
        TestSupport.check(match.draw(), "full board is draw");
        TestSupport.check(match.state() == GameState.ENDED, "draw ends match");
    }

    private static void resetClearsState() {
        MatchController match = startedMatch();
        UUID black = match.playerFor(Stone.BLACK).orElseThrow();
        TestSupport.check(match.play(black, 0, 0).accepted(), "move before reset");
        match.reset();
        TestSupport.check(match.state() == GameState.IDLE, "idle after reset");
        TestSupport.check(match.currentTurn() == Stone.EMPTY, "no turn after reset");
        TestSupport.check(match.board().isEmpty(0, 0), "board cleared");
    }

    private static void usesCustomBoardSize() {
        MatchController match = new MatchController(9);
        UUID black = UUID.randomUUID();
        UUID white = UUID.randomUUID();
        match.join(black);
        match.join(white);

        TestSupport.check(match.boardSize() == 9, "custom match board size");
        TestSupport.check(match.play(black, 8, 8).accepted(), "custom board accepts far corner");
        TestSupport.check(match.play(white, 9, 8).status() == MoveStatus.OUT_OF_BOUNDS, "custom board rejects outside");
    }

    private static void undoLastMoveRestoresTurnAndClearsCell() {
        MatchController match = startedMatch();
        UUID black = match.playerFor(Stone.BLACK).orElseThrow();
        UUID white = match.playerFor(Stone.WHITE).orElseThrow();
        TestSupport.check(match.play(black, 0, 0).accepted(), "black move before undo");
        UndoResult blackUndo = match.undoLastMove(Stone.BLACK, 0, 0);
        TestSupport.check(blackUndo.accepted(), "black undo accepted");
        TestSupport.check(match.board().isEmpty(0, 0), "black undo clears cell");
        TestSupport.check(match.currentTurn() == Stone.BLACK, "black undo restores black turn");

        TestSupport.check(match.play(black, 1, 1).accepted(), "black move after undo");
        TestSupport.check(match.play(white, 2, 2).accepted(), "white move before undo");
        UndoResult whiteUndo = match.undoLastMove(Stone.WHITE, 2, 2);
        TestSupport.check(whiteUndo.accepted(), "white undo accepted");
        TestSupport.check(match.board().isEmpty(2, 2), "white undo clears cell");
        TestSupport.check(match.currentTurn() == Stone.WHITE, "white undo restores white turn");
    }

    private static void rejectsUndoWhenStateOrCellDoesNotMatch() {
        MatchController idle = new MatchController();
        TestSupport.check(idle.undoLastMove(Stone.BLACK, 0, 0).status() == UndoStatus.NOT_STARTED, "idle undo rejected");

        MatchController waiting = new MatchController();
        waiting.join(UUID.randomUUID());
        TestSupport.check(waiting.undoLastMove(Stone.BLACK, 0, 0).status() == UndoStatus.NOT_STARTED, "waiting undo rejected");

        MatchController match = startedMatch();
        UUID black = match.playerFor(Stone.BLACK).orElseThrow();
        TestSupport.check(match.play(black, 0, 0).accepted(), "move before rejected undo");
        TestSupport.check(match.undoLastMove(Stone.BLACK, -1, 0).status() == UndoStatus.OUT_OF_BOUNDS, "out of bounds undo rejected");
        TestSupport.check(match.undoLastMove(Stone.WHITE, 0, 0).status() == UndoStatus.STONE_MISMATCH, "mismatched stone undo rejected");
        TestSupport.check(match.undoLastMove(Stone.BLACK, 0, 1).status() == UndoStatus.EMPTY_CELL, "empty cell undo rejected");
        match.forceEnd(Stone.BLACK, false);
        TestSupport.check(match.undoLastMove(Stone.BLACK, 0, 0).status() == UndoStatus.ENDED, "ended undo rejected");
    }

    private static void forceEndLocksAdministrativeResult() {
        MatchController match = startedMatch();
        match.forceEnd(Stone.WHITE, false);
        TestSupport.check(match.state() == GameState.ENDED, "force end marks ended");
        TestSupport.check(match.winner() == Stone.WHITE, "force end winner");
        TestSupport.check(match.play(match.playerFor(Stone.BLACK).orElseThrow(), 0, 0).status() == MoveStatus.ENDED, "force ended match rejects moves");
    }

    private static MatchController startedMatch() {
        MatchController match = new MatchController();
        match.join(UUID.randomUUID());
        match.join(UUID.randomUUID());
        return match;
    }
}
