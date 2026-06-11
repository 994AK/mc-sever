package net.leafmc.gomoku;

import java.util.UUID;

public final class PlayerStats {
    private final UUID playerId;
    private String playerName;
    private int games;
    private int wins;
    private int losses;
    private int draws;
    private int points;
    private int currentStreak;
    private int bestStreak;
    private long lastPlayedMillis;

    public PlayerStats(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public int games() {
        return games;
    }

    public int wins() {
        return wins;
    }

    public int losses() {
        return losses;
    }

    public int draws() {
        return draws;
    }

    public int points() {
        return points;
    }

    public int currentStreak() {
        return currentStreak;
    }

    public int bestStreak() {
        return bestStreak;
    }

    public long lastPlayedMillis() {
        return lastPlayedMillis;
    }

    public double winRate() {
        return games == 0 ? 0.0D : (double) wins / games;
    }

    public void applyWin(String name, int winPoints, long timestamp) {
        playerName = name;
        games++;
        wins++;
        points += winPoints;
        currentStreak++;
        bestStreak = Math.max(bestStreak, currentStreak);
        lastPlayedMillis = timestamp;
    }

    public void applyLoss(String name, int lossPoints, long timestamp) {
        playerName = name;
        games++;
        losses++;
        points += lossPoints;
        currentStreak = 0;
        lastPlayedMillis = timestamp;
    }

    public void applyDraw(String name, int drawPoints, long timestamp) {
        playerName = name;
        games++;
        draws++;
        points += drawPoints;
        currentStreak = 0;
        lastPlayedMillis = timestamp;
    }

    public void load(int games, int wins, int losses, int draws, int points, int currentStreak, int bestStreak, long lastPlayedMillis) {
        this.games = games;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.points = points;
        this.currentStreak = currentStreak;
        this.bestStreak = bestStreak;
        this.lastPlayedMillis = lastPlayedMillis;
    }
}
