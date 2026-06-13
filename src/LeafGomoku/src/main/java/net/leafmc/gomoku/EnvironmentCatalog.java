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

public final class EnvironmentCatalog {
    private final Map<String, RoomEnvironmentTemplate> templates;
    private final Map<String, EnvironmentFeedbackProfile> feedbackProfiles;

    private EnvironmentCatalog(
        Map<String, RoomEnvironmentTemplate> templates,
        Map<String, EnvironmentFeedbackProfile> feedbackProfiles
    ) {
        this.templates = templates;
        this.feedbackProfiles = feedbackProfiles;
    }

    public static EnvironmentCatalog load(FileConfiguration config, Logger logger) {
        Map<String, EnvironmentFeedbackProfile> profiles = new LinkedHashMap<>();
        profiles.put(EnvironmentFeedbackProfile.DEFAULT_ID, EnvironmentFeedbackProfile.soft());
        profiles.put("quiet", EnvironmentFeedbackProfile.quiet());
        loadProfiles(config.getConfigurationSection("environment-feedback.profiles"), profiles);

        Map<String, RoomEnvironmentTemplate> templates = new LinkedHashMap<>();
        addDefaultTemplates(templates, profiles);
        loadTemplates(config.getConfigurationSection("environment-templates"), templates, profiles, logger);
        return new EnvironmentCatalog(templates, profiles);
    }

    private static void addDefaultTemplates(
        Map<String, RoomEnvironmentTemplate> templates,
        Map<String, EnvironmentFeedbackProfile> profiles
    ) {
        EnvironmentFeedbackProfile soft = profiles.get(EnvironmentFeedbackProfile.DEFAULT_ID);
        templates.put(RoomEnvironmentTemplate.DEFAULT_ID, new RoomEnvironmentTemplate(
            RoomEnvironmentTemplate.DEFAULT_ID,
            "仅棋盘",
            Material.AIR,
            List.of(),
            Material.AIR,
            0,
            0,
            List.of(),
            soft,
            RoomEnvironmentTemplate.DEFAULT_BLOCK_BUDGET
        ));
        templates.put("garden", new RoomEnvironmentTemplate(
            "garden",
            "竹林棋亭",
            Material.MOSS_BLOCK,
            List.of(new RoomEnvironmentTemplate.FloorAccent(-7, 18, 7, 7, Material.POLISHED_ANDESITE)),
            Material.OAK_FENCE,
            2,
            5,
            List.of(
                column(-6, -3, 1, 3, Material.BAMBOO_BLOCK),
                column(-6, 17, 1, 3, Material.BAMBOO_BLOCK),
                column(18, -3, 1, 3, Material.BAMBOO_BLOCK),
                column(18, 17, 1, 3, Material.BAMBOO_BLOCK),
                block(-5, 1, 1, Material.FLOWERING_AZALEA),
                block(-5, 13, 1, Material.AZALEA),
                block(-6, 5, 1, Material.LANTERN),
                block(-6, 9, 1, Material.LANTERN)
            ),
            soft,
            RoomEnvironmentTemplate.DEFAULT_BLOCK_BUDGET
        ));
        templates.put("dojo", new RoomEnvironmentTemplate(
            "dojo",
            "道场棋室",
            Material.SPRUCE_PLANKS,
            List.of(
                new RoomEnvironmentTemplate.FloorAccent(-8, 18, -4, -4, Material.RED_TERRACOTTA),
                new RoomEnvironmentTemplate.FloorAccent(-8, 18, 18, 18, Material.RED_TERRACOTTA),
                new RoomEnvironmentTemplate.FloorAccent(-2, 16, 7, 7, Material.DARK_OAK_PLANKS)
            ),
            Material.DARK_OAK_FENCE,
            2,
            3,
            List.of(
                column(-6, -3, 1, 3, Material.DARK_OAK_LOG),
                column(-6, 17, 1, 3, Material.DARK_OAK_LOG),
                column(18, -3, 1, 3, Material.DARK_OAK_LOG),
                column(18, 17, 1, 3, Material.DARK_OAK_LOG),
                block(-5, 7, 1, Material.RED_CARPET),
                block(17, 7, 1, Material.RED_CARPET)
            ),
            soft,
            RoomEnvironmentTemplate.DEFAULT_BLOCK_BUDGET
        ));
    }

    private static RoomEnvironmentTemplate.DecorElement block(int row, int column, int yOffset, Material material) {
        return new RoomEnvironmentTemplate.DecorElement(RoomEnvironmentTemplate.DecorType.BLOCK, row, column, yOffset, 1, material);
    }

    private static RoomEnvironmentTemplate.DecorElement column(int row, int column, int yOffset, int height, Material material) {
        return new RoomEnvironmentTemplate.DecorElement(RoomEnvironmentTemplate.DecorType.COLUMN, row, column, yOffset, height, material);
    }

    private static void loadProfiles(ConfigurationSection section, Map<String, EnvironmentFeedbackProfile> profiles) {
        if (section == null) {
            return;
        }
        for (String rawId : section.getKeys(false)) {
            ConfigurationSection profile = section.getConfigurationSection(rawId);
            if (profile == null) {
                continue;
            }
            String id = normalizeId(rawId);
            profiles.put(id, new EnvironmentFeedbackProfile(
                id,
                profile.getBoolean("actionbar", true),
                profile.getBoolean("particles", true),
                profile.getBoolean("sounds", true),
                profile.getBoolean("win-line", true),
                profile.getBoolean("countdown", true)
            ));
        }
    }

