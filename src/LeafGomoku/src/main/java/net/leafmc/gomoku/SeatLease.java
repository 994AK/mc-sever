package net.leafmc.gomoku;

import java.util.UUID;

public final class SeatLease {
    private final Stone side;
    private final UUID playerId;
    private String playerName;
    private final long joinedAtMillis;
    private boolean disconnected;
    private long recoveryDeadlineMillis;

    public SeatLease(Stone side, UUID playerId, String playerName, long joinedAtMillis) {
        this.side = side;
        this.playerId = playerId;
        this.playerName = playerName;
        this.joinedAtMillis = joinedAtMillis;
    }

    public Stone side() {
        return side;
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public long joinedAtMillis() {
        return joinedAtMillis;
    }

    public boolean disconnected() {
        return disconnected;
    }

    public long recoveryDeadlineMillis() {
        return recoveryDeadlineMillis;
    }

    public void markOnline(String name) {
        playerName = name;
        disconnected = false;
        recoveryDeadlineMillis = 0L;
    }

    public void markDisconnected(long recoveryDeadlineMillis) {
        disconnected = true;
        this.recoveryDeadlineMillis = recoveryDeadlineMillis;
    }

    public boolean recoveryExpired(long nowMillis) {
        return disconnected && recoveryDeadlineMillis > 0L && nowMillis >= recoveryDeadlineMillis;
    }
}
