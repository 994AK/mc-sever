package net.leafmc.gomoku;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class GomokuPlaceholderExpansion extends PlaceholderExpansion {
    private final LeafGomokuPlugin plugin;
    private final VariableService variables;

    public GomokuPlaceholderExpansion(LeafGomokuPlugin plugin, VariableService variables) {
        this.plugin = plugin;
        this.variables = variables;
    }

    @Override
    public String getIdentifier() {
        return "leafgomoku";
    }

    @Override
    public String getAuthor() {
        return "LeafMC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        return variables.resolve(params, player);
    }
}
