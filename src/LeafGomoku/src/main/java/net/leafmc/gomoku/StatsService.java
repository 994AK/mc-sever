package net.leafmc.gomoku;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class StatsService {
    private static final String LEGACY_STATS_MIGRATED = "stats_yml_migrated";

    private final DataStore dataStore;
    private final File legacyStatsFile;
    private final int winPoints;
    private final int drawPoints;
    private final int lossPoints;

    public StatsService(JavaPlugin plugin) {
        this(
            new SqliteDataStore(new File(plugin.getDataFolder(), "data.db")),
            new File(plugin.getDataFolder(), "stats.yml"),
            Math.max(0, plugin.getConfig().getInt("points.win", 3)),
            Math.max(0, plugin.getConfig().getInt("points.draw", 1)),
            Math.max(0, plugin.getConfig().getInt("points.loss", 0))
        );
    }

    StatsService(File databaseFile, File legacyStatsFile) {
        this(new SqliteDataStore(databaseFile), legacyStatsFile, 3, 1, 0);
    }

    StatsService(DataStore dataStore, File legacyStatsFile, int winPoints, int drawPoints, int lossPoints) {
        this.dataStore = dataStore;
        this.legacyStatsFile = legacyStatsFile;
        this.winPoints = winPoints;
        this.drawPoints = drawPoints;
        this.lossPoints = lossPoints;
    }

    public void load() {
        dataStore.init();
        migrateLegacyStats();
    }

    public boolean record(MatchRecord record) {
        return record(record, List.of());
    }

    public boolean record(MatchRecord record, List<MatchMove> moves) {
        if (!record.scored()) {
            return false;
        }
        return dataStore.recordMatch(record, moves, winPoints, drawPoints, lossPoints);
    }

    public PlayerStats statsFor(OfflinePlayer player) {
        UUID playerId = player.getUniqueId();
        return dataStore.findStats(playerId)
            .orElseGet(() -> new PlayerStats(playerId, player.getName() == null ? playerId.toString() : player.getName()));
    }

    public Optional<PlayerStats> find(UUID playerId) {
        return dataStore.findStats(playerId);
    }

    public List<PlayerStats> leaderboard(String metric, int limit) {
        Comparator<PlayerStats> comparator = switch (metric == null ? "" : metric.toLowerCase()) {
            case "wins" -> Comparator.comparingInt(PlayerStats::wins).reversed()
                .thenComparing(Comparator.comparingInt(PlayerStats::points).reversed())
                .thenComparing(PlayerStats::playerName, String.CASE_INSENSITIVE_ORDER);
            case "winrate", "win-rate" -> Comparator.comparingDouble(PlayerStats::winRate).reversed()
                .thenComparing(Comparator.comparingInt(PlayerStats::wins).reversed())
                .thenComparing(PlayerStats::playerName, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingInt(PlayerStats::points).reversed()
                .thenComparing(Comparator.comparingInt(PlayerStats::wins).reversed())
                .thenComparing(Comparator.comparingDouble(PlayerStats::winRate).reversed())
                .thenComparing(PlayerStats::playerName, String.CASE_INSENSITIVE_ORDER);
        };
        return dataStore.allStats().stream()
            .sorted(comparator)
            .limit(Math.max(0, limit))
            .toList();
    }

    public int rank(UUID playerId) {
        List<PlayerStats> ranked = new ArrayList<>(leaderboard("points", Integer.MAX_VALUE));
        for (int index = 0; index < ranked.size(); index++) {
            if (ranked.get(index).playerId().equals(playerId)) {
                return index + 1;
            }
        }
        return 0;
    }

    public void resetPlayer(UUID playerId) {
        dataStore.deleteStats(playerId);
    }

    public boolean trySpendPoints(UUID playerId, String playerName, int cost, String reason, String referenceId) {
        return dataStore.trySpendPoints(playerId, playerName, cost, reason, referenceId);
    }

    public PlayerStats addPoints(OfflinePlayer player, int amount, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        String name = player.getName() == null ? player.getUniqueId().toString() : player.getName();
        String normalizedReason = reason == null || reason.isBlank() ? "admin-grant" : reason.trim();
        return dataStore.addPoints(player.getUniqueId(), name, amount, normalizedReason, Long.toString(System.currentTimeMillis()));
    }

    public void saveStats(PlayerStats stats) {
        dataStore.saveStats(stats);
    }

    public DataStore dataStore() {
        return dataStore;
    }

    public void close() {
        dataStore.close();
    }

    private void migrateLegacyStats() {
        if (dataStore.metadata(LEGACY_STATS_MIGRATED).orElse("").equals("true")) {
            return;
        }
        if (!legacyStatsFile.exists()) {
            dataStore.setMetadata(LEGACY_STATS_MIGRATED, "true");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(legacyStatsFile);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                UUID uuid = parseUuid(key);
                ConfigurationSection section = players.getConfigurationSection(key);
                if (uuid == null || section == null || dataStore.findStats(uuid).isPresent()) {
                    continue;
                }
                PlayerStats playerStats = new PlayerStats(uuid, section.getString("name", key));
                playerStats.load(
                    section.getInt("games"),
                    section.getInt("wins"),
                    section.getInt("losses"),
                    section.getInt("draws"),
                    section.getInt("points"),
                    section.getInt("current-streak"),
                    section.getInt("best-streak"),
                    section.getLong("last-played")
                );
                dataStore.saveStats(playerStats);
            }
        }
        ConfigurationSection records = yaml.getConfigurationSection("records");
        if (records != null) {
            for (String matchId : records.getKeys(false)) {
                dataStore.rememberLegacyMatch(matchId);
            }
        }
        dataStore.setMetadata(LEGACY_STATS_MIGRATED, "true");
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }
}
