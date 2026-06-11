package net.leafmc.gomoku;

import org.bukkit.Location;

public final class RoomLayoutFactory {
    public ArenaConfig create(String roomId, Location anchor, ArenaConfig defaults) {
        BlockPoint origin = new BlockPoint(anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());
        BlockPoint forward = forwardStep(anchor.getYaw());
        BlockPoint right = rightStep(forward);

        BoardGeometry geometry = new BoardGeometry(
            origin,
            forward,
            right,
            origin,
            BoardGeometry.stepFromAxis("-y"),
            right
        );

        BlockPoint blackEmitter = origin.add(right.multiply(-3)).add(forward.multiply(7)).add(new BlockPoint(0, 1, 0));
        BlockPoint whiteEmitter = origin.add(right.multiply(17)).add(forward.multiply(7)).add(new BlockPoint(0, 1, 0));

        ArenaSeat blackSeat = seat(origin.add(right.multiply(7)).add(forward.multiply(-3)).add(new BlockPoint(0, 1, 0)), yawToFace(forward));
        ArenaSeat whiteSeat = seat(origin.add(right.multiply(7)).add(forward.multiply(17)).add(new BlockPoint(0, 1, 0)), yawToFace(forward.multiply(-1)));
        ArenaSeat spectatorSpawn = seat(origin.add(right.multiply(7)).add(forward.multiply(-7)).add(new BlockPoint(0, 1, 0)), yawToFace(forward));
        ArenaSeat spectatorExit = seat(origin.add(right.multiply(7)).add(forward.multiply(-9)).add(new BlockPoint(0, 1, 0)), yawToFace(forward));

        return ArenaConfig.create(
            roomId,
            anchor.getWorld() == null ? "world" : anchor.getWorld().getName(),
            geometry,
            blackEmitter,
            whiteEmitter,
            blackSeat,
            whiteSeat,
            spectatorSpawn,
            spectatorExit,
            defaults
        );
    }

    private ArenaSeat seat(BlockPoint point, float yaw) {
        return new ArenaSeat(point.x() + 0.5D, point.y(), point.z() + 0.5D, yaw, 0.0F);
    }

    private BlockPoint forwardStep(float yaw) {
        float normalized = ((yaw % 360.0F) + 360.0F) % 360.0F;
        if (normalized >= 315.0F || normalized < 45.0F) {
            return new BlockPoint(0, 0, 1);
        }
        if (normalized < 135.0F) {
            return new BlockPoint(-1, 0, 0);
        }
        if (normalized < 225.0F) {
            return new BlockPoint(0, 0, -1);
        }
        return new BlockPoint(1, 0, 0);
    }

    private BlockPoint rightStep(BlockPoint forward) {
        if (forward.equals(new BlockPoint(0, 0, 1))) {
            return new BlockPoint(1, 0, 0);
        }
        if (forward.equals(new BlockPoint(0, 0, -1))) {
            return new BlockPoint(-1, 0, 0);
        }
        if (forward.equals(new BlockPoint(1, 0, 0))) {
            return new BlockPoint(0, 0, -1);
        }
        return new BlockPoint(0, 0, 1);
    }

    private float yawToFace(BlockPoint direction) {
        if (direction.equals(new BlockPoint(0, 0, 1))) {
            return 0.0F;
        }
        if (direction.equals(new BlockPoint(-1, 0, 0))) {
            return 90.0F;
        }
        if (direction.equals(new BlockPoint(0, 0, -1))) {
            return 180.0F;
        }
        return -90.0F;
    }
}
