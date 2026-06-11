package net.leafmc.gomoku;

public record BlockPoint(int x, int y, int z) {
    public BlockPoint add(BlockPoint other) {
        return new BlockPoint(x + other.x, y + other.y, z + other.z);
    }

    public BlockPoint multiply(int value) {
        return new BlockPoint(x * value, y * value, z * value);
    }
}
