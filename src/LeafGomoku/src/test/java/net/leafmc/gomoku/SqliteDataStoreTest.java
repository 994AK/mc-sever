package net.leafmc.gomoku;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

public final class SqliteDataStoreTest {
    public static void main(String[] args) throws Exception {
        recordsStatsHistoryMovesAndPointLedger();
        spendsPointsAtomically();
        adminAddsPointsWithLedger();
    }

    private static void recordsStatsHistoryMovesAndPointLedger() throws Exception {
        File database = Files.createTempDirectory("leafgomoku-db").resolve("data.db").toFile();
        SqliteDataStore store = new SqliteDataStore(database);
        store.init();
        UUID black = UUID.randomUUID();
        UUID white = UUID.randomUUID();
        MatchRecord record = new MatchRecord("match-1", "main", black, "Alice", white, "Bob", Stone.BLACK, false, "win", 100L);
        List<MatchMove> moves = List.of(
            new MatchMove(1, black, "Alice", Stone.BLACK, 0, 0, 90L),
            new MatchMove(2, white, "Bob", Stone.WHITE, 1, 0, 95L)
        );

        TestSupport.check(store.recordMatch(record, moves, 3, 1, 0), "match inserted");
        TestSupport.check(!store.recordMatch(record, moves, 3, 1, 0), "duplicate ignored");
        TestSupport.check(store.findStats(black).orElseThrow().points() == 3, "winner points");
        TestSupport.check(store.findStats(white).orElseThrow().losses() == 1, "loser loss");
        TestSupport.check(count(database, "match_moves") == 2, "moves persisted");
        TestSupport.check(count(database, "point_ledger") == 1, "non-zero point delta ledger persisted");
    }

    private static void spendsPointsAtomically() throws Exception {
        File database = Files.createTempDirectory("leafgomoku-spend").resolve("data.db").toFile();
        SqliteDataStore store = new SqliteDataStore(database);
        store.init();
        UUID player = UUID.randomUUID();
        PlayerStats stats = new PlayerStats(player, "Alice");
        stats.load(1, 1, 0, 0, 10, 1, 1, 100L);
        store.saveStats(stats);

        TestSupport.check(store.trySpendPoints(player, "Alice", 7, "appearance", "skin"), "spend accepted");
        TestSupport.check(store.findStats(player).orElseThrow().points() == 3, "points deducted");
        TestSupport.check(!store.trySpendPoints(player, "Alice", 4, "appearance", "skin2"), "overspend rejected");
        TestSupport.check(store.findStats(player).orElseThrow().points() == 3, "rejected spend unchanged");
    }

    private static void adminAddsPointsWithLedger() throws Exception {
        File database = Files.createTempDirectory("leafgomoku-grant").resolve("data.db").toFile();
        SqliteDataStore store = new SqliteDataStore(database);
        store.init();
        UUID player = UUID.randomUUID();

        PlayerStats created = store.addPoints(player, "Alice", 25, "admin-grant:test", "grant-1");
        TestSupport.check(created.points() == 25, "admin grant creates points");
        TestSupport.check(store.findStats(player).orElseThrow().points() == 25, "admin grant persisted");
        TestSupport.check(count(database, "point_ledger") == 1, "admin grant ledger persisted");

        PlayerStats updated = store.addPoints(player, "Alice", 5, "admin-grant:test", "grant-2");
        TestSupport.check(updated.points() == 30, "admin grant increments existing points");
        TestSupport.check(count(database, "point_ledger") == 2, "second admin grant ledger persisted");
    }

    private static int count(File database, String table) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }
}
