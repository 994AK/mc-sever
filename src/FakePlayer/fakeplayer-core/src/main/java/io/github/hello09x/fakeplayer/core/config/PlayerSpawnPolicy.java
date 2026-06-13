package io.github.hello09x.fakeplayer.core.config;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@ToString
public class PlayerSpawnPolicy {

    public static final PlayerSpawnPolicy DEFAULT = new PlayerSpawnPolicy(true, null, "");

    private final boolean spawnEnabled;

    @Nullable
    private final Integer playerLimit;

    @NotNull
    private final String reason;

    public PlayerSpawnPolicy(boolean spawnEnabled, @Nullable Integer playerLimit, @Nullable String reason) {
        this.spawnEnabled = spawnEnabled;
        this.playerLimit = playerLimit;
        this.reason = reason == null ? "" : reason;
    }

    public int resolveLimit(int fallback) {
        return this.playerLimit == null ? fallback : this.playerLimit;
    }

}
