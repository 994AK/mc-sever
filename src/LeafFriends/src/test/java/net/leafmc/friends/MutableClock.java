package net.leafmc.friends;

public final class MutableClock implements TimeSource {
    private long nowMillis;

    public MutableClock(long nowMillis) {
        this.nowMillis = nowMillis;
    }

    @Override
    public long nowMillis() {
        return nowMillis;
    }

    public void advance(long millis) {
        nowMillis += millis;
    }
}
