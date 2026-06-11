package net.leafmc.gomoku;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataStore extends AutoCloseable {
    void init();

    Optional<String> metadata(String key);

    void setMetadata(String key, String value);

    Optional<PlayerStats> findStats(UUID playerId);

    Collection<PlayerStats> allStats();

    void saveStats(PlayerStats stats);

    void deleteStats(UUID playerId);

    PlayerStats addPoints(UUID playerId, String playerName, int amount, String reason, String referenceId);

    boolean hasMatch(String matchId);

    void rememberLegacyMatch(String matchId);

    boolean recordMatch(MatchRecord record, List<MatchMove> moves, int winPoints, int drawPoints, int lossPoints);

    boolean isAppearanceUnlocked(UUID playerId, String type, String appearanceId);

    void unlockAppearance(UUID playerId, String type, String appearanceId);

    PlayerAppearanceState appearanceState(UUID playerId);

    void setPreferredBoardTheme(UUID playerId, String themeId);

    void setPreferredPieceSkin(UUID playerId, String skinId);

    boolean trySpendPoints(UUID playerId, String playerName, int cost, String reason, String referenceId);

    @Override
    void close();
}
