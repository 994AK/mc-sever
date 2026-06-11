package net.leafmc.gomoku;

import java.util.Optional;
import java.util.UUID;

public final class AppearanceUnlockService {
    public static final String TYPE_THEME = "board_theme";
    public static final String TYPE_SKIN = "piece_skin";

    private final AppearanceCatalog catalog;
    private final StatsService statsService;
    private final DataStore dataStore;

    public AppearanceUnlockService(AppearanceCatalog catalog, StatsService statsService) {
        this.catalog = catalog;
        this.statsService = statsService;
        this.dataStore = statsService.dataStore();
    }

    public boolean canUseTheme(UUID playerId, BoardTheme theme) {
        return theme.unlockedByDefault() || dataStore.isAppearanceUnlocked(playerId, TYPE_THEME, theme.id());
    }

    public boolean canUseSkin(UUID playerId, PieceSkin skin) {
        return skin.unlockedByDefault() || dataStore.isAppearanceUnlocked(playerId, TYPE_SKIN, skin.id());
    }

    public PlayerAppearanceState state(UUID playerId) {
        return dataStore.appearanceState(playerId);
    }

    public BoardTheme preferredTheme(UUID playerId) {
        PlayerAppearanceState state = state(playerId);
        Optional<BoardTheme> preferred = catalog.boardTheme(state.boardThemeId());
        if (preferred.isPresent() && canUseTheme(playerId, preferred.get())) {
            return preferred.get();
        }
        return catalog.defaultBoardTheme();
    }

    public PieceSkin preferredSkin(UUID playerId, Stone side) {
        PlayerAppearanceState state = state(playerId);
        Optional<PieceSkin> preferred = catalog.pieceSkin(state.pieceSkinId());
        if (preferred.isPresent() && canUseSkin(playerId, preferred.get())) {
            return preferred.get();
        }
        return side == Stone.WHITE ? catalog.defaultWhiteSkin() : catalog.defaultBlackSkin();
    }

    public PieceSkin firstAvailableSkinExcept(UUID playerId, PieceSkin blocked, Stone side) {
        PieceSkin fallback = preferredSkin(playerId, side);
        if (!sameMaterial(fallback, blocked)) {
            return fallback;
        }
        for (PieceSkin skin : catalog.pieceSkins()) {
            if (canUseSkin(playerId, skin) && !sameMaterial(skin, blocked)) {
                return skin;
            }
        }
        return fallback;
    }

    public String selectTheme(UUID playerId, String themeId) {
        Optional<BoardTheme> theme = catalog.boardTheme(themeId);
        if (theme.isEmpty()) {
            return "§c找不到棋盘主题: " + themeId;
        }
        if (!canUseTheme(playerId, theme.get())) {
            return "§e该棋盘主题需要先兑换: " + theme.get().displayName() + "，需要积分 " + theme.get().cost();
        }
        dataStore.setPreferredBoardTheme(playerId, theme.get().id());
        return "";
    }

    public String selectSkin(UUID playerId, String skinId) {
        Optional<PieceSkin> skin = catalog.pieceSkin(skinId);
        if (skin.isEmpty()) {
            return "§c找不到棋子皮肤: " + skinId;
        }
        if (!canUseSkin(playerId, skin.get())) {
            return "§e该棋子皮肤需要先兑换: " + skin.get().displayName() + "，需要积分 " + skin.get().cost();
        }
        dataStore.setPreferredPieceSkin(playerId, skin.get().id());
        return "";
    }

    public String purchaseTheme(UUID playerId, String playerName, String themeId) {
        Optional<BoardTheme> theme = catalog.boardTheme(themeId);
        if (theme.isEmpty()) {
            return "§c找不到棋盘主题: " + themeId;
        }
        BoardTheme value = theme.get();
        if (canUseTheme(playerId, value)) {
            dataStore.setPreferredBoardTheme(playerId, value.id());
            return "§a已选择棋盘主题: " + value.displayName();
        }
        if (!statsService.trySpendPoints(playerId, playerName, value.cost(), "appearance:" + TYPE_THEME, value.id())) {
            return "§e积分不足，兑换 " + value.displayName() + " 需要 " + value.cost() + " 分。";
        }
        dataStore.unlockAppearance(playerId, TYPE_THEME, value.id());
        dataStore.setPreferredBoardTheme(playerId, value.id());
        return "§a已兑换并选择棋盘主题: " + value.displayName();
    }

    public String purchaseSkin(UUID playerId, String playerName, String skinId) {
        Optional<PieceSkin> skin = catalog.pieceSkin(skinId);
        if (skin.isEmpty()) {
            return "§c找不到棋子皮肤: " + skinId;
        }
        PieceSkin value = skin.get();
        if (canUseSkin(playerId, value)) {
            dataStore.setPreferredPieceSkin(playerId, value.id());
            return "§a已选择棋子皮肤: " + value.displayName();
        }
        if (!statsService.trySpendPoints(playerId, playerName, value.cost(), "appearance:" + TYPE_SKIN, value.id())) {
            return "§e积分不足，兑换 " + value.displayName() + " 需要 " + value.cost() + " 分。";
        }
        dataStore.unlockAppearance(playerId, TYPE_SKIN, value.id());
        dataStore.setPreferredPieceSkin(playerId, value.id());
        return "§a已兑换并选择棋子皮肤: " + value.displayName();
    }

    public boolean sameMaterial(PieceSkin left, PieceSkin right) {
        return left != null && right != null && left.visualKey().equals(right.visualKey());
    }
}
