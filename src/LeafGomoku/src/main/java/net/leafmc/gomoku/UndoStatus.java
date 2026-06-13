package net.leafmc.gomoku;

public enum UndoStatus {
    ACCEPTED,
    OUT_OF_BOUNDS,
    NOT_STARTED,
    ENDED,
    INVALID_STONE,
    EMPTY_CELL,
    STONE_MISMATCH
}
