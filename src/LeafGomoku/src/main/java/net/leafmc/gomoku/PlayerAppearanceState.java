package net.leafmc.gomoku;

public record PlayerAppearanceState(
    String boardThemeId,
    String pieceSkinId
) {
    public static PlayerAppearanceState empty() {
        return new PlayerAppearanceState("", "");
    }
}
