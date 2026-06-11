package net.leafmc.gomoku;

import java.util.UUID;

public record MatchRecord(
    String matchId,
    String roomId,
    UUID blackId,
    String blackName,
    UUID whiteId,
    String whiteName,
    Stone winner,
    boolean draw,
    String reason,
    long finishedAtMillis
) {
    public boolean scored() {
        return draw || winner == Stone.BLACK || winner == Stone.WHITE;
    }
}
