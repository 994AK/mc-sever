package io.github.hello09x.fakeplayer.core.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.config.FakeplayerConfig;
import io.github.hello09x.fakeplayer.core.constant.MetadataKeys;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import io.github.hello09x.fakeplayer.core.util.PlayerListVisibility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PlayerListener implements Listener {

    private final FakeplayerManager manager;
    private final FakeplayerConfig config;

    @Inject
    public PlayerListener(FakeplayerManager manager, FakeplayerConfig config) {
        this.manager = manager;
        this.config = config;
    }

    /**
     * 玩家蹲伏时取消假人骑乘
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSneak(@NotNull PlayerToggleSneakEvent event) {
        var player = event.getPlayer();
        var passengers = player.getPassengers();
        if (passengers.isEmpty()) {
            return;
        }

        for (var passenger : passengers) {
            if (!(passenger instanceof Player target)) {
                continue;
            }
            if (manager.isFake(target)) {
                player.removePassenger(target);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void hideFakeplayersFromPlayerList(@NotNull PlayerJoinEvent event) {
        if (!config.isHideFromPlayerList()) {
            return;
        }

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            var player = event.getPlayer();
            if (!player.isOnline()) {
                return;
            }

            if (manager.isFake(player)) {
                for (var viewer : Bukkit.getOnlinePlayers()) {
                    if (manager.isNotFake(viewer)) {
                        PlayerListVisibility.unlist(viewer, player);
                    }
                }
                return;
            }

            for (var fakeplayer : manager.getAll(Player::isOnline)) {
                PlayerListVisibility.unlist(player, fakeplayer);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void hideFakeplayersFromServerListPingCount(@NotNull ServerListPingEvent event) {
        if (!config.isHideFromServerListPingCount()) {
            return;
        }

        PlayerListVisibility.removeFromServerListPing(event, player -> player.hasMetadata(MetadataKeys.SPAWNED_AT));
    }

}
