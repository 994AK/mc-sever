package net.leafmc.gomoku;

import java.util.Locale;

public enum PieceDisplayType {
    BLOCK,
    PLAYER_HEAD,
    ENTITY;

    public static PieceDisplayType parse(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "head", "player_head", "skull" -> PLAYER_HEAD;
            case "entity", "mob", "static_entity" -> ENTITY;
            default -> BLOCK;
        };
    }
}
