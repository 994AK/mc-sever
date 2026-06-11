package net.leafmc.gomoku;

import java.util.Locale;
import org.bukkit.Material;

public enum PieceAnimationType {
    ARC,
    POP,
    EXPLOSION,
    WALK;

    public static PieceAnimationType parse(String value, PieceDisplayType displayType, Material material) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (!normalized.isBlank()) {
            return switch (normalized) {
                case "pop", "place", "snap" -> POP;
                case "explode", "explosion", "tnt" -> EXPLOSION;
                case "walk", "walking", "entity_walk" -> WALK;
                default -> ARC;
            };
        }
        return defaultFor(displayType, material);
    }

    public static PieceAnimationType defaultFor(PieceDisplayType displayType, Material material) {
        if (displayType == PieceDisplayType.ENTITY) {
            return WALK;
        }
        if (material == Material.TNT) {
            return EXPLOSION;
        }
        if (displayType == PieceDisplayType.PLAYER_HEAD) {
            return POP;
        }
        return ARC;
    }
}
