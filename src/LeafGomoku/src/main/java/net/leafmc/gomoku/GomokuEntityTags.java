package net.leafmc.gomoku;

import org.bukkit.entity.Entity;

public final class GomokuEntityTags {
    public static final String ANIMATION = "leafgomoku-animation";
    public static final String STATIC_PIECE = "leafgomoku-static-piece";

    private GomokuEntityTags() {
    }

    public static boolean isGomokuEntity(Entity entity) {
        return entity.getScoreboardTags().contains(ANIMATION) || entity.getScoreboardTags().contains(STATIC_PIECE);
    }

    public static String roomStaticPiece(String roomId) {
        return STATIC_PIECE + "-" + roomId;
    }
}
