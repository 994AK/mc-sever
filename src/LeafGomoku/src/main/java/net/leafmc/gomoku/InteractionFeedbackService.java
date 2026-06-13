package net.leafmc.gomoku;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class InteractionFeedbackService {
    private final Plugin plugin;
    private EnvironmentFeedbackProfile profile;
    private final Set<BukkitTask> countdownTasks = new HashSet<>();

    public InteractionFeedbackService(Plugin plugin, EnvironmentFeedbackProfile profile) {
        this.plugin = plugin;
        this.profile = profile == null ? EnvironmentFeedbackProfile.soft() : profile;
    }

    public void updateProfile(EnvironmentFeedbackProfile profile) {
        this.profile = profile == null ? EnvironmentFeedbackProfile.soft() : profile;
    }

    public void accepted(Player player, ArenaConfig arena, GridCell cell) {
        sendActionbar(player, "§a已选择 " + formatCell(cell) + "，棋子落下中");
        Location location = cellLocation(arena, cell);
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (profile.particlesEnabled()) {
            location.getWorld().spawnParticle(Particle.END_ROD, location, 8, 0.25D, 0.15D, 0.25D, 0.0D);
        }
        if (profile.soundsEnabled()) {
            location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.55F, 1.65F);
        }
    }

    public void rejected(Player player, ArenaConfig arena, GridCell cell, String reason) {
        sendActionbar(player, reason);
        Location location = cell == null ? player.getLocation() : cellLocation(arena, cell);
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (profile.particlesEnabled()) {
            location.getWorld().spawnParticle(Particle.SMOKE, location, 8, 0.22D, 0.12D, 0.22D, 0.01D);
        }
        if (profile.soundsEnabled()) {
            location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.45F, 0.65F);
        }
    }

    public void landed(ArenaConfig arena, GridCell cell) {
        Location location = cellLocation(arena, cell);
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (profile.particlesEnabled()) {
            location.getWorld().spawnParticle(Particle.CLOUD, location, 6, 0.18D, 0.08D, 0.18D, 0.0D);
        }
    }

    public void undoCleared(Collection<Player> players, ArenaConfig arena, GridCell cell) {
        for (Player player : players) {
            sendActionbar(player, "§a悔棋已同意，回到 " + formatCell(cell));
        }
        Location location = cellLocation(arena, cell);
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (profile.particlesEnabled()) {
            location.getWorld().spawnParticle(Particle.POOF, location, 10, 0.22D, 0.12D, 0.22D, 0.01D);
        }
        if (profile.soundsEnabled()) {
            location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6F, 1.35F);
        }
    }

    public void turnChanged(Player current, Player waiting, Stone turn) {
        if (current != null) {
            sendActionbar(current, "§a轮到你落子：§f" + turn.displayName());
        }
        if (waiting != null) {
            sendActionbar(waiting, "§7等待对方落子：§f" + turn.displayName());
        }
    }

    public void spectatorsTurn(Collection<Player> spectators, Stone turn) {
        for (Player spectator : spectators) {
            sendActionbar(spectator, "§7当前回合：§f" + turn.displayName());
        }
    }

    public void winningLine(ArenaConfig arena, List<GridCell> cells) {
        if (!profile.winLineEnabled() || !profile.particlesEnabled() || cells == null || cells.isEmpty()) {
            return;
        }
        for (GridCell cell : cells) {
            Location location = cellLocation(arena, cell);
            World world = location == null ? null : location.getWorld();
            if (world != null) {
                world.spawnParticle(Particle.END_ROD, location, 16, 0.25D, 0.25D, 0.25D, 0.0D);
            }
        }
    }

    public void startResetCountdown(Collection<Player> players, int seconds) {
        cancelCountdown();
        if (!profile.countdownEnabled() || seconds <= 0) {
            return;
        }
        for (int second = seconds; second > 0; second--) {
            int remaining = second;
            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player player : players) {
                    if (player != null && player.isOnline()) {
                        sendActionbar(player, "§e棋盘将在 §f" + remaining + " §e秒后重置");
                    }
                }
            }, (long) (seconds - second) * 20L);
            countdownTasks.add(task);
        }
    }

    public void cancelCountdown() {
        for (BukkitTask task : new HashSet<>(countdownTasks)) {
            task.cancel();
        }
        countdownTasks.clear();
    }

    public void cleanup() {
        cancelCountdown();
    }

    private Location cellLocation(ArenaConfig arena, GridCell cell) {
        return arena.centeredLocation(arena.geometry().piecePoint(cell.row(), cell.column()));
    }

    private void sendActionbar(Player player, String message) {
        if (!profile.actionbarEnabled() || player == null || message == null || message.isBlank()) {
            return;
        }
        player.sendActionBar(Component.text(message));
    }

    private String formatCell(GridCell cell) {
        return "(" + (cell.row() + 1) + ", " + (cell.column() + 1) + ")";
    }
}
