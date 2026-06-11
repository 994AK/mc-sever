package net.leafmc.friends;

public final class TestSupport {
    private TestSupport() {
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
