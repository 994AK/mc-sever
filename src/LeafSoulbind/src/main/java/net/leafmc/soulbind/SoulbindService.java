package net.leafmc.soulbind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class SoulbindService {
    private static final int MAX_NESTED_CONTAINER_DEPTH = 3;
    private final LeafSoulbindPlugin plugin;
    private final NamespacedKey lockedKey;
    private final NamespacedKey boundKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey ownerNameKey;

    public SoulbindService(LeafSoulbindPlugin plugin) {
        this.plugin = plugin;
        this.lockedKey = new NamespacedKey(plugin, "locked");
        this.boundKey = new NamespacedKey(plugin, "bound");
        this.ownerKey = new NamespacedKey(plugin, "owner");
        this.ownerNameKey = new NamespacedKey(plugin, "owner_name");
    }

    public MarkResult lock(ItemStack item) {
        if (isEmpty(item)) {
            return MarkResult.EMPTY;
        }
        if (isBound(item)) {
            refreshDisplay(item);
            return MarkResult.ALREADY_BOUND;
        }
        if (isLocked(item) && !isBound(item)) {
            refreshDisplay(item);
            return MarkResult.ALREADY_LOCKED;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return MarkResult.EMPTY;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(lockedKey, PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        refreshDisplay(item);
        return MarkResult.CHANGED;
    }

    public MarkResult bind(ItemStack item, UUID owner, String ownerName) {
        if (isEmpty(item)) {
            return MarkResult.EMPTY;
        }
        if (isBound(item)) {
            refreshDisplay(item);
            return MarkResult.ALREADY_BOUND;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return MarkResult.EMPTY;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(lockedKey, PersistentDataType.INTEGER, 1);
        data.set(boundKey, PersistentDataType.INTEGER, 1);
        data.set(ownerKey, PersistentDataType.STRING, owner.toString());
        data.set(ownerNameKey, PersistentDataType.STRING, ownerName);
        item.setItemMeta(meta);
        refreshDisplay(item);
        return MarkResult.CHANGED;
    }

    public UnlockResult unlock(ItemStack item, UUID actor, boolean bypass) {
        if (isEmpty(item) || !isProtected(item)) {
            return UnlockResult.NOT_PROTECTED;
        }
        Optional<UUID> owner = owner(item);
        if (owner.isPresent() && !owner.get().equals(actor) && !bypass) {
            return UnlockResult.DENIED;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return UnlockResult.NOT_PROTECTED;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.remove(lockedKey);
        data.remove(boundKey);
        data.remove(ownerKey);
        data.remove(ownerNameKey);
        item.setItemMeta(meta);
        refreshDisplay(item);
        return UnlockResult.CHANGED;
    }

    public boolean isProtected(ItemStack item) {
        return isLocked(item) || isBound(item);
    }

    public boolean isLocked(ItemStack item) {
        return hasFlag(item, lockedKey);
    }

    public boolean isBound(ItemStack item) {
        return hasFlag(item, boundKey);
    }

    public Optional<UUID> owner(ItemStack item) {
        if (isEmpty(item)) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        String value = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public String ownerName(ItemStack item) {
        if (isEmpty(item)) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return "";
        }
        String value = meta.getPersistentDataContainer().get(ownerNameKey, PersistentDataType.STRING);
        return value == null || value.isBlank() ? "未知玩家" : value;
    }

    public boolean containsProtectedItemDeep(ItemStack item) {
        return containsProtectedItemDeep(item, 0);
    }

    public boolean containsProtectedItemDeep(Collection<ItemStack> items) {
        for (ItemStack item : items) {
            if (containsProtectedItemDeep(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsProtectedItemDeep(ItemStack[] items) {
        for (ItemStack item : items) {
            if (containsProtectedItemDeep(item)) {
                return true;
            }
        }
        return false;
    }

    public ItemStack refreshDisplay(ItemStack item) {
        if (isEmpty(item)) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(SoulbindService::isSoulbindLore);
        if (plugin.getConfig().getBoolean("display.add-lore", true)) {
            if (isBound(item)) {
                lore.add(plugin.text("display.bound-lore", java.util.Map.of("owner", ownerName(item))));
            } else if (isLocked(item)) {
                lore.add(plugin.text("display.locked-lore", java.util.Map.of()));
            }
        }
        meta.setLore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    public static int countItems(Collection<ItemStack> items) {
        int count = 0;
        for (ItemStack item : items) {
            if (!isEmpty(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public static int countItems(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (!isEmpty(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean containsProtectedItemDeep(ItemStack item, int depth) {
        if (isEmpty(item)) {
            return false;
        }
        if (isProtected(item)) {
            return true;
        }
        if (!plugin.rule("block-nested-container-drop") || depth >= MAX_NESTED_CONTAINER_DEPTH) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return false;
        }
        if (!(blockStateMeta.getBlockState() instanceof Container container)) {
            return false;
        }
        Inventory inventory = container.getSnapshotInventory();
        for (ItemStack content : inventory.getContents()) {
            if (containsProtectedItemDeep(content, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFlag(ItemStack item, NamespacedKey key) {
        if (isEmpty(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Integer value = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return value != null && value == 1;
    }

    private static boolean isSoulbindLore(String line) {
        return line != null && (line.contains("灵魂锁定") || line.contains("灵魂绑定"));
    }

    public enum MarkResult {
        CHANGED,
        ALREADY_LOCKED,
        ALREADY_BOUND,
        EMPTY
    }

    public enum UnlockResult {
        CHANGED,
        DENIED,
        NOT_PROTECTED
    }
}
