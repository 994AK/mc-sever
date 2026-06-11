package net.leafmc.gomoku;

import java.util.UUID;

public record MatchMove(
    int moveIndex,
    UUID playerId,
    String playerName,
    Stone side,
    int row,
    int column,
    long playedAtMillis
) {
}
