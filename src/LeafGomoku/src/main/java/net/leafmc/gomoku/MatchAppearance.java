package net.leafmc.gomoku;

import org.bukkit.Material;

public record MatchAppearance(
    BoardTheme boardTheme,
    PieceSkin blackSkin,
    PieceSkin whiteSkin
) {
    public Material boardMaterialAt(int row, int column) {
        return boardTheme.materialAt(row, column);
    }

    public PieceSkin skinFor(Stone side) {
        return switch (side) {
            case BLACK -> blackSkin;
            case WHITE -> whiteSkin;
            case EMPTY -> null;
        };
    }

    public Material materialFor(Stone side) {
        PieceSkin skin = skinFor(side);
        return skin == null ? boardTheme.primaryMaterial() : skin.material();
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
