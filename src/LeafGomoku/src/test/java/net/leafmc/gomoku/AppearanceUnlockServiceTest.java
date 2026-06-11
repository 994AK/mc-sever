package net.leafmc.gomoku;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public final class AppearanceUnlockServiceTest {
    public static void main(String[] args) throws Exception {
        defaultAppearancesAreUsableWithoutSpending();
        purchaseUnlocksAndPersistsPreference();
        insufficientPointsDoNotUnlock();
    }

    private static void defaultAppearancesAreUsableWithoutSpending() throws Exception {
        Fixture fixture = fixture();
        UUID player = UUID.randomUUID();

        TestSupport.check(fixture.unlocks.canUseSkin(player, fixture.catalog.defaultBlackSkin()), "default skin usable");
        TestSupport.check(fixture.unlocks.canUseTheme(player, fixture.catalog.defaultBoardTheme()), "default theme usable");
    }

    private static void purchaseUnlocksAndPersistsPreference() throws Exception {
        Fixture fixture = fixture();
        UUID player = UUID.randomUUID();
        PlayerStats stats = new PlayerStats(player, "Alice");
        stats.load(1, 1, 0, 0, 25, 1, 1, 100L);
        fixture.stats.saveStats(stats);

        String result = fixture.unlocks.purchaseSkin(player, "Alice", "amethyst");

        TestSupport.check(result.contains("已兑换"), "purchase message");
        PieceSkin skin = fixture.catalog.pieceSkin("amethyst").orElseThrow();
        TestSupport.check(fixture.unlocks.canUseSkin(player, skin), "skin unlocked");
        TestSupport.check(fixture.stats.find(player).orElseThrow().points() == 5, "points deducted");
        TestSupport.check(fixture.stats.dataStore().appearanceState(player).pieceSkinId().equals("amethyst"), "preference persisted");
    }

    private static void insufficientPointsDoNotUnlock() throws Exception {
        Fixture fixture = fixture();
        UUID player = UUID.randomUUID();
        PlayerStats stats = new PlayerStats(player, "Bob");
        stats.load(1, 0, 1, 0, 3, 0, 0, 100L);
        fixture.stats.saveStats(stats);

        String result = fixture.unlocks.purchaseSkin(player, "Bob", "emerald");

        TestSupport.check(result.contains("积分不足"), "insufficient message");
        PieceSkin skin = fixture.catalog.pieceSkin("emerald").orElseThrow();
        TestSupport.check(!fixture.unlocks.canUseSkin(player, skin), "skin remains locked");
        TestSupport.check(fixture.stats.find(player).orElseThrow().points() == 3, "points unchanged");
    }

    private static Fixture fixture() throws Exception {
        File dir = Files.createTempDirectory("leafgomoku-appearance").toFile();
        StatsService stats = new StatsService(new File(dir, "data.db"), new File(dir, "stats.yml"));
        stats.load();
        AppearanceCatalog catalog = AppearanceCatalog.load(new YamlConfiguration(), null);
        return new Fixture(catalog, stats, new AppearanceUnlockService(catalog, stats));
    }

    private record Fixture(AppearanceCatalog catalog, StatsService stats, AppearanceUnlockService unlocks) {
    }
}
