package net.leafmc.gomoku;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public final class StatsServiceTest {
    public static void main(String[] args) throws Exception {
        recordsScoredMatchesOnceAndReloads();
        leaderboardUsesDeterministicTieBreakers();
        migratesLegacyStatsYmlOnce();
    }

    private static void recordsScoredMatchesOnceAndReloads() throws Exception {
        Path dir = Files.createTempDirectory("leafgomoku-stats");
        StatsService stats = new StatsService(dir.resolve("data.db").toFile(), dir.resolve("stats.yml").toFile());
        stats.load();
        UUID black = UUID.randomUUID();
        UUID white = UUID.randomUUID();

        MatchRecord record = new MatchRecord("match-1", "main", black, "Alice", white, "Bob", Stone.BLACK, false, "win", 100L);
        TestSupport.check(stats.record(record), "first record accepted");
        TestSupport.check(!stats.record(record), "duplicate record ignored");

        StatsService reloaded = new StatsService(dir.resolve("data.db").toFile(), dir.resolve("stats.yml").toFile());
        reloaded.load();
        PlayerStats blackStats = reloaded.find(black).orElseThrow();
        PlayerStats whiteStats = reloaded.find(white).orElseThrow();
        TestSupport.check(blackStats.games() == 1 && blackStats.wins() == 1 && blackStats.points() == 3, "black win persisted once");
        TestSupport.check(whiteStats.games() == 1 && whiteStats.losses() == 1 && whiteStats.points() == 0, "white loss persisted once");
    }

    private static void leaderboardUsesDeterministicTieBreakers() throws Exception {
        Path dir = Files.createTempDirectory("leafgomoku-ranking");
        StatsService stats = new StatsService(dir.resolve("data.db").toFile(), dir.resolve("stats.yml").toFile());
        stats.load();
        UUID alpha = UUID.randomUUID();
        UUID beta = UUID.randomUUID();

        stats.record(new MatchRecord("match-a", "main", alpha, "Alpha", UUID.randomUUID(), "LoserA", Stone.BLACK, false, "win", 100L));
        stats.record(new MatchRecord("match-b", "main", beta, "Beta", UUID.randomUUID(), "LoserB", Stone.BLACK, false, "win", 101L));

        List<PlayerStats> rows = stats.leaderboard("points", 2);
        TestSupport.check(rows.size() == 2, "two leaderboard rows");
        TestSupport.check(rows.get(0).playerName().equals("Alpha"), "same score sorts by name");
        TestSupport.check(rows.get(1).playerName().equals("Beta"), "same score second row");
    }

    private static void migratesLegacyStatsYmlOnce() throws Exception {
        Path dir = Files.createTempDirectory("leafgomoku-legacy");
        File legacy = dir.resolve("stats.yml").toFile();
        UUID player = UUID.randomUUID();
        YamlConfiguration yaml = new YamlConfiguration();
        String path = "players." + player;
        yaml.set(path + ".name", "Legacy");
        yaml.set(path + ".games", 2);
        yaml.set(path + ".wins", 1);
        yaml.set(path + ".losses", 1);
        yaml.set(path + ".draws", 0);
        yaml.set(path + ".points", 3);
        yaml.set(path + ".current-streak", 0);
        yaml.set(path + ".best-streak", 1);
        yaml.set(path + ".last-played", 100L);
        yaml.set("records.old-match", true);
        yaml.save(legacy);

        StatsService stats = new StatsService(dir.resolve("data.db").toFile(), legacy);
        stats.load();
        TestSupport.check(stats.find(player).orElseThrow().points() == 3, "legacy points migrated");
        TestSupport.check(stats.dataStore().hasMatch("old-match"), "legacy match remembered");

        StatsService reloaded = new StatsService(dir.resolve("data.db").toFile(), legacy);
        reloaded.load();
        TestSupport.check(reloaded.find(player).orElseThrow().games() == 2, "migration not duplicated");
    }
}
