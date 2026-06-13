package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BoardGeometry {
    public static final int ROOM_MIN_ROW = -8;
    public static final int ROOM_MAX_ROW = 18;
    public static final int ROOM_MIN_COLUMN = -4;
    public static final int ROOM_MAX_COLUMN = 18;
    public static final int ROOM_FRAME_HEIGHT = 3;

    private final BlockPoint boardOrigin;
    private final BlockPoint boardRowStep;
    private final BlockPoint boardColumnStep;
    private final BlockPoint previewOrigin;
    private final BlockPoint previewRowStep;
    private final BlockPoint previewColumnStep;
    private final int boardSize;

    public BoardGeometry(
        BlockPoint boardOrigin,
        BlockPoint boardRowStep,
        BlockPoint boardColumnStep,
        BlockPoint previewOrigin,
        BlockPoint previewRowStep,
        BlockPoint previewColumnStep
    ) {
        this(
            boardOrigin,
            boardRowStep,
            boardColumnStep,
            previewOrigin,
            previewRowStep,
            previewColumnStep,
            GomokuBoard.DEFAULT_SIZE
        );
    }

    public BoardGeometry(
        BlockPoint boardOrigin,
        BlockPoint boardRowStep,
        BlockPoint boardColumnStep,
        BlockPoint previewOrigin,
        BlockPoint previewRowStep,
        BlockPoint previewColumnStep,
        int boardSize
    ) {
        this.boardOrigin = boardOrigin;
        this.boardRowStep = boardRowStep;
        this.boardColumnStep = boardColumnStep;
        this.previewOrigin = previewOrigin;
        this.previewRowStep = previewRowStep;
        this.previewColumnStep = previewColumnStep;
        this.boardSize = GomokuBoard.requireValidSize(boardSize);
    }

    public Optional<GridCell> mapBoardCell(BlockPoint point) {
        for (int row = 0; row < boardSize; row++) {
            for (int column = 0; column < boardSize; column++) {
                if (boardPoint(row, column).equals(point)) {
                    return Optional.of(new GridCell(row, column));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<GridCell> mapPieceCell(BlockPoint point) {
        for (int row = 0; row < boardSize; row++) {
            for (int column = 0; column < boardSize; column++) {
                if (piecePoint(row, column).equals(point)) {
                    return Optional.of(new GridCell(row, column));
                }
            }
        }
        return Optional.empty();
    }

    public boolean protects(BlockPoint point) {
        if (mapBoardCell(point).isPresent()) {
            return true;
        }
        if (mapPieceCell(point).isPresent()) {
            return true;
        }
        return false;
    }

    public List<BlockPoint> roomFloorPoints() {
        List<BlockPoint> points = new ArrayList<>();
        for (int row = ROOM_MIN_ROW; row <= ROOM_MAX_ROW; row++) {
            for (int column = ROOM_MIN_COLUMN; column <= ROOM_MAX_COLUMN; column++) {
                points.add(boardPoint(row, column));
            }
        }
        return points;
    }

    public List<BlockPoint> roomFramePoints() {
        List<BlockPoint> points = new ArrayList<>();
        for (int row = ROOM_MIN_ROW; row <= ROOM_MAX_ROW; row++) {
            for (int column = ROOM_MIN_COLUMN; column <= ROOM_MAX_COLUMN; column++) {
                boolean edge = row == ROOM_MIN_ROW || row == ROOM_MAX_ROW || column == ROOM_MIN_COLUMN || column == ROOM_MAX_COLUMN;
                if (!edge) {
                    continue;
                }
                BlockPoint base = boardPoint(row, column);
                for (int height = 0; height < ROOM_FRAME_HEIGHT; height++) {
                    points.add(base.add(new BlockPoint(0, height, 0)));
                }
            }
        }
        return points;
    }

    public BlockPoint boardPoint(int row, int column) {
        return boardOrigin.add(boardRowStep.multiply(row)).add(boardColumnStep.multiply(column));
    }

    public BlockPoint piecePoint(int row, int column) {
        return boardPoint(row, column).add(new BlockPoint(0, 1, 0));
    }

    public BlockPoint previewPoint(int row, int column) {
        return previewOrigin.add(previewRowStep.multiply(row)).add(previewColumnStep.multiply(column));
    }

    public BoardRegion boardRegion() {
        BlockPoint[] corners = {
            boardPoint(0, 0),
            boardPoint(boardSize - 1, 0),
            boardPoint(0, boardSize - 1),
            boardPoint(boardSize - 1, boardSize - 1)
        };
        int minX = corners[0].x();
        int minY = corners[0].y();
        int minZ = corners[0].z();
        int maxX = corners[0].x();
        int maxY = corners[0].y();
        int maxZ = corners[0].z();
        for (BlockPoint corner : corners) {
            minX = Math.min(minX, corner.x());
            minY = Math.min(minY, corner.y());
            minZ = Math.min(minZ, corner.z());
            maxX = Math.max(maxX, corner.x());
            maxY = Math.max(maxY, corner.y());
            maxZ = Math.max(maxZ, corner.z());
        }
        return new BoardRegion(new BlockPoint(minX, minY, minZ), new BlockPoint(maxX, maxY, maxZ));
    }

    public BlockPoint boardOrigin() {
        return boardOrigin;
    }

    public BlockPoint boardRowStep() {
        return boardRowStep;
    }

    public BlockPoint boardColumnStep() {
        return boardColumnStep;
    }

    public BlockPoint previewOrigin() {
        return previewOrigin;
    }

    public BlockPoint previewRowStep() {
        return previewRowStep;
    }

    public BlockPoint previewColumnStep() {
        return previewColumnStep;
    }

    public int boardSize() {
        return boardSize;
    }

    public record BoardRegion(BlockPoint minimum, BlockPoint maximum) {
    }

    public static BlockPoint stepFromAxis(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return switch (normalized) {
            case "+x", "x", "east" -> new BlockPoint(1, 0, 0);
            case "-x", "west" -> new BlockPoint(-1, 0, 0);
            case "+y", "up" -> new BlockPoint(0, 1, 0);
            case "-y", "down" -> new BlockPoint(0, -1, 0);
            case "+z", "z", "south" -> new BlockPoint(0, 0, 1);
            case "-z", "north" -> new BlockPoint(0, 0, -1);
            default -> throw new IllegalArgumentException("Unsupported axis: " + value);
        };
    }

    public static String axisName(BlockPoint step) {
        if (step.equals(new BlockPoint(1, 0, 0))) {
            return "+x";
        }
        if (step.equals(new BlockPoint(-1, 0, 0))) {
            return "-x";
        }
        if (step.equals(new BlockPoint(0, 1, 0))) {
            return "+y";
        }
        if (step.equals(new BlockPoint(0, -1, 0))) {
            return "-y";
        }
        if (step.equals(new BlockPoint(0, 0, 1))) {
            return "+z";
        }
        if (step.equals(new BlockPoint(0, 0, -1))) {
            return "-z";
        }
        throw new IllegalArgumentException("Unsupported axis step: " + step);
    }
}
