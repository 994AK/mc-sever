package net.leafmc.gomoku;

public final class BoardGeometryTest {
    public static void main(String[] args) {
        mapsOriginAndFarCorner();
        mapsPieceLayerAboveBoard();
        rejectsOutsideCells();
        mapsPreviewCells();
        protectsOnlyBoardAndPieceLayer();
        mapsCustomBoardSizeAndRegion();
        supportsReversedAxis();
        serializesAxisNames();
    }

    private static void mapsOriginAndFarCorner() {
        BoardGeometry geometry = geometry();
        TestSupport.check(geometry.mapBoardCell(new BlockPoint(10, 70, 20)).orElseThrow().equals(new GridCell(0, 0)), "origin cell");
        TestSupport.check(geometry.mapBoardCell(new BlockPoint(24, 70, 34)).orElseThrow().equals(new GridCell(14, 14)), "far corner cell");
    }

    private static void mapsPieceLayerAboveBoard() {
        BoardGeometry geometry = geometry();
        TestSupport.check(geometry.piecePoint(2, 3).equals(new BlockPoint(13, 71, 22)), "piece above board");
        TestSupport.check(geometry.mapPieceCell(new BlockPoint(13, 71, 22)).orElseThrow().equals(new GridCell(2, 3)), "piece layer maps cell");
        TestSupport.check(geometry.mapBoardCell(new BlockPoint(13, 71, 22)).isEmpty(), "piece layer is not board layer");
        TestSupport.check(geometry.protects(new BlockPoint(13, 71, 22)), "piece layer protected");
    }

    private static void rejectsOutsideCells() {
        BoardGeometry geometry = geometry();
        TestSupport.check(geometry.mapBoardCell(new BlockPoint(9, 70, 20)).isEmpty(), "outside west");
        TestSupport.check(geometry.mapBoardCell(new BlockPoint(25, 70, 34)).isEmpty(), "outside east");
        TestSupport.check(geometry.mapBoardCell(new BlockPoint(10, 71, 20)).isEmpty(), "wrong y");
    }

    private static void mapsPreviewCells() {
        BoardGeometry geometry = geometry();
        TestSupport.check(geometry.previewPoint(2, 3).equals(new BlockPoint(103, 88, 50)), "legacy preview point");
        TestSupport.check(!geometry.protects(new BlockPoint(103, 88, 50)), "legacy preview no longer protected");
    }

    private static void protectsOnlyBoardAndPieceLayer() {
        BoardGeometry geometry = geometry();
        TestSupport.check(geometry.protects(new BlockPoint(10, 70, 20)), "board protected");
        TestSupport.check(geometry.protects(new BlockPoint(10, 71, 20)), "piece layer protected");
        TestSupport.check(!geometry.protects(new BlockPoint(6, 71, 12)), "old room frame no longer protected");
        TestSupport.check(!geometry.protects(new BlockPoint(7, 70, 13)), "old room floor no longer protected");
    }

    private static void mapsCustomBoardSizeAndRegion() {
        BoardGeometry geometry = new BoardGeometry(
            new BlockPoint(10, 70, 20),
            BoardGeometry.stepFromAxis("+z"),
            BoardGeometry.stepFromAxis("+x"),
            new BlockPoint(100, 90, 50),
            BoardGeometry.stepFromAxis("-y"),
            BoardGeometry.stepFromAxis("+x"),
            19
        );

        TestSupport.check(geometry.boardSize() == 19, "custom size stored");
        TestSupport.check(geometry.mapBoardCell(new BlockPoint(28, 70, 38)).orElseThrow().equals(new GridCell(18, 18)), "custom far corner maps");
        TestSupport.check(geometry.mapBoardCell(new BlockPoint(29, 70, 38)).isEmpty(), "custom outside rejected");
        BoardGeometry.BoardRegion region = geometry.boardRegion();
        TestSupport.check(region.minimum().equals(new BlockPoint(10, 70, 20)), "preview min corner");
        TestSupport.check(region.maximum().equals(new BlockPoint(28, 70, 38)), "preview max corner");
    }

    private static void supportsReversedAxis() {
        BoardGeometry geometry = new BoardGeometry(
            new BlockPoint(0, 80, 0),
            BoardGeometry.stepFromAxis("-z"),
            BoardGeometry.stepFromAxis("-x"),
            new BlockPoint(0, 90, 0),
            BoardGeometry.stepFromAxis("-y"),
            BoardGeometry.stepFromAxis("+x")
        );
        TestSupport.check(geometry.boardPoint(2, 3).equals(new BlockPoint(-3, 80, -2)), "reversed point");
        TestSupport.check(geometry.mapBoardCell(new BlockPoint(-3, 80, -2)).orElseThrow().equals(new GridCell(2, 3)), "reversed map");
    }

    private static void serializesAxisNames() {
        TestSupport.check(BoardGeometry.axisName(new BlockPoint(1, 0, 0)).equals("+x"), "positive x axis");
        TestSupport.check(BoardGeometry.axisName(new BlockPoint(0, 0, -1)).equals("-z"), "negative z axis");
    }

    private static BoardGeometry geometry() {
        return new BoardGeometry(
            new BlockPoint(10, 70, 20),
            BoardGeometry.stepFromAxis("+z"),
            BoardGeometry.stepFromAxis("+x"),
            new BlockPoint(100, 90, 50),
            BoardGeometry.stepFromAxis("-y"),
            BoardGeometry.stepFromAxis("+x")
        );
    }
}
