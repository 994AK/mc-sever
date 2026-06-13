package net.leafmc.recycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public final class RecycleService {
    private final List<RecycleEntry> entries;
    private int maxStoredStacks;

    public RecycleService(Collection<RecycleEntry> entries, int maxStoredStacks) {
        this.entries = new ArrayList<>(entries);
        this.maxStoredStacks = Math.max(1, maxStoredStacks);
        removeInvalidEntries();
    }

    public synchronized List<RecycleEntry> entries() {
        return List.copyOf(entries);
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized int totalItems() {
        int total = 0;
        for (RecycleEntry entry : entries) {
            total += entry.amount();
        }
        return total;
    }

    public synchronized void setMaxStoredStacks(int maxStoredStacks) {
        this.maxStoredStacks = Math.max(1, maxStoredStacks);
        removeInvalidEntries();
    }

    public synchronized Optional<RecycleEntry> entry(String id) {
        return entries.stream()
            .filter(entry -> entry.id().equals(id))
            .findFirst();
    }

    public synchronized AddResult addItems(Collection<ItemStack> items, UUID contributorId, String contributorName) {
        int acceptedItems = 0;
        int rejectedItems = 0;
        List<ItemStack> rejectedStacks = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (ItemStack original : items) {
            if (original == null || original.getType().isAir() || original.getAmount() <= 0) {
                continue;
            }
            ItemStack remaining = original.clone();
            int amount = remaining.getAmount();
            while (amount > 0) {
                int merged = mergeIntoExisting(remaining, amount);
                amount -= merged;
                acceptedItems += merged;
                if (amount <= 0) {
                    break;
                }
                if (entries.size() >= maxStoredStacks) {
                    ItemStack rejected = remaining.clone();
                    rejected.setAmount(amount);
                    rejectedStacks.add(rejected);
                    rejectedItems += amount;
                    break;
                }
                int newStackAmount = Math.min(amount, remaining.getMaxStackSize());
                ItemStack newStack = remaining.clone();
                newStack.setAmount(newStackAmount);
                entries.add(new RecycleEntry(newStack, contributorId, contributorName, now));
                amount -= newStackAmount;
                acceptedItems += newStackAmount;
            }
        }
        return new AddResult(acceptedItems, rejectedItems, List.copyOf(rejectedStacks));
    }

    public synchronized Optional<ItemStack> claim(String id) {
        int index = indexOf(id);
        if (index < 0) {
            return Optional.empty();
        }
        return Optional.of(entries.remove(index).item());
    }

    public synchronized boolean delete(String id) {
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }
        entries.remove(index);
        return true;
    }

    private int mergeIntoExisting(ItemStack item, int amount) {
        int remaining = amount;
        for (RecycleEntry entry : entries) {
            if (remaining <= 0) {
                break;
            }
            if (!entry.isSimilar(item)) {
                continue;
            }
            remaining -= entry.addAmount(remaining);
        }
        return amount - remaining;
    }

    private int indexOf(String id) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private void removeInvalidEntries() {
        entries.removeIf(entry -> entry.amount() <= 0 || entry.item().getType().isAir());
    }

    public record AddResult(int acceptedItems, int rejectedItems, List<ItemStack> rejectedStacks) {
    }
}
