package net.leafmc.gomoku;

import org.bukkit.Material;

public record BoardTheme(
    String id,
    String displayName,
    Material primaryMaterial,
    Material secondaryMaterial,
    boolean unlockedByDefault,
    int cost
) {
    public Material materialAt(int row, int column) {
        return ((row + column) & 1) == 0 ? primaryMaterial : secondaryMaterial;
    }
}
