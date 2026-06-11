package net.leafmc.gomoku;

public enum MoveStatus {
    ACCEPTED,
    WRONG_TURN,
    OCCUPIED,
    NOT_PLAYING,
    NOT_STARTED,
    ENDED,
    OUT_OF_BOUNDS
}
