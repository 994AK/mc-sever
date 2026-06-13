package net.leafmc.recycle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public final class YamlRecycleStore implements RecycleStore {
    private final File file;

    public YamlRecycleStore(File file) {
        this.file = file;
    }

    @Override
    public List<RecycleEntry> load() throws IOException {
        if (!file.isFile()) {
            return List.of();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("entries");
        if (root == null) {
            return List.of();
        }
        List<String> keys = new ArrayList<>(root.getKeys(false));
        keys.sort(Comparator.comparingInt(YamlRecycleStore::numericKey));

        List<RecycleEntry> entries = new ArrayList<>();
        for (String key : keys) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            ItemStack item = section.getItemStack("item");
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                continue;
            }
            entries.add(new RecycleEntry(
                section.getString("id", key),
                item,
                parseUuid(section.getString("contributor.id")),
                section.getString("contributor.name", "Unknown"),
                section.getLong("createdAt", System.currentTimeMillis())
            ));
        }
        return entries;
    }

    @Override
    public void save(List<RecycleEntry> entries) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        for (int index = 0; index < entries.size(); index++) {
            RecycleEntry entry = entries.get(index);
            String path = "entries." + index;
            yaml.set(path + ".id", entry.id());
            yaml.set(path + ".item", entry.item());
            yaml.set(path + ".contributor.id", entry.contributorId() == null ? null : entry.contributorId().toString());
            yaml.set(path + ".contributor.name", entry.contributorName());
            yaml.set(path + ".createdAt", entry.createdAtMillis());
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        yaml.save(file);
    }

    private static int numericKey(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
