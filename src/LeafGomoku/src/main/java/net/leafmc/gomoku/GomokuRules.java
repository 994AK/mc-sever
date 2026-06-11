package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GomokuRules {
    private static final int[][] DIRECTIONS = {
        {1, 0},
        {0, 1},
        {1, 1},
        {-1, 1}
    };

    public Stone winnerAfterMove(GomokuBoard board, int row, int column) {
        List<GridCell> line = winningLineAfterMove(board, row, column);
        return line.isEmpty() ? Stone.EMPTY : board.get(row, column);
    }

    public List<GridCell> winningLineAfterMove(GomokuBoard board, int row, int column) {
        Stone stone = board.get(row, column);
        if (stone == Stone.EMPTY) {
            return List.of();
        }

        for (int[] direction : DIRECTIONS) {
            List<GridCell> line = collectLine(board, row, column, direction[0], direction[1], stone);
            if (line.size() >= 5) {
                return winningFive(line, row, column);
            }
        }
        return List.of();
    }

    private List<GridCell> collectLine(GomokuBoard board, int row, int column, int rowStep, int columnStep, Stone stone) {
        List<GridCell> before = collectDirection(board, row, column, -rowStep, -columnStep, stone);
        Collections.reverse(before);
        List<GridCell> line = new ArrayList<>(before);
        line.add(new GridCell(row, column));
        line.addAll(collectDirection(board, row, column, rowStep, columnStep, stone));
        return line;
    }

    private List<GridCell> collectDirection(GomokuBoard board, int row, int column, int rowStep, int columnStep, Stone stone) {
        List<GridCell> cells = new ArrayList<>();
        int nextRow = row + rowStep;
        int nextColumn = column + columnStep;
        while (board.isInside(nextRow, nextColumn) && board.get(nextRow, nextColumn) == stone) {
            cells.add(new GridCell(nextRow, nextColumn));
            nextRow += rowStep;
            nextColumn += columnStep;
        }
        return cells;
    }

    private List<GridCell> winningFive(List<GridCell> line, int row, int column) {
        int placedIndex = 0;
        GridCell placed = new GridCell(row, column);
        for (int index = 0; index < line.size(); index++) {
            if (line.get(index).equals(placed)) {
                placedIndex = index;
                break;
            }
        }
        int start = Math.max(0, Math.min(placedIndex - 4, line.size() - 5));
        return List.copyOf(line.subList(start, start + 5));
    }
}
