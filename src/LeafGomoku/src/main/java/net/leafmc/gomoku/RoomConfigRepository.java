package net.leafmc.gomoku;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class RoomConfigRepository {
    private final JavaPlugin plugin;
    private final File roomsFile;

    public RoomConfigRepository(JavaPlugin plugin) {
        this(plugin, new File(plugin.getDataFolder(), "rooms.yml"));
    }

    RoomConfigRepository(JavaPlugin plugin, File roomsFile) {
        this.plugin = plugin;
        this.roomsFile = roomsFile;
    }

    public List<ArenaConfig> loadRooms(FileConfiguration legacyConfig) {
        if (!roomsFile.exists()) {
            migrateLegacyMainRoom(legacyConfig);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(roomsFile);
        ConfigurationSection rooms = yaml.getConfigurationSection("rooms");
        if (rooms == null) {
            return List.of();
        }
        List<ArenaConfig> configs = new ArrayList<>();
        for (String id : rooms.getKeys(false)) {
            configs.add(ArenaConfig.load(id, rooms.getConfigurationSection(id)));
        }
        configs.sort(Comparator.comparing(ArenaConfig::id));
        return configs;
    }

    public void saveRoom(ArenaConfig config) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(roomsFile);
        ConfigurationSection rooms = yaml.getConfigurationSection("rooms");
        if (rooms == null) {
            rooms = yaml.createSection("rooms");
        }
        yaml.set("rooms." + config.id(), null);
        ConfigurationSection section = yaml.createSection("rooms." + config.id());
        config.save(section);
        save(yaml);
    }

    public void deleteRoom(String roomId) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(roomsFile);
        yaml.set("rooms." + ArenaConfig.normalizeRoomId(roomId), null);
        save(yaml);
    }

    private void migrateLegacyMainRoom(FileConfiguration legacyConfig) {
        ConfigurationSection legacyArena = legacyConfig.getConfigurationSection("arena");
        if (legacyArena == null) {
            plugin.getLogger().warning("LeafGomoku rooms.yml missing and no legacy arena config exists.");
            return;
        }
        ArenaConfig main = ArenaConfig.load(ArenaConfig.MAIN_ROOM_ID, legacyArena);
        if (!main.enabled()) {
            plugin.getLogger().warning("LeafGomoku legacy arena could not migrate: " + main.error());
            return;
        }
        saveRoom(main);
        plugin.getLogger().info("Migrated legacy LeafGomoku arena config to rooms.yml room 'main'.");
    }

    private void save(YamlConfiguration yaml) {
        try {
            File parent = roomsFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create " + parent);
            }
            yaml.save(roomsFile);
        } catch (IOException error) {
            throw new IllegalStateException("Could not save " + roomsFile, error);
        }
    }
}
