package net.leafmc.soulbind;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public final class PendingReturnStore {
    private final File file;
    private final Map<UUID, List<ItemStack>> pending = new HashMap<>();

    public PendingReturnStore(File file) {
        this.file = file;
    }

    public void load() throws IOException {
        pending.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            List<?> rawItems = players.getList(key, List.of());
            List<ItemStack> items = new ArrayList<>();
            for (Object rawItem : rawItems) {
                if (rawItem instanceof ItemStack item && !SoulbindService.isEmpty(item)) {
                    items.add(item);
                }
            }
            if (!items.isEmpty()) {
                pending.put(playerId, items);
            }
        }
    }

    public void save() throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IOException("Could not create " + file.getParentFile());
        }
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> entry : pending.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                config.set("players." + entry.getKey(), entry.getValue());
            }
        }
        config.save(file);
    }

    public void add(UUID playerId, Collection<ItemStack> items) {
        List<ItemStack> cleaned = items.stream()
            .filter(item -> !SoulbindService.isEmpty(item))
            .map(ItemStack::clone)
            .toList();
        if (cleaned.isEmpty()) {
            return;
        }
        pending.computeIfAbsent(playerId, ignored -> new ArrayList<>()).addAll(cleaned);
    }

    public List<ItemStack> remove(UUID playerId) {
        List<ItemStack> items = pending.remove(playerId);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().map(ItemStack::clone).toList();
    }

    public void replace(UUID playerId, Collection<ItemStack> items) {
        List<ItemStack> cleaned = items.stream()
            .filter(item -> !SoulbindService.isEmpty(item))
            .map(ItemStack::clone)
            .toList();
        if (cleaned.isEmpty()) {
            pending.remove(playerId);
        } else {
            pending.put(playerId, new ArrayList<>(cleaned));
        }
    }
}
