package net.leafmc.friends;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlFriendStore implements FriendStore {
    private final File file;

    public YamlFriendStore(File file) {
        this.file = file;
    }

    @Override
    public List<FriendProfile> load() throws IOException {
        if (!file.isFile()) {
            return List.of();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("profiles");
        if (root == null) {
            return List.of();
        }
        List<FriendProfile> profiles = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            UUID id = parseUuid(key);
            if (id == null) {
                continue;
            }
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            FriendProfile profile = new FriendProfile(id, section.getString("name", "Unknown"));
            profile.setLastSeenMillis(section.getLong("lastSeen", 0L));
            for (String friend : section.getStringList("friends")) {
                UUID friendId = parseUuid(friend);
                if (friendId != null) {
                    profile.friends().add(friendId);
                }
            }
            for (String blocked : section.getStringList("blacklist")) {
                UUID blockedId = parseUuid(blocked);
                if (blockedId != null) {
                    profile.blacklist().add(blockedId);
                }
            }
            for (String trusted : section.getStringList("trustedTeleporters")) {
                UUID trustedId = parseUuid(trusted);
                if (trustedId != null) {
                    profile.trustedTeleporters().add(trustedId);
                }
            }
            FriendSettings settings = new FriendSettings();
            settings.setReceiveRequests(section.getBoolean("settings.receiveRequests", true));
            settings.setReceiveTeleports(section.getBoolean("settings.receiveTeleports", true));
            settings.setReceiveMessages(section.getBoolean("settings.receiveMessages", true));
            settings.setOnlineNotifications(section.getBoolean("settings.onlineNotifications", true));
            settings.setShowOnlineStatus(section.getBoolean("settings.showOnlineStatus", true));
            profile.setSettings(settings);
            profiles.add(profile);
        }
        return profiles;
    }

    @Override
    public void save(Collection<FriendProfile> profiles) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        for (FriendProfile profile : profiles) {
            String path = "profiles." + profile.id();
            yaml.set(path + ".name", profile.latestName());
            yaml.set(path + ".lastSeen", profile.lastSeenMillis());
            yaml.set(path + ".friends", profile.friends().stream().map(UUID::toString).sorted().toList());
            yaml.set(path + ".blacklist", profile.blacklist().stream().map(UUID::toString).sorted().toList());
            yaml.set(path + ".trustedTeleporters", profile.trustedTeleporters().stream().map(UUID::toString).sorted().toList());
            yaml.set(path + ".settings.receiveRequests", profile.settings().receiveRequests());
            yaml.set(path + ".settings.receiveTeleports", profile.settings().receiveTeleports());
            yaml.set(path + ".settings.receiveMessages", profile.settings().receiveMessages());
            yaml.set(path + ".settings.onlineNotifications", profile.settings().onlineNotifications());
            yaml.set(path + ".settings.showOnlineStatus", profile.settings().showOnlineStatus());
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        yaml.save(file);
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
