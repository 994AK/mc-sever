package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;

public final class RoomEnvironmentLayout {
    private final BoardGeometry geometry;
    private final RoomEnvironmentTemplate template;

    public RoomEnvironmentLayout(BoardGeometry geometry, RoomEnvironmentTemplate template) {
        this.geometry = geometry;
        this.template = template;
    }

    public List<EnvironmentBlock> floorBlocks() {
        List<EnvironmentBlock> blocks = new ArrayList<>();
        for (int row = BoardGeometry.ROOM_MIN_ROW; row <= BoardGeometry.ROOM_MAX_ROW; row++) {
            for (int column = BoardGeometry.ROOM_MIN_COLUMN; column <= BoardGeometry.ROOM_MAX_COLUMN; column++) {
                Material material = template.floorMaterialAt(row, column);
                if (material != Material.AIR) {
                    blocks.add(new EnvironmentBlock(geometry.boardPoint(row, column), material));
                }
            }
        }
        return blocks;
    }

    public List<EnvironmentBlock> frameBlocks() {
        List<EnvironmentBlock> blocks = new ArrayList<>();
        if (template.frameMaterial() == Material.AIR || template.frameHeight() <= 0) {
            return blocks;
        }
        for (int row = BoardGeometry.ROOM_MIN_ROW; row <= BoardGeometry.ROOM_MAX_ROW; row++) {
            for (int column = BoardGeometry.ROOM_MIN_COLUMN; column <= BoardGeometry.ROOM_MAX_COLUMN; column++) {
                boolean edge = row == BoardGeometry.ROOM_MIN_ROW
                    || row == BoardGeometry.ROOM_MAX_ROW
                    || column == BoardGeometry.ROOM_MIN_COLUMN
                    || column == BoardGeometry.ROOM_MAX_COLUMN;
                if (!edge || template.isEntrance(row, column, geometry.boardSize())) {
                    continue;
                }
                BlockPoint base = geometry.boardPoint(row, column);
                for (int height = 0; height < template.frameHeight(); height++) {
                    blocks.add(new EnvironmentBlock(base.add(new BlockPoint(0, height, 0)), template.frameMaterial()));
                }
            }
        }
        return blocks;
    }

    public List<EnvironmentBlock> decorBlocks() {
        List<EnvironmentBlock> blocks = new ArrayList<>();
        for (RoomEnvironmentTemplate.DecorElement element : template.decor()) {
            if (element.material() == Material.AIR) {
                continue;
            }
            BlockPoint base = geometry.boardPoint(element.row(), element.column()).add(new BlockPoint(0, element.yOffset(), 0));
            if (element.type() == RoomEnvironmentTemplate.DecorType.COLUMN) {
                for (int height = 0; height < element.height(); height++) {
                    blocks.add(new EnvironmentBlock(base.add(new BlockPoint(0, height, 0)), element.material()));
                }
            } else {
                blocks.add(new EnvironmentBlock(base, element.material()));
            }
        }
        return blocks;
    }

    public List<EnvironmentBlock> allBlocks() {
        List<EnvironmentBlock> blocks = new ArrayList<>();
        blocks.addAll(floorBlocks());
        blocks.addAll(frameBlocks());
        blocks.addAll(decorBlocks());
        return blocks;
    }

    public boolean withinBudget() {
        return allBlocks().size() <= template.blockBudget();
    }

    public boolean protects(BlockPoint point) {
        Set<BlockPoint> points = new HashSet<>();
        for (EnvironmentBlock block : allBlocks()) {
            points.add(block.point());
        }
        return points.contains(point);
    }

    public record EnvironmentBlock(BlockPoint point, Material material) {
    }
}
