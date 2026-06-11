package net.leafmc.gomoku;

import org.bukkit.Material;

public record PieceSkin(
    String id,
    String displayName,
    Material material,
    PieceDisplayType displayType,
    String headOwner,
    String entityType,
    PieceAnimationType animationType,
    boolean unlockedByDefault,
    int cost
) {
    public PieceSkin {
        displayType = displayType == null ? PieceDisplayType.BLOCK : displayType;
        headOwner = headOwner == null ? "" : headOwner;
        entityType = entityType == null ? "" : entityType;
        animationType = animationType == null ? PieceAnimationType.defaultFor(displayType, material) : animationType;
    }

    public Material animationMaterial() {
        if (material != null && material != Material.AIR) {
            return material;
        }
        if (displayType == PieceDisplayType.PLAYER_HEAD) {
            return Material.PLAYER_HEAD;
        }
        return Material.STONE;
    }

    public Material boardBlockMaterial() {
        return animationMaterial();
    }

    public Material iconMaterial() {
        if (material != null && material != Material.AIR) {
            return material;
        }
        return boardBlockMaterial();
    }

    public String visualKey() {
        return switch (displayType) {
            case ENTITY -> "entity:" + normalize(entityType);
            case PLAYER_HEAD -> "head:" + normalize(headOwner) + ":" + boardBlockMaterial().name();
            case BLOCK -> "block:" + boardBlockMaterial().name();
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