    private static void loadTemplates(
        ConfigurationSection section,
        Map<String, RoomEnvironmentTemplate> templates,
        Map<String, EnvironmentFeedbackProfile> profiles,
        Logger logger
    ) {
        if (section == null) {
            return;
        }
        for (String rawId : section.getKeys(false)) {
            ConfigurationSection template = section.getConfigurationSection(rawId);
            if (template == null) {
                continue;
            }
            RoomEnvironmentTemplate parsed = parseTemplate(rawId, template, profiles, logger);
            if (parsed != null) {
                templates.put(parsed.id(), parsed);
            }
        }
    }

    private static RoomEnvironmentTemplate parseTemplate(
        String rawId,
        ConfigurationSection section,
        Map<String, EnvironmentFeedbackProfile> profiles,
        Logger logger
    ) {
        String id = normalizeId(rawId);
        Material floor = material(section.getString("floor.default"));
        Material frame = material(section.getString("frame.material"));
        if (id.isBlank() || floor == null || frame == null) {
            warn(logger, "Skipping invalid environment template: " + rawId);
            return null;
        }
        String profileId = normalizeId(section.getString("feedback-profile", EnvironmentFeedbackProfile.DEFAULT_ID));
        EnvironmentFeedbackProfile profile = profiles.getOrDefault(profileId, profiles.get(EnvironmentFeedbackProfile.DEFAULT_ID));
        return new RoomEnvironmentTemplate(
            id,
            section.getString("display-name", id),
            floor,
            parseAccents(section.getMapList("floor.accents"), logger, rawId),
            frame,
            Math.max(0, section.getInt("frame.height", 3)),
            Math.max(0, section.getInt("frame.entrance-width", 3)),
            parseDecor(section.getMapList("decor"), logger, rawId),
            profile,
            Math.max(1, section.getInt("block-budget", RoomEnvironmentTemplate.DEFAULT_BLOCK_BUDGET))
        );
    }

    private static List<RoomEnvironmentTemplate.FloorAccent> parseAccents(
        List<Map<?, ?>> values,
        Logger logger,
        String templateId
    ) {
        List<RoomEnvironmentTemplate.FloorAccent> accents = new ArrayList<>();
        for (Map<?, ?> value : values) {
            Material material = material(string(value, "material"));
            if (material == null) {
                warn(logger, "Skipping invalid floor accent in environment template: " + templateId);
                continue;
            }
            accents.add(new RoomEnvironmentTemplate.FloorAccent(
                integer(value, "row-min", 0),
                integer(value, "row-max", 0),
                integer(value, "column-min", 0),
                integer(value, "column-max", 0),
                material
            ));
        }
        return accents;
    }

    private static List<RoomEnvironmentTemplate.DecorElement> parseDecor(
        List<Map<?, ?>> values,
        Logger logger,
        String templateId
    ) {
        List<RoomEnvironmentTemplate.DecorElement> decor = new ArrayList<>();
        for (Map<?, ?> value : values) {
            Material material = material(string(value, "material"));
            RoomEnvironmentTemplate.DecorType type = decorType(string(value, "type"));
            if (material == null || type == null) {
                warn(logger, "Skipping invalid decor element in environment template: " + templateId);
                continue;
            }
            decor.add(new RoomEnvironmentTemplate.DecorElement(
                type,
                integer(value, "row", 0),
                integer(value, "column", 0),
                integer(value, "y-offset", 1),
                integer(value, "height", 1),
                material
            ));
        }
        return decor;
    }

    private static RoomEnvironmentTemplate.DecorType decorType(String value) {
        return switch (normalizeId(value)) {
            case "block" -> RoomEnvironmentTemplate.DecorType.BLOCK;
            case "column" -> RoomEnvironmentTemplate.DecorType.COLUMN;
            default -> null;
        };
    }

    private static String string(Map<?, ?> value, String key) {
        Object raw = value.get(key);
        return raw == null ? "" : String.valueOf(raw);
    }

    private static int integer(Map<?, ?> value, String key, int fallback) {
        Object raw = value.get(key);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(raw == null ? "" : String.valueOf(raw));
        } catch (NumberFormatException error) {
            return fallback;
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

    public Optional<RoomEnvironmentTemplate> template(String id) {
        return Optional.ofNullable(templates.get(normalizeId(id)));
    }

    public RoomEnvironmentTemplate defaultTemplate() {
        return template(RoomEnvironmentTemplate.DEFAULT_ID).orElseGet(() -> templates.values().iterator().next());
    }

    public RoomEnvironmentTemplate resolve(String id) {
        return template(id).orElse(defaultTemplate());
    }

    public List<RoomEnvironmentTemplate> templates() {
        List<RoomEnvironmentTemplate> values = new ArrayList<>(templates.values());
        values.sort(Comparator.comparing(RoomEnvironmentTemplate::id));
        return values;
    }

    public List<String> templateIds() {
        return templates().stream().map(RoomEnvironmentTemplate::id).toList();
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
