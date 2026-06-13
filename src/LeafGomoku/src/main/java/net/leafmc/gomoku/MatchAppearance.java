package net.leafmc.gomoku;

public record MatchAppearance(
    BoardTheme boardTheme,
    PieceSkin blackSkin,
    PieceSkin whiteSkin
) {
    public PieceSkin skinFor(Stone side) {
        return switch (side) {
            case BLACK -> blackSkin;
            case WHITE -> whiteSkin;
            case EMPTY -> null;
        };
    }

    public MatchAppearance withTheme(BoardTheme theme) {
        return new MatchAppearance(theme, blackSkin, whiteSkin);
    }

    public MatchAppearance withSkin(Stone side, PieceSkin skin) {
        return switch (side) {
            case BLACK -> new MatchAppearance(boardTheme, skin, whiteSkin);
            case WHITE -> new MatchAppearance(boardTheme, blackSkin, skin);
            case EMPTY -> this;
        };
    }
}
