package net.leafmc.gomoku;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class AppearanceCatalog {
    public static final String DEFAULT_THEME_ID = "classic_logs";
    public static final String DEFAULT_BLACK_SKIN_ID = "black_concrete";
    public static final String DEFAULT_WHITE_SKIN_ID = "white_concrete";

    private final Map<String, BoardTheme> boardThemes;
    private final Map<String, PieceSkin> pieceSkins;

    private AppearanceCatalog(Map<String, BoardTheme> boardThemes, Map<String, PieceSkin> pieceSkins) {
        this.boardThemes = boardThemes;
        this.pieceSkins = pieceSkins;
    }

    public static AppearanceCatalog load(FileConfiguration config, Logger logger) {
        Map<String, BoardTheme> themes = new LinkedHashMap<>();
        Map<String, PieceSkin> skins = new LinkedHashMap<>();

        addDefaultThemes(themes);
        addDefaultSkins(skins);
        loadThemes(config.getConfigurationSection("appearances.board-themes"), themes, logger);
        loadSkins(config.getConfigurationSection("appearances.piece-skins"), skins, logger);

        return new AppearanceCatalog(themes, skins);
    }

    private static void addDefaultThemes(Map<String, BoardTheme> themes) {
        themes.put(DEFAULT_THEME_ID, new BoardTheme(
            DEFAULT_THEME_ID,
            "经典原木棋盘",
            Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_SPRUCE_LOG,
            true,
            0
        ));
        themes.put("polished_stone", new BoardTheme(
            "polished_stone",
            "磨制石棋盘",
            Material.SMOOTH_STONE,
            Material.POLISHED_ANDESITE,
            false,
            30
        ));
    }

    private static void addDefaultSkins(Map<String, PieceSkin> skins) {
        skins.put(DEFAULT_BLACK_SKIN_ID, blockSkin(DEFAULT_BLACK_SKIN_ID, "黑色混凝土", Material.BLACK_CONCRETE, true, 0));
        skins.put(DEFAULT_WHITE_SKIN_ID, blockSkin(DEFAULT_WHITE_SKIN_ID, "白色混凝土", Material.WHITE_CONCRETE, true, 0));
        skins.put("red_concrete", blockSkin("red_concrete", "红色混凝土", Material.RED_CONCRETE, true, 0));
        skins.put("blue_concrete", blockSkin("blue_concrete", "蓝色混凝土", Material.BLUE_CONCRETE, true, 0));
        skins.put("skeleton_head", headSkin("skeleton_head", "骷髅头", Material.SKELETON_SKULL, "", true, 0));
        skins.put("steve_head", headSkin("steve_head", "Steve 头颅", Material.PLAYER_HEAD, "Steve", false, 15));
        skins.put("tnt_block", blockSkin("tnt_block", "TNT 爆破棋子", Material.TNT, false, 18));
        skins.put("amethyst", blockSkin("amethyst", "紫水晶块", Material.AMETHYST_BLOCK, false, 20));
        skins.put("emerald", blockSkin("emerald", "绿宝石块", Material.EMERALD_BLOCK, false, 25));
        skins.put("slime_entity", entitySkin("slime_entity", "静态史莱姆", Material.SLIME_BLOCK, "SLIME", false, 30));
        skins.put("bee_entity", entitySkin("bee_entity", "静态蜜蜂", Material.HONEYCOMB_BLOCK, "BEE", false, 30));
    }

    private static void loadThemes(ConfigurationSection section, Map<String, BoardTheme> themes, Logger logger) {
        if (section == null) {
            return;
        }
        for (String rawId : section.getKeys(false)) {
            ConfigurationSection theme = section.getConfigurationSection(rawId);
            if (theme == null) {
                continue;
            }
            String id = normalizeId(rawId);
            Material primary = material(theme.getString("primary-material"));
            Material secondary = material(theme.getString("secondary-material"));
            if (id.isBlank() || primary == null || secondary == null) {
                warn(logger, "Skipping invalid board theme: " + rawId);
                continue;
            }
            themes.put(id, new BoardTheme(
                id,
                theme.getString("display-name", id),
                primary,
                secondary,
                theme.getBoolean("default", false),
                Math.max(0, theme.getInt("cost", 0))
            ));
        }
    }

    private static void loadSkins(ConfigurationSection section, Map<String, PieceSkin> skins, Logger logger) {
        if (section == null) {
            return;
        }
        for (String rawId : section.getKeys(false)) {
            ConfigurationSection skin = section.getConfigurationSection(rawId);
            if (skin == null) {
                continue;
            }
            String id = normalizeId(rawId);
            Material material = material(skin.getString("material"));
            if (id.isBlank() || material == null) {
                warn(logger, "Skipping invalid piece skin: " + rawId);
                continue;
            }
            PieceDisplayType displayType = PieceDisplayType.parse(skin.getString("display", "block"));
            skins.put(id, new PieceSkin(
                id,
                skin.getString("display-name", id),
                material,
                displayType,
                skin.getString("head-owner", ""),
                skin.getString("entity-type", ""),
                PieceAnimationType.parse(skin.getString("animation", ""), displayType, material),
                skin.getBoolean("default", false),
                Math.max(0, skin.getInt("cost", 0))
            ));
        }
    }

    private static Material material(String value) {
        return Material.matchMaterial(value == null ? "" : value);
    }

    private static void warn(Logger logger, String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    public Optional<BoardTheme> boardTheme(String id) {
        return Optional.ofNullable(boardThemes.get(normalizeId(id)));
    }

    public Optional<PieceSkin> pieceSkin(String id) {
        return Optional.ofNullable(pieceSkins.get(normalizeId(id)));
    }

    public BoardTheme defaultBoardTheme() {
        return boardTheme(DEFAULT_THEME_ID).orElseGet(() -> boardThemes.values().iterator().next());
    }

    public PieceSkin defaultBlackSkin() {
        return pieceSkin(DEFAULT_BLACK_SKIN_ID).orElseGet(() -> pieceSkins.values().iterator().next());
    }

    public PieceSkin defaultWhiteSkin() {
        return pieceSkin(DEFAULT_WHITE_SKIN_ID).orElseGet(() -> pieceSkins.values().stream().skip(1).findFirst().orElse(defaultBlackSkin()));
    }

    public List<BoardTheme> boardThemes() {
        return sorted(boardThemes);
    }

    public List<PieceSkin> pieceSkins() {
        return sorted(pieceSkins);
    }

    private static <T> List<T> sorted(Map<String, T> values) {
        List<Map.Entry<String, T>> entries = new ArrayList<>(values.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        return entries.stream().map(Map.Entry::getValue).toList();
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static PieceSkin blockSkin(String id, String name, Material material, boolean defaultUnlocked, int cost) {
        return new PieceSkin(
            id,
            name,
            material,
            PieceDisplayType.BLOCK,
            "",
            "",
            PieceAnimationType.defaultFor(PieceDisplayType.BLOCK, material),
            defaultUnlocked,
            cost
        );
    }

    private static PieceSkin headSkin(String id, String name, Material material, String owner, boolean defaultUnlocked, int cost) {
        return new PieceSkin(
            id,
            name,
            material,
            PieceDisplayType.PLAYER_HEAD,
            owner,
            "",
            PieceAnimationType.defaultFor(PieceDisplayType.PLAYER_HEAD, material),
            defaultUnlocked,
            cost
        );
    }

    private static PieceSkin entitySkin(String id, String name, Material material, String entityType, boolean defaultUnlocked, int cost) {
        return new PieceSkin(
            id,
            name,
            material,
            PieceDisplayType.ENTITY,
            "",
            entityType,
            PieceAnimationType.defaultFor(PieceDisplayType.ENTITY, material),
            defaultUnlocked,
            cost
        );
    }
}
