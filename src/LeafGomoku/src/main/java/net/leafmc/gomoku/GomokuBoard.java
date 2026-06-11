package net.leafmc.gomoku;

import java.util.Arrays;

public final class GomokuBoard {
    public static final int SIZE = 15;

    private final Stone[][] cells = new Stone[SIZE][SIZE];

    public GomokuBoard() {
        clear();
    }

    public void clear() {
        for (Stone[] row : cells) {
            Arrays.fill(row, Stone.EMPTY);
        }
    }

    public Stone get(int row, int column) {
        requireInside(row, column);
        return cells[row][column];
    }

    public void place(int row, int column, Stone stone) {
        requireInside(row, column);
        if (stone == Stone.EMPTY) {
            throw new IllegalArgumentException("Cannot place EMPTY as a move");
        }
        cells[row][column] = stone;
    }

    public boolean isEmpty(int row, int column) {
        return get(row, column) == Stone.EMPTY;
    }

    public boolean isInside(int row, int column) {
        return row >= 0 && row < SIZE && column >= 0 && column < SIZE;
    }

    public boolean isFull() {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (cells[row][column] == Stone.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    public Stone[][] snapshot() {
        Stone[][] copy = new Stone[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(cells[row], 0, copy[row], 0, SIZE);
        }
        return copy;
    }

    private void requireInside(int row, int column) {
        if (!isInside(row, column)) {
            throw new IndexOutOfBoundsException("Cell outside board: " + row + "," + column);
        }
    }
}
