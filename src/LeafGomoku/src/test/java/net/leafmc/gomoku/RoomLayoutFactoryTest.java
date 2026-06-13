package net.leafmc.gomoku;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RoomLayoutFactoryTest {
    public static void main(String[] args) {
        createsRoomFromCurrentYAndSouthFacing();
        createsRoomFromEastFacing();
        createsRoomWithCustomBoardSize();
        createsRoomWithEnvironmentTemplate();
        savesNewRoomsWithoutPreviewFields();
    }

    private static void createsRoomFromCurrentYAndSouthFacing() {
        RoomLayoutFactory factory = new RoomLayoutFactory();
        ArenaConfig room = factory.create("Room_2", new Location(null, 10.2D, 88.0D, 20.8D, 0.0F, 0.0F), null);

        TestSupport.check(room.id().equals("room_2"), "room id normalized");
        TestSupport.check(room.geometry().boardOrigin().equals(new BlockPoint(10, 88, 20)), "uses current block y");
        TestSupport.check(room.geometry().boardRowStep().equals(new BlockPoint(0, 0, 1)), "south row axis");
        TestSupport.check(room.geometry().boardColumnStep().equals(new BlockPoint(1, 0, 0)), "south column axis");
        TestSupport.check(room.blackEmitter().equals(new BlockPoint(7, 89, 27)), "black emitter from template");
        TestSupport.check(!room.spectatorSpawn().equals(room.blackSeat()), "spectator spawn separate from seat");
    }

    private static void createsRoomFromEastFacing() {
        RoomLayoutFactory factory = new RoomLayoutFactory();
        ArenaConfig room = factory.create("east", new Location(null, 0.0D, 70.0D, 0.0D, -90.0F, 0.0F), null);

        TestSupport.check(room.geometry().boardRowStep().equals(new BlockPoint(1, 0, 0)), "east row axis");
        TestSupport.check(room.geometry().boardColumnStep().equals(new BlockPoint(0, 0, -1)), "east right axis");
        TestSupport.check(room.geometry().boardPoint(2, 3).equals(new BlockPoint(2, 70, -3)), "east board point");
    }

    private static void createsRoomWithCustomBoardSize() {
        RoomLayoutFactory factory = new RoomLayoutFactory();
        ArenaConfig room = factory.create("large", new Location(null, 10.0D, 70.0D, 20.0D, 0.0F, 0.0F), null, 19);

        TestSupport.check(room.boardSize() == 19, "custom room size");
        TestSupport.check(room.geometry().boardPoint(18, 18).equals(new BlockPoint(28, 70, 38)), "custom board far corner");
        TestSupport.check(room.whiteEmitter().equals(new BlockPoint(31, 71, 29)), "white emitter follows size");
        BoardGeometry.BoardRegion region = room.geometry().boardRegion();
        TestSupport.check(region.minimum().equals(new BlockPoint(10, 70, 20)), "custom preview min");
        TestSupport.check(region.maximum().equals(new BlockPoint(28, 70, 38)), "custom preview max");
    }

    private static void savesNewRoomsWithoutPreviewFields() {
        RoomLayoutFactory factory = new RoomLayoutFactory();
        ArenaConfig room = factory.create("new_room", new Location(null, 0.0D, 70.0D, 0.0D, 0.0F, 0.0F), null);
        YamlConfiguration yaml = new YamlConfiguration();

        room.save(yaml);

        TestSupport.check(!yaml.contains("preview"), "preview section not saved");
        TestSupport.check(!yaml.contains("materials.empty-preview"), "empty preview material not saved");
        TestSupport.check(yaml.getInt("board.size") == GomokuBoard.DEFAULT_SIZE, "board size saved");
    }

    private static void createsRoomWithEnvironmentTemplate() {
        RoomLayoutFactory factory = new RoomLayoutFactory();
        ArenaConfig room = factory.create("garden_room", new Location(null, 0.0D, 70.0D, 0.0D, 0.0F, 0.0F), null, "garden");

        TestSupport.check(room.environmentTemplateId().equals("garden"), "environment template from create");
    }
}
