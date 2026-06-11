package net.leafmc.gomoku;

public enum Stone {
    EMPTY,
    BLACK,
    WHITE;

    public Stone opposite() {
        return switch (this) {
            case BLACK -> WHITE;
            case WHITE -> BLACK;
            case EMPTY -> EMPTY;
        };
    }

    public String displayName() {
        return switch (this) {
            case BLACK -> "黑方";
            case WHITE -> "白方";
            case EMPTY -> "空位";
        };
    }
}
