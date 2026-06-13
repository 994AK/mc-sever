package net.leafmc.gomoku;

import java.util.List;

public final class GomokuRulesTest {
    public static void main(String[] args) {
        detectsHorizontalWin();
        detectsVerticalWin();
        detectsDiagonalDownWin();
        detectsDiagonalUpWin();
        ignoresFourInARow();
        supportsCustomBoardSize();
    }

    private static void detectsHorizontalWin() {
        GomokuBoard board = new GomokuBoard();
        for (int column = 0; column < 5; column++) {
            board.place(3, column, Stone.BLACK);
        }
        GomokuRules rules = new GomokuRules();
        TestSupport.check(rules.winnerAfterMove(board, 3, 4) == Stone.BLACK, "horizontal win");
        TestSupport.check(rules.winningLineAfterMove(board, 3, 4).equals(List.of(
            new GridCell(3, 0),
            new GridCell(3, 1),
            new GridCell(3, 2),
            new GridCell(3, 3),
            new GridCell(3, 4)
        )), "horizontal winning line");
    }

    private static void detectsVerticalWin() {
        GomokuBoard board = new GomokuBoard();
        for (int row = 0; row < 5; row++) {
            board.place(row, 6, Stone.WHITE);
        }
        GomokuRules rules = new GomokuRules();
        TestSupport.check(rules.winnerAfterMove(board, 4, 6) == Stone.WHITE, "vertical win");
        TestSupport.check(rules.winningLineAfterMove(board, 4, 6).equals(List.of(
            new GridCell(0, 6),
            new GridCell(1, 6),
            new GridCell(2, 6),
            new GridCell(3, 6),
            new GridCell(4, 6)
        )), "vertical winning line");
    }

    private static void detectsDiagonalDownWin() {
        GomokuBoard board = new GomokuBoard();
        for (int offset = 0; offset < 5; offset++) {
            board.place(offset, offset, Stone.BLACK);
        }
        GomokuRules rules = new GomokuRules();
        TestSupport.check(rules.winnerAfterMove(board, 4, 4) == Stone.BLACK, "diagonal down win");
        TestSupport.check(rules.winningLineAfterMove(board, 4, 4).equals(List.of(
            new GridCell(0, 0),
            new GridCell(1, 1),
            new GridCell(2, 2),
            new GridCell(3, 3),
            new GridCell(4, 4)
        )), "diagonal down winning line");
    }

    private static void detectsDiagonalUpWin() {
        GomokuBoard board = new GomokuBoard();
        for (int offset = 0; offset < 5; offset++) {
            board.place(6 - offset, offset, Stone.WHITE);
        }
        GomokuRules rules = new GomokuRules();
        TestSupport.check(rules.winnerAfterMove(board, 2, 4) == Stone.WHITE, "diagonal up win");
        TestSupport.check(rules.winningLineAfterMove(board, 2, 4).equals(List.of(
            new GridCell(6, 0),
            new GridCell(5, 1),
            new GridCell(4, 2),
            new GridCell(3, 3),
            new GridCell(2, 4)
        )), "diagonal up winning line");
    }

    private static void ignoresFourInARow() {
        GomokuBoard board = new GomokuBoard();
        for (int column = 0; column < 4; column++) {
            board.place(10, column, Stone.BLACK);
        }
        GomokuRules rules = new GomokuRules();
        TestSupport.check(rules.winnerAfterMove(board, 10, 3) == Stone.EMPTY, "four is not a win");
        TestSupport.check(rules.winningLineAfterMove(board, 10, 3).isEmpty(), "four has no winning line");
    }

    private static void supportsCustomBoardSize() {
        GomokuBoard board = new GomokuBoard(9);
        for (int column = 4; column < 9; column++) {
            board.place(8, column, Stone.BLACK);
        }

        GomokuRules rules = new GomokuRules();
        TestSupport.check(board.size() == 9, "custom board size");
        TestSupport.check(rules.winnerAfterMove(board, 8, 8) == Stone.BLACK, "custom board edge win");
        TestSupport.check(!board.isInside(9, 8), "custom board outside");
    }
}
