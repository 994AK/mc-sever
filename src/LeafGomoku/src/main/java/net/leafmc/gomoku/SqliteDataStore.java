package net.leafmc.gomoku;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteDataStore implements DataStore {
    private final File databaseFile;

    public SqliteDataStore(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    @Override
    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException("Missing sqlite-jdbc runtime dependency for LeafGomoku data.db", error);
        }
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    player_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    games INTEGER NOT NULL DEFAULT 0,
                    wins INTEGER NOT NULL DEFAULT 0,
                    losses INTEGER NOT NULL DEFAULT 0,
                    draws INTEGER NOT NULL DEFAULT 0,
                    points INTEGER NOT NULL DEFAULT 0,
                    current_streak INTEGER NOT NULL DEFAULT 0,
                    best_streak INTEGER NOT NULL DEFAULT 0,
                    last_played INTEGER NOT NULL DEFAULT 0
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS match_history (
                    match_id TEXT PRIMARY KEY,
                    room_id TEXT NOT NULL,
                    black_id TEXT,
                    black_name TEXT,
                    white_id TEXT,
                    white_name TEXT,
                    winner TEXT NOT NULL DEFAULT 'EMPTY',
                    draw INTEGER NOT NULL DEFAULT 0,
                    reason TEXT NOT NULL DEFAULT '',
                    finished_at INTEGER NOT NULL DEFAULT 0,
                    legacy INTEGER NOT NULL DEFAULT 0
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS match_moves (
                    match_id TEXT NOT NULL,
                    move_index INTEGER NOT NULL,
                    player_id TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    side TEXT NOT NULL,
                    row INTEGER NOT NULL,
                    column INTEGER NOT NULL,
                    played_at INTEGER NOT NULL,
                    PRIMARY KEY (match_id, move_index)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS appearance_unlocks (
                    player_id TEXT NOT NULL,
                    appearance_type TEXT NOT NULL,
                    appearance_id TEXT NOT NULL,
                    unlocked_at INTEGER NOT NULL,
                    PRIMARY KEY (player_id, appearance_type, appearance_id)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS appearance_preferences (
                    player_id TEXT PRIMARY KEY,
                    board_theme_id TEXT NOT NULL DEFAULT '',
                    piece_skin_id TEXT NOT NULL DEFAULT '',
                    updated_at INTEGER NOT NULL
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS point_ledger (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_id TEXT NOT NULL,
                    delta INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    reference_id TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);
        } catch (SQLException error) {
            throw new IllegalStateException("Could not initialize " + databaseFile, error);
        }
    }

    @Override
    public Optional<String> metadata(String key) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("SELECT value FROM metadata WHERE key = ?")) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(result.getString("value")) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not read metadata " + key, error);
        }
    }

    @Override
    public void setMetadata(String key, String value) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO metadata(key, value) VALUES(?, ?)
                 ON CONFLICT(key) DO UPDATE SET value = excluded.value
                 """)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Could not write metadata " + key, error);
        }
    }

    @Override
    public Optional<PlayerStats> findStats(UUID playerId) {
        try (Connection connection = connection()) {
            return loadStats(connection, playerId);
        } catch (SQLException error) {
            throw new IllegalStateException("Could not read player stats " + playerId, error);
        }
    }

    @Override
    public Collection<PlayerStats> allStats() {
        List<PlayerStats> rows = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM players");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                rows.add(readStats(result));
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not read all player stats", error);
        }
        return rows;
    }

    @Override
    public void saveStats(PlayerStats stats) {
        try (Connection connection = connection()) {
            saveStats(connection, stats);
        } catch (SQLException error) {
            throw new IllegalStateException("Could not save player stats " + stats.playerId(), error);
        }
    }

    @Override
    public void deleteStats(UUID playerId) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM players WHERE player_id = ?")) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Could not delete player stats " + playerId, error);
        }
    }

    @Override
    public PlayerStats addPoints(UUID playerId, String playerName, int amount, String reason, String referenceId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                PlayerStats stats = loadStats(connection, playerId)
                    .orElseGet(() -> new PlayerStats(playerId, safeName(playerId, playerName)));
                stats.load(
                    stats.games(),
                    stats.wins(),
                    stats.losses(),
                    stats.draws(),
                    Math.addExact(stats.points(), amount),
                    stats.currentStreak(),
                    stats.bestStreak(),
                    stats.lastPlayedMillis()
                );
                saveStats(connection, stats);
                insertLedger(connection, playerId, amount, reason == null || reason.isBlank() ? "admin-grant" : reason, referenceId);
                connection.commit();
                return stats;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not add points for " + playerId, error);
        }
    }

    @Override
    public boolean hasMatch(String matchId) {
        try (Connection connection = connection()) {
            return hasMatch(connection, matchId);
        } catch (SQLException error) {
            throw new IllegalStateException("Could not check match " + matchId, error);
        }
    }

    @Override
    public void rememberLegacyMatch(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return;
        }
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT OR IGNORE INTO match_history(
                     match_id, room_id, winner, draw, reason, finished_at, legacy
                 ) VALUES(?, 'legacy', 'EMPTY', 0, 'stats-yml-migration', 0, 1)
                 """)) {
            statement.setString(1, matchId);
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Could not migrate legacy match " + matchId, error);
        }
    }

    @Override
    public boolean recordMatch(MatchRecord record, List<MatchMove> moves, int winPoints, int drawPoints, int lossPoints) {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                if (hasMatch(connection, record.matchId())) {
                    connection.rollback();
                    return false;
                }
                insertMatch(connection, record);
                insertMoves(connection, record.matchId(), moves);
                applyStats(connection, record, winPoints, drawPoints, lossPoints);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not record match " + record.matchId(), error);
        }
    }

    @Override
    public boolean isAppearanceUnlocked(UUID playerId, String type, String appearanceId) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                 SELECT 1 FROM appearance_unlocks
                 WHERE player_id = ? AND appearance_type = ? AND appearance_id = ?
                 """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type);
            statement.setString(3, appearanceId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not read appearance unlock", error);
        }
    }

    @Override
    public void unlockAppearance(UUID playerId, String type, String appearanceId) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT OR IGNORE INTO appearance_unlocks(player_id, appearance_type, appearance_id, unlocked_at)
                 VALUES(?, ?, ?, ?)
                 """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, type);
            statement.setString(3, appearanceId);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Could not write appearance unlock", error);
        }
    }

    @Override
    public PlayerAppearanceState appearanceState(UUID playerId) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                 SELECT board_theme_id, piece_skin_id FROM appearance_preferences WHERE player_id = ?
                 """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return PlayerAppearanceState.empty();
                }
                return new PlayerAppearanceState(result.getString("board_theme_id"), result.getString("piece_skin_id"));
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not read appearance preferences " + playerId, error);
        }
    }

    @Override
    public void setPreferredBoardTheme(UUID playerId, String themeId) {
        updatePreference(playerId, themeId, null);
    }

    @Override
    public void setPreferredPieceSkin(UUID playerId, String skinId) {
        updatePreference(playerId, null, skinId);
    }

    @Override
    public boolean trySpendPoints(UUID playerId, String playerName, int cost, String reason, String referenceId) {
        if (cost <= 0) {
            return true;
        }
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                PlayerStats stats = loadStats(connection, playerId)
                    .orElseGet(() -> new PlayerStats(playerId, safeName(playerId, playerName)));
                if (stats.points() < cost) {
                    connection.rollback();
                    return false;
                }
                stats.load(
                    stats.games(),
                    stats.wins(),
                    stats.losses(),
                    stats.draws(),
                    stats.points() - cost,
                    stats.currentStreak(),
                    stats.bestStreak(),
                    stats.lastPlayedMillis()
                );
                saveStats(connection, stats);
                insertLedger(connection, playerId, -cost, reason, referenceId);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not spend points for " + playerId, error);
        }
    }

    @Override
    public void close() {
    }

    private Connection connection() throws SQLException {
        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    private Optional<PlayerStats> loadStats(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE player_id = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readStats(result)) : Optional.empty();
            }
        }
    }

    private PlayerStats readStats(ResultSet result) throws SQLException {
        PlayerStats stats = new PlayerStats(UUID.fromString(result.getString("player_id")), result.getString("name"));
        stats.load(
            result.getInt("games"),
            result.getInt("wins"),
            result.getInt("losses"),
            result.getInt("draws"),
            result.getInt("points"),
            result.getInt("current_streak"),
            result.getInt("best_streak"),
            result.getLong("last_played")
        );
        return stats;
    }

    private void saveStats(Connection connection, PlayerStats stats) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO players(
                player_id, name, games, wins, losses, draws, points, current_streak, best_streak, last_played
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_id) DO UPDATE SET
                name = excluded.name,
                games = excluded.games,
                wins = excluded.wins,
                losses = excluded.losses,
                draws = excluded.draws,
                points = excluded.points,
                current_streak = excluded.current_streak,
                best_streak = excluded.best_streak,
                last_played = excluded.last_played
            """)) {
            statement.setString(1, stats.playerId().toString());
            statement.setString(2, stats.playerName());
            statement.setInt(3, stats.games());
            statement.setInt(4, stats.wins());
            statement.setInt(5, stats.losses());
            statement.setInt(6, stats.draws());
            statement.setInt(7, stats.points());
            statement.setInt(8, stats.currentStreak());
            statement.setInt(9, stats.bestStreak());
            statement.setLong(10, stats.lastPlayedMillis());
            statement.executeUpdate();
        }
    }

    private boolean hasMatch(Connection connection, String matchId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM match_history WHERE match_id = ?")) {
            statement.setString(1, matchId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void insertMatch(Connection connection, MatchRecord record) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO match_history(
                match_id, room_id, black_id, black_name, white_id, white_name,
                winner, draw, reason, finished_at, legacy
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
            """)) {
            statement.setString(1, record.matchId());
            statement.setString(2, record.roomId());
            statement.setString(3, record.blackId().toString());
            statement.setString(4, record.blackName());
            statement.setString(5, record.whiteId().toString());
            statement.setString(6, record.whiteName());
            statement.setString(7, record.winner().name());
            statement.setInt(8, record.draw() ? 1 : 0);
            statement.setString(9, record.reason());
            statement.setLong(10, record.finishedAtMillis());
            statement.executeUpdate();
        }
    }

    private void insertMoves(Connection connection, String matchId, List<MatchMove> moves) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO match_moves(
                match_id, move_index, player_id, player_name, side, row, column, played_at
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            for (MatchMove move : moves) {
                statement.setString(1, matchId);
                statement.setInt(2, move.moveIndex());
                statement.setString(3, move.playerId().toString());
                statement.setString(4, move.playerName());
                statement.setString(5, move.side().name());
                statement.setInt(6, move.row());
                statement.setInt(7, move.column());
                statement.setLong(8, move.playedAtMillis());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void applyStats(Connection connection, MatchRecord record, int winPoints, int drawPoints, int lossPoints) throws SQLException {
        long now = record.finishedAtMillis();
        PlayerStats black = loadStats(connection, record.blackId()).orElseGet(() -> new PlayerStats(record.blackId(), record.blackName()));
        PlayerStats white = loadStats(connection, record.whiteId()).orElseGet(() -> new PlayerStats(record.whiteId(), record.whiteName()));
        int blackBefore = black.points();
        int whiteBefore = white.points();
        if (record.draw()) {
            black.applyDraw(record.blackName(), drawPoints, now);
            white.applyDraw(record.whiteName(), drawPoints, now);
        } else if (record.winner() == Stone.BLACK) {
            black.applyWin(record.blackName(), winPoints, now);
            white.applyLoss(record.whiteName(), lossPoints, now);
        } else if (record.winner() == Stone.WHITE) {
            white.applyWin(record.whiteName(), winPoints, now);
            black.applyLoss(record.blackName(), lossPoints, now);
        }
        saveStats(connection, black);
        saveStats(connection, white);
        insertLedger(connection, black.playerId(), black.points() - blackBefore, "match:" + record.reason(), record.matchId());
        insertLedger(connection, white.playerId(), white.points() - whiteBefore, "match:" + record.reason(), record.matchId());
    }

    private void updatePreference(UUID playerId, String boardThemeId, String pieceSkinId) {
        PlayerAppearanceState current = appearanceState(playerId);
        String nextTheme = boardThemeId == null ? current.boardThemeId() : boardThemeId;
        String nextSkin = pieceSkinId == null ? current.pieceSkinId() : pieceSkinId;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO appearance_preferences(player_id, board_theme_id, piece_skin_id, updated_at)
                 VALUES(?, ?, ?, ?)
                 ON CONFLICT(player_id) DO UPDATE SET
                     board_theme_id = excluded.board_theme_id,
                     piece_skin_id = excluded.piece_skin_id,
                     updated_at = excluded.updated_at
                 """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, nextTheme == null ? "" : nextTheme);
            statement.setString(3, nextSkin == null ? "" : nextSkin);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Could not write appearance preferences " + playerId, error);
        }
    }

    private void insertLedger(Connection connection, UUID playerId, int delta, String reason, String referenceId) throws SQLException {
        if (delta == 0) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO point_ledger(player_id, delta, reason, reference_id, created_at)
            VALUES(?, ?, ?, ?, ?)
            """)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, delta);
            statement.setString(3, reason == null ? "" : reason);
            statement.setString(4, referenceId == null ? "" : referenceId);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private String safeName(UUID playerId, String playerName) {
        return playerName == null || playerName.isBlank() ? playerId.toString() : playerName;
    }
}
