package net.leafmc.recycle;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public final class RecycleEntry {
    private final String id;
    private final UUID contributorId;
    private final String contributorName;
    private final long createdAtMillis;
    private ItemStack item;

    public RecycleEntry(ItemStack item, UUID contributorId, String contributorName, long createdAtMillis) {
        this(java.util.UUID.randomUUID().toString(), item, contributorId, contributorName, createdAtMillis);
    }

    public RecycleEntry(String id, ItemStack item, UUID contributorId, String contributorName, long createdAtMillis) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            throw new IllegalArgumentException("Recycle entry item must be a non-empty stack.");
        }
        this.id = id == null || id.isBlank() ? java.util.UUID.randomUUID().toString() : id;
        this.item = item.clone();
        this.contributorId = contributorId;
        this.contributorName = contributorName == null || contributorName.isBlank() ? "Unknown" : contributorName;
        this.createdAtMillis = createdAtMillis;
    }

    public String id() {
        return id;
    }

    public UUID contributorId() {
        return contributorId;
    }

    public String contributorName() {
        return contributorName;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public ItemStack item() {
        return item.clone();
    }

    public int amount() {
        return item.getAmount();
    }

    boolean isSimilar(ItemStack candidate) {
        return item.isSimilar(candidate);
    }

    int addAmount(int amount) {
        int room = Math.max(0, item.getMaxStackSize() - item.getAmount());
        int accepted = Math.min(room, Math.max(0, amount));
        if (accepted > 0) {
            item.setAmount(item.getAmount() + accepted);
        }
        return accepted;
    }
}
