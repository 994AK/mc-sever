package net.leafmc.gomoku;

import org.bukkit.Location;
import org.bukkit.World;

public record ArenaSeat(double x, double y, double z, float yaw, float pitch) {
    public Location location(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
