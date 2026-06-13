package net.leafmc.gomoku;

import java.util.List;
import org.bukkit.Material;

public final class RoomEnvironmentLayoutTest {
    public static void main(String[] args) {
        buildsEntranceAndDecorBlocks();
        skipsAirOnlyTemplate();
        tracksBudgetAndProtection();
        rotatesWithBoardGeometry();
    }

    private static void buildsEntranceAndDecorBlocks() {
        RoomEnvironmentTemplate template = template();
        RoomEnvironmentLayout layout = new RoomEnvironmentLayout(geometry(), template);

        TestSupport.check(layout.floorBlocks().stream().anyMatch(block ->
            block.point().equals(new BlockPoint(17, 70, 12)) && block.material() == Material.GOLD_BLOCK
        ), "floor accent material");
        TestSupport.check(layout.frameBlocks().stream().noneMatch(block ->
            block.point().equals(new BlockPoint(17, 70, 12))
        ), "front entrance gap");
        TestSupport.check(layout.decorBlocks().stream().anyMatch(block ->
            block.point().equals(new BlockPoint(7, 72, 17)) && block.material() == Material.OAK_LOG
        ), "decor column top");
    }

    private static void tracksBudgetAndProtection() {
        RoomEnvironmentTemplate template = new RoomEnvironmentTemplate(
            "tiny",
            "Tiny",
            Material.STONE,
            List.of(),
            Material.GLASS,
            3,
            0,
            List.of(),
            EnvironmentFeedbackProfile.soft(),
            10
        );
        RoomEnvironmentLayout layout = new RoomEnvironmentLayout(geometry(), template);

        TestSupport.check(!layout.withinBudget(), "budget detects large template");
        TestSupport.check(layout.protects(new BlockPoint(7, 70, 12)), "floor point protected");
        TestSupport.check(!layout.protects(new BlockPoint(999, 70, 999)), "outside point not protected");
    }

    private static void skipsAirOnlyTemplate() {
        RoomEnvironmentTemplate template = new RoomEnvironmentTemplate(
            "board",
            "Board",
            Material.AIR,
            List.of(),
            Material.AIR,
            0,
            0,
            List.of(),
            EnvironmentFeedbackProfile.soft(),
            10
        );
        RoomEnvironmentLayout layout = new RoomEnvironmentLayout(geometry(), template);

        TestSupport.check(layout.allBlocks().isEmpty(), "air-only template does not clear outside board");
        TestSupport.check(layout.withinBudget(), "empty template within budget");
    }

    private static void rotatesWithBoardGeometry() {
        BoardGeometry rotated = new BoardGeometry(
            new BlockPoint(0, 80, 0),
            BoardGeometry.stepFromAxis("+x"),
            BoardGeometry.stepFromAxis("-z"),
            new BlockPoint(0, 80, 0),
            BoardGeometry.stepFromAxis("-y"),
            BoardGeometry.stepFromAxis("-z")
        );
        RoomEnvironmentLayout layout = new RoomEnvironmentLayout(rotated, template());

        TestSupport.check(layout.floorBlocks().stream().anyMatch(block ->
            block.point().equals(new BlockPoint(7, 80, -7))
        ), "rotated accent follows board axes");
    }

    private static RoomEnvironmentTemplate template() {
        return new RoomEnvironmentTemplate(
            "test",
            "Test",
            Material.STONE,
            List.of(new RoomEnvironmentTemplate.FloorAccent(-8, 18, 7, 7, Material.GOLD_BLOCK)),
            Material.GLASS,
            2,
            3,
            List.of(new RoomEnvironmentTemplate.DecorElement(
                RoomEnvironmentTemplate.DecorType.COLUMN,
                -3,
                -3,
                1,
                2,
                Material.OAK_LOG
            )),
            EnvironmentFeedbackProfile.soft(),
            1500
        );
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
