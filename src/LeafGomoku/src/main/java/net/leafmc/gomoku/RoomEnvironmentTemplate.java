package net.leafmc.gomoku;

import java.util.List;
import org.bukkit.Material;

public record RoomEnvironmentTemplate(
    String id,
    String displayName,
    Material floorMaterial,
    List<FloorAccent> floorAccents,
    Material frameMaterial,
    int frameHeight,
    int entranceWidth,
    List<DecorElement> decor,
    EnvironmentFeedbackProfile feedbackProfile,
    int blockBudget
) {
    public static final String DEFAULT_ID = "classic";
    public static final int DEFAULT_BLOCK_BUDGET = 1500;

    public RoomEnvironmentTemplate {
        floorAccents = List.copyOf(floorAccents == null ? List.of() : floorAccents);
        decor = List.copyOf(decor == null ? List.of() : decor);
        frameHeight = Math.max(0, frameHeight);
        entranceWidth = Math.max(0, entranceWidth);
        blockBudget = Math.max(1, blockBudget);
        feedbackProfile = feedbackProfile == null ? EnvironmentFeedbackProfile.soft() : feedbackProfile;
    }

    public Material floorMaterialAt(int row, int column) {
        for (FloorAccent accent : floorAccents) {
            if (accent.contains(row, column)) {
                return accent.material();
            }
        }
        return floorMaterial;
    }

    public boolean isEntrance(int row, int column) {
        return isEntrance(row, column, GomokuBoard.DEFAULT_SIZE);
    }

    public boolean isEntrance(int row, int column, int boardSize) {
        if (entranceWidth <= 0 || row != BoardGeometry.ROOM_MIN_ROW) {
            return false;
        }
        int center = boardSize / 2;
        int half = entranceWidth / 2;
        return column >= center - half && column <= center + half;
    }

    public record FloorAccent(int rowMin, int rowMax, int columnMin, int columnMax, Material material) {
        public boolean contains(int row, int column) {
            return row >= rowMin && row <= rowMax && column >= columnMin && column <= columnMax;
        }
    }

    public record DecorElement(DecorType type, int row, int column, int yOffset, int height, Material material) {
        public DecorElement {
            height = Math.max(1, height);
        }
    }

    public enum DecorType {
        BLOCK,
        COLUMN
    }
}
