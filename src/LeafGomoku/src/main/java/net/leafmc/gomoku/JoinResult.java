package net.leafmc.gomoku;

public record JoinResult(JoinStatus status, Stone side, GameState state, Stone nextTurn) {
}
