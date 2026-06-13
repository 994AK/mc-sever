package net.leafmc.gomoku;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

public final class AppearanceCatalogTest {
    public static void main(String[] args) {
        loadsDefaultAppearances();
        loadsConfiguredSkinDisplayAndAnimation();
        skipsInvalidConfiguredAppearances();
    }

    private static void loadsDefaultAppearances() {
        AppearanceCatalog catalog = AppearanceCatalog.load(new YamlConfiguration(), null);

        BoardTheme theme = catalog.defaultBoardTheme();
        TestSupport.check(theme.id().equals(AppearanceCatalog.DEFAULT_THEME_ID), "default theme id");
        TestSupport.check(theme.materialAt(0, 0) == Material.STRIPPED_BIRCH_LOG, "primary log material");
        TestSupport.check(theme.materialAt(0, 1) == Material.STRIPPED_SPRUCE_LOG, "secondary log material");
        TestSupport.check(catalog.defaultBlackSkin().material() == Material.BLACK_CONCRETE, "default black skin");
        TestSupport.check(catalog.defaultWhiteSkin().material() == Material.WHITE_CONCRETE, "default white skin");
        TestSupport.check(catalog.pieceSkin("skeleton_head").orElseThrow().boardBlockMaterial() == Material.SKELETON_SKULL, "default skull block");
        TestSupport.check(catalog.pieceSkin("steve_head").orElseThrow().headOwner().equals("Steve"), "default player head owner");
        TestSupport.check(catalog.pieceSkin("tnt_block").orElseThrow().animationType() == PieceAnimationType.EXPLOSION, "default tnt animation");
        TestSupport.check(catalog.pieceSkin("slime_entity").orElseThrow().displayType() == PieceDisplayType.ENTITY, "default entity skin");
        TestSupport.check(catalog.pieceSkin("allay_entity").orElseThrow().entityType().equals("ALLAY"), "default friendly entity skin");
        TestSupport.check(catalog.pieceSkin("creeper_entity").orElseThrow().entityType().equals("CREEPER"), "default hostile entity skin");
        TestSupport.check(catalog.pieceSkin("silverfish_entity").orElseThrow().material() == Material.INFESTED_DEEPSLATE, "default small hostile entity icon");
    }

    private static void loadsConfiguredSkinDisplayAndAnimation() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("appearances.piece-skins.boom.display-name", "Boom");
        yaml.set("appearances.piece-skins.boom.display", "block");
        yaml.set("appearances.piece-skins.boom.material", "TNT");
        yaml.set("appearances.piece-skins.boom.animation", "explosion");
        yaml.set("appearances.piece-skins.boom.default", true);
        yaml.set("appearances.piece-skins.mob.display-name", "Mob");
        yaml.set("appearances.piece-skins.mob.display", "entity");
        yaml.set("appearances.piece-skins.mob.material", "SLIME_BLOCK");
        yaml.set("appearances.piece-skins.mob.entity-type", "SLIME");
        yaml.set("appearances.piece-skins.mob.animation", "walk");

        AppearanceCatalog catalog = AppearanceCatalog.load(yaml, null);

        PieceSkin boom = catalog.pieceSkin("boom").orElseThrow();
        TestSupport.check(boom.displayType() == PieceDisplayType.BLOCK, "configured block display");
        TestSupport.check(boom.animationType() == PieceAnimationType.EXPLOSION, "configured explosion animation");
        PieceSkin mob = catalog.pieceSkin("mob").orElseThrow();
        TestSupport.check(mob.displayType() == PieceDisplayType.ENTITY, "configured entity display");
        TestSupport.check(mob.entityType().equals("SLIME"), "configured entity type");
        TestSupport.check(mob.animationType() == PieceAnimationType.WALK, "configured walk animation");
    }

    private static void skipsInvalidConfiguredAppearances() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("appearances.board-themes.bad.primary-material", "NO_SUCH_BLOCK");
        yaml.set("appearances.board-themes.bad.secondary-material", "STONE");
        yaml.set("appearances.piece-skins.bad.material", "NO_SUCH_BLOCK");

        AppearanceCatalog catalog = AppearanceCatalog.load(yaml, null);

        TestSupport.check(catalog.boardTheme("bad").isEmpty(), "bad theme skipped");
        TestSupport.check(catalog.pieceSkin("bad").isEmpty(), "bad skin skipped");
        TestSupport.check(catalog.boardTheme(AppearanceCatalog.DEFAULT_THEME_ID).isPresent(), "defaults still present");
    }
}
