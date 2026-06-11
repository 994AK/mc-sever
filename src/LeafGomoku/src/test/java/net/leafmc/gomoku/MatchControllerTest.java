package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MatchControllerTest {
    public static void main(String[] args) {
        joinsBlackAndWhite();
        rejectsWrongTurnAndOccupiedCell();
        detectsWinAndLocksBoard();
        detectsDraw();
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
        for (int row = 0; row < GomokuBoard.SIZE; row++) {
            for (int column = 0; column < GomokuBoard.SIZE; column++) {
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
