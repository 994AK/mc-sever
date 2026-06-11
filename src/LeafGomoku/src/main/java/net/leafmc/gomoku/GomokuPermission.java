package net.leafmc.gomoku;

public enum GomokuPermission {
    PLAY("leafgomoku.play"),
    SPECTATE("leafgomoku.spectate"),
    GUI("leafgomoku.gui"),
    STATS("leafgomoku.stats"),
    LEADERBOARD("leafgomoku.leaderboard"),
    APPEARANCE("leafgomoku.appearance"),
    ADMIN("leafgomoku.admin"),
    ADMIN_SETUP("leafgomoku.admin.setup"),
    ADMIN_ROOM("leafgomoku.admin.room"),
    ADMIN_LIFECYCLE("leafgomoku.admin.lifecycle"),
    ADMIN_STATS("leafgomoku.admin.stats"),
    ADMIN_RELOAD("leafgomoku.admin.reload"),
    ADMIN_FORCE("leafgomoku.admin.force"),
    ADMIN_BYPASS("leafgomoku.admin.bypass-protection");

    private final String node;

    GomokuPermission(String node) {
        this.node = node;
    }

    public String node() {
        return node;
    }
}
