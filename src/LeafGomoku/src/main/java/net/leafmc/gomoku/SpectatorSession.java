package net.leafmc.gomoku;

import java.util.UUID;
import org.bukkit.Location;

public record SpectatorSession(UUID playerId, String playerName, long joinedAtMillis, Location previousLocation) {
}
