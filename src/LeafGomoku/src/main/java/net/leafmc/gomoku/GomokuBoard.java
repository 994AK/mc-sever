package net.leafmc.gomoku;

import java.util.Arrays;

public final class GomokuBoard {
    public static final int MIN_SIZE = 5;
    public static final int DEFAULT_SIZE = 15;
    public static final int MAX_SIZE = 25;
    public static final int SIZE = DEFAULT_SIZE;

    private final int size;
    private final Stone[][] cells;

    public GomokuBoard() {
        this(DEFAULT_SIZE);
    }

    public GomokuBoard(int size) {
        this.size = requireValidSize(size);
        this.cells = new Stone[this.size][this.size];
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

    public void clearCell(int row, int column) {
        requireInside(row, column);
        cells[row][column] = Stone.EMPTY;
    }

    public boolean isEmpty(int row, int column) {
        return get(row, column) == Stone.EMPTY;
    }

    public boolean isInside(int row, int column) {
        return row >= 0 && row < size && column >= 0 && column < size;
    }

    public boolean isFull() {
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (cells[row][column] == Stone.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    public Stone[][] snapshot() {
        Stone[][] copy = new Stone[size][size];
        for (int row = 0; row < size; row++) {
            System.arraycopy(cells[row], 0, copy[row], 0, size);
        }
        return copy;
    }

    public int size() {
        return size;
    }

    public static boolean isValidSize(int size) {
        return size >= MIN_SIZE && size <= MAX_SIZE;
    }

    public static int requireValidSize(int size) {
        if (!isValidSize(size)) {
            throw new IllegalArgumentException("Board size must be between " + MIN_SIZE + " and " + MAX_SIZE + ": " + size);
        }
        return size;
    }

    private void requireInside(int row, int column) {
        if (!isInside(row, column)) {
            throw new IndexOutOfBoundsException("Cell outside board: " + row + "," + column);
        }
    }
}
