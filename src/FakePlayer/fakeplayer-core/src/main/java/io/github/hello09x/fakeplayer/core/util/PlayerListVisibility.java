package io.github.hello09x.fakeplayer.core.util;

import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerListPingEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.function.Predicate;

public final class PlayerListVisibility {

    private static final Method PLAYER_UNLIST_PLAYER = findPlayerUnlistPlayer();

    private PlayerListVisibility() {
    }

    public static void unlist(@NotNull Player viewer, @NotNull Player target) {
        if (viewer.getUniqueId().equals(target.getUniqueId()) || PLAYER_UNLIST_PLAYER == null) {
            return;
        }

        try {
            PLAYER_UNLIST_PLAYER.invoke(viewer, target);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    public static void removeFromServerListPing(
            @NotNull ServerListPingEvent event,
            @NotNull Predicate<Player> hiddenPlayer
    ) {
        var hiddenCount = removePlayerSamples(event, hiddenPlayer);
        if (hiddenCount > 0) {
            setPingPlayerCount(event, Math.max(0, event.getNumPlayers() - hiddenCount));
        }
    }

    private static int removePlayerSamples(@NotNull ServerListPingEvent event, @NotNull Predicate<Player> hiddenPlayer) {
        var hiddenCount = 0;
        try {
            Iterator<Player> iterator = event.iterator();
            while (iterator.hasNext()) {
                if (hiddenPlayer.test(iterator.next())) {
                    hiddenCount++;
                    iterator.remove();
                }
            }
        } catch (UnsupportedOperationException ignored) {
        }
        return hiddenCount;
    }

    private static void setPingPlayerCount(@NotNull ServerListPingEvent event, int count) {
        try {
            event.getClass().getMethod("setNumPlayers", int.class).invoke(event, count);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    private static Method findPlayerUnlistPlayer() {
        try {
            return Player.class.getMethod("unlistPlayer", Player.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
