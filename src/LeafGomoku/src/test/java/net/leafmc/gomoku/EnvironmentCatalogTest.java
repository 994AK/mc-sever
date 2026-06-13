package net.leafmc.gomoku;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

public final class EnvironmentCatalogTest {
    public static void main(String[] args) {
        loadsDefaultTemplates();
        loadsConfiguredTemplateAndProfile();
        skipsInvalidTemplate();
        arenaConfigDefaultsAndSavesTemplate();
    }

    private static void loadsDefaultTemplates() {
        EnvironmentCatalog catalog = EnvironmentCatalog.load(new YamlConfiguration(), null);

        TestSupport.check(catalog.template("classic").isPresent(), "classic template exists");
        TestSupport.check(catalog.template("garden").isPresent(), "garden template exists");
        TestSupport.check(catalog.template("dojo").isPresent(), "dojo template exists");
        TestSupport.check(catalog.defaultTemplate().id().equals(RoomEnvironmentTemplate.DEFAULT_ID), "default template id");
        TestSupport.check(catalog.template("garden").orElseThrow().feedbackProfile().particlesEnabled(), "default profile attached");
    }

    private static void loadsConfiguredTemplateAndProfile() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("environment-feedback.profiles.no_particles.actionbar", true);
        yaml.set("environment-feedback.profiles.no_particles.particles", false);
        yaml.set("environment-feedback.profiles.no_particles.sounds", true);
        yaml.set("environment-feedback.profiles.no_particles.win-line", false);
        yaml.set("environment-feedback.profiles.no_particles.countdown", true);
        yaml.set("environment-templates.garden.display-name", "Custom Garden");
        yaml.set("environment-templates.garden.floor.default", "GRASS_BLOCK");
        yaml.set("environment-templates.garden.floor.accents", List.of(Map.of(
            "row-min", -1,
            "row-max", 1,
            "column-min", 7,
            "column-max", 7,
            "material", "GOLD_BLOCK"
        )));
        yaml.set("environment-templates.garden.frame.material", "BIRCH_FENCE");
        yaml.set("environment-templates.garden.frame.height", 1);
        yaml.set("environment-templates.garden.frame.entrance-width", 5);
        yaml.set("environment-templates.garden.decor", List.of(Map.of(
            "type", "column",
            "row", -3,
            "column", -2,
            "y-offset", 1,
            "height", 2,
            "material", "OAK_LOG"
        )));
        yaml.set("environment-templates.garden.feedback-profile", "no_particles");

        EnvironmentCatalog catalog = EnvironmentCatalog.load(yaml, null);
        RoomEnvironmentTemplate template = catalog.template("garden").orElseThrow();

        TestSupport.check(template.displayName().equals("Custom Garden"), "configured template overrides built-in");
        TestSupport.check(template.floorMaterial() == Material.GRASS_BLOCK, "configured floor");
        TestSupport.check(template.floorMaterialAt(0, 7) == Material.GOLD_BLOCK, "configured accent");
        TestSupport.check(template.frameMaterial() == Material.BIRCH_FENCE, "configured frame");
        TestSupport.check(template.decor().size() == 1, "configured decor");
        TestSupport.check(!template.feedbackProfile().particlesEnabled(), "configured feedback profile");
    }

    private static void skipsInvalidTemplate() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("environment-templates.bad.floor.default", "NO_SUCH_BLOCK");
        yaml.set("environment-templates.bad.frame.material", "GLASS");

        EnvironmentCatalog catalog = EnvironmentCatalog.load(yaml, null);

        TestSupport.check(catalog.template("bad").isEmpty(), "bad template skipped");
        TestSupport.check(catalog.template("classic").isPresent(), "defaults remain");
    }

    private static void arenaConfigDefaultsAndSavesTemplate() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("world", "world");
        yaml.set("board.origin.x", 1);
        yaml.set("board.origin.y", 2);
        yaml.set("board.origin.z", 3);
        yaml.set("emitters.black.x", 0);
        yaml.set("emitters.black.y", 3);
        yaml.set("emitters.black.z", 0);
        yaml.set("emitters.white.x", 10);
        yaml.set("emitters.white.y", 3);
        yaml.set("emitters.white.z", 0);

        ArenaConfig config = ArenaConfig.load("test", yaml);
        TestSupport.check(config.environmentTemplateId().equals(RoomEnvironmentTemplate.DEFAULT_ID), "missing template defaults");
        TestSupport.check(config.undoRequestTimeoutTicks() == 300L, "missing undo timeout defaults");

        YamlConfiguration saved = new YamlConfiguration();
        config.withEnvironmentTemplateId("garden").save(saved);
        TestSupport.check(saved.getString("environment-template").equals("garden"), "template saved");
        TestSupport.check(saved.getLong("gameplay.undo-request-timeout-ticks") == 300L, "undo timeout saved");

        yaml.set("gameplay.undo-request-timeout-ticks", -20L);
        ArenaConfig invalidTimeout = ArenaConfig.load("test", yaml);
        TestSupport.check(invalidTimeout.undoRequestTimeoutTicks() == 300L, "invalid undo timeout falls back");
    }
}
