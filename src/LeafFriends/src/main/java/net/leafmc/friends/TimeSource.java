package net.leafmc.friends;

@FunctionalInterface
public interface TimeSource {
    long nowMillis();

    static TimeSource system() {
        return System::currentTimeMillis;
    }
}
