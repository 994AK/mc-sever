package net.leafmc.gomoku;

import java.util.Locale;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class ArenaConfig {
    public static final String MAIN_ROOM_ID = "main";
    private static final Pattern ROOM_ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private final String id;
    private final boolean enabled;
    private final String error;
    private final String environmentTemplateId;
    private final String worldName;
    private final BoardGeometry geometry;
    private final BlockPoint blackEmitter;
    private final BlockPoint whiteEmitter;
    private final ArenaSeat blackSeat;
    private final ArenaSeat whiteSeat;
    private final ArenaSeat spectatorSpawn;
    private final ArenaSeat spectatorExit;
    private final int spectatorCapacity;
    private final boolean openByDefault;
    private final long disconnectRecoveryTicks;
    private final boolean forfeitOnDisconnectTimeout;
    private final Material emptyBoardMaterial;
    private final boolean legacyPreviewConfigured;
    private final Material floorMaterial;
    private final Material frameMaterial;
    private final int animationTicks;
    private final double animationArcHeight;
    private final int autoResetTicks;
    private final long undoRequestTimeoutTicks;
    private final int fireworkCount;

    private ArenaConfig(
        String id,
        boolean enabled,
        String error,
        String environmentTemplateId,
        String worldName,
        BoardGeometry geometry,
        BlockPoint blackEmitter,
        BlockPoint whiteEmitter,
        ArenaSeat blackSeat,
        ArenaSeat whiteSeat,
        ArenaSeat spectatorSpawn,
        ArenaSeat spectatorExit,
        int spectatorCapacity,
        boolean openByDefault,
        long disconnectRecoveryTicks,
        boolean forfeitOnDisconnectTimeout,
        Material emptyBoardMaterial,
        boolean legacyPreviewConfigured,
        Material floorMaterial,
        Material frameMaterial,
        int animationTicks,
        double animationArcHeight,
        int autoResetTicks,
        long undoRequestTimeoutTicks,
        int fireworkCount
    ) {
        this.id = id;
        this.enabled = enabled;
        this.error = error;
        this.environmentTemplateId = EnvironmentCatalog.normalizeId(environmentTemplateId == null || environmentTemplateId.isBlank()
            ? RoomEnvironmentTemplate.DEFAULT_ID
            : environmentTemplateId);
        this.worldName = worldName;
        this.geometry = geometry;
        this.blackEmitter = blackEmitter;
        this.whiteEmitter = whiteEmitter;
        this.blackSeat = blackSeat;
        this.whiteSeat = whiteSeat;
        this.spectatorSpawn = spectatorSpawn;
        this.spectatorExit = spectatorExit;
        this.spectatorCapacity = spectatorCapacity;
        this.openByDefault = openByDefault;
        this.disconnectRecoveryTicks = disconnectRecoveryTicks;
        this.forfeitOnDisconnectTimeout = forfeitOnDisconnectTimeout;
        this.emptyBoardMaterial = emptyBoardMaterial;
        this.legacyPreviewConfigured = legacyPreviewConfigured;
        this.floorMaterial = floorMaterial;
        this.frameMaterial = frameMaterial;
        this.animationTicks = animationTicks;
        this.animationArcHeight = animationArcHeight;
        this.autoResetTicks = autoResetTicks;
        this.undoRequestTimeoutTicks = undoRequestTimeoutTicks;
        this.fireworkCount = fireworkCount;
    }

    public static ArenaConfig load(FileConfiguration config) {
        return load(MAIN_ROOM_ID, config.getConfigurationSection("arena"));
    }

    public static ArenaConfig load(String id, ConfigurationSection room) {
        String normalizedId = normalizeRoomId(id);
        if (!isValidRoomId(normalizedId)) {
            return disabled(normalizedId, "Invalid room id: " + id);
        }
        try {
            if (room == null) {
                return disabled(normalizedId, "Missing room section");
            }
            String world = room.getString("world", "world");
            int boardSize = GomokuBoard.requireValidSize(room.getInt("board.size", GomokuBoard.DEFAULT_SIZE));
            BlockPoint boardOrigin = point(room, "board.origin");
            BlockPoint boardRowStep = BoardGeometry.stepFromAxis(room.getString("board.row-axis", "+z"));
            BlockPoint boardColumnStep = BoardGeometry.stepFromAxis(room.getString("board.column-axis", "+x"));
            boolean legacyPreviewConfigured = room.getConfigurationSection("preview.origin") != null;
            BoardGeometry geometry = new BoardGeometry(
                boardOrigin,
                boardRowStep,
                boardColumnStep,
                optionalPoint(room, "preview.origin", boardOrigin),
                BoardGeometry.stepFromAxis(room.getString("preview.row-axis", "-y")),
                BoardGeometry.stepFromAxis(room.getString("preview.column-axis", "+x")),
                boardSize
            );
            BlockPoint blackEmitter = point(room, "emitters.black");
            BlockPoint whiteEmitter = point(room, "emitters.white");
            ArenaSeat blackSeat = seat(room, "seats.black", blackEmitter, 0.0F);
            ArenaSeat whiteSeat = seat(room, "seats.white", whiteEmitter, 180.0F);
            ArenaSeat spectatorSpawn = seat(room, "spectators.spawn", geometry.boardPoint((boardSize - 1) / 2, -5), 0.0F);
            ArenaSeat spectatorExit = optionalSeat(room, "spectators.exit", spectatorSpawn);
            return new ArenaConfig(
                normalizedId,
                true,
                "",
                EnvironmentCatalog.normalizeId(room.getString("environment-template", RoomEnvironmentTemplate.DEFAULT_ID)),
                world,
                geometry,
                blackEmitter,
                whiteEmitter,
                blackSeat,
                whiteSeat,
                spectatorSpawn,
                spectatorExit,
                Math.max(0, room.getInt("spectators.capacity", 24)),
                room.getBoolean("lifecycle.open-by-default", true),
                Math.max(0L, room.getLong("lifecycle.disconnect-recovery-ticks", 600L)),
                room.getBoolean("lifecycle.forfeit-on-disconnect-timeout", true),
                material(room.getString("materials.empty-board", "STRIPPED_BIRCH_LOG")),
                legacyPreviewConfigured,
                material(room.getString("materials.floor", "AIR")),
                material(room.getString("materials.frame", "AIR")),
                Math.max(1, room.getInt("animation.ticks", 20)),
                Math.max(0.0D, room.getDouble("animation.arc-height", 1.35D)),
                Math.max(0, room.getInt("gameplay.auto-reset-ticks", 120)),
                positiveTicks(room, "gameplay.undo-request-timeout-ticks", 300L),
                Math.max(0, room.getInt("celebration.fireworks", 3))
            );
        } catch (RuntimeException error) {
            return disabled(normalizedId, error.getMessage());
        }
    }

    public static ArenaConfig create(
        String id,
        String worldName,
        BoardGeometry geometry,
        BlockPoint blackEmitter,
        BlockPoint whiteEmitter,
        ArenaSeat blackSeat,
        ArenaSeat whiteSeat,
        ArenaSeat spectatorSpawn,
        ArenaSeat spectatorExit,
        ArenaConfig defaults
    ) {
        return create(id, worldName, geometry, blackEmitter, whiteEmitter, blackSeat, whiteSeat, spectatorSpawn, spectatorExit, defaults, null);
    }

    public static ArenaConfig create(
        String id,
        String worldName,
        BoardGeometry geometry,
        BlockPoint blackEmitter,
        BlockPoint whiteEmitter,
        ArenaSeat blackSeat,
        ArenaSeat whiteSeat,
        ArenaSeat spectatorSpawn,
        ArenaSeat spectatorExit,
        ArenaConfig defaults,
        int boardSize
    ) {
        return create(id, worldName, geometry, blackEmitter, whiteEmitter, blackSeat, whiteSeat, spectatorSpawn, spectatorExit, defaults, null, boardSize);
    }

    public static ArenaConfig create(
        String id,
        String worldName,
        BoardGeometry geometry,
        BlockPoint blackEmitter,
        BlockPoint whiteEmitter,
        ArenaSeat blackSeat,
        ArenaSeat whiteSeat,
        ArenaSeat spectatorSpawn,
        ArenaSeat spectatorExit,
        ArenaConfig defaults,
        String environmentTemplateId
    ) {
        int boardSize = defaults == null ? GomokuBoard.DEFAULT_SIZE : defaults.boardSize();
        return create(id, worldName, geometry, blackEmitter, whiteEmitter, blackSeat, whiteSeat, spectatorSpawn, spectatorExit, defaults, environmentTemplateId, boardSize);
    }

    public static ArenaConfig create(
        String id,
        String worldName,
        BoardGeometry geometry,
        BlockPoint blackEmitter,
        BlockPoint whiteEmitter,
        ArenaSeat blackSeat,
        ArenaSeat whiteSeat,
        ArenaSeat spectatorSpawn,
        ArenaSeat spectatorExit,
        ArenaConfig defaults,
        String environmentTemplateId,
        int boardSize
    ) {
        String normalizedId = normalizeRoomId(id);
        if (!isValidRoomId(normalizedId)) {
            return disabled(normalizedId, "Invalid room id: " + id);
        }
        int validatedBoardSize = GomokuBoard.requireValidSize(boardSize);
        BoardGeometry sizedGeometry = geometry.boardSize() == validatedBoardSize ? geometry : new BoardGeometry(
            geometry.boardOrigin(),
            geometry.boardRowStep(),
            geometry.boardColumnStep(),
            geometry.previewOrigin(),
            geometry.previewRowStep(),
            geometry.previewColumnStep(),
            validatedBoardSize
        );
        String templateId = environmentTemplateId == null || environmentTemplateId.isBlank()
            ? defaults == null ? RoomEnvironmentTemplate.DEFAULT_ID : defaults.environmentTemplateId
            : environmentTemplateId;
        return new ArenaConfig(
            normalizedId,
            true,
            "",
            templateId,
            worldName,
            sizedGeometry,
            blackEmitter,
            whiteEmitter,
            blackSeat,
            whiteSeat,
            spectatorSpawn,
            spectatorExit,
            defaults == null ? 24 : defaults.spectatorCapacity,
            defaults == null || defaults.openByDefault,
            defaults == null ? 600L : defaults.disconnectRecoveryTicks,
            defaults == null || defaults.forfeitOnDisconnectTimeout,
            defaults == null ? Material.STRIPPED_BIRCH_LOG : defaults.emptyBoardMaterial,
            false,
            defaults == null ? Material.AIR : defaults.floorMaterial,
            defaults == null ? Material.AIR : defaults.frameMaterial,
            defaults == null ? 20 : defaults.animationTicks,
            defaults == null ? 1.35D : defaults.animationArcHeight,
            defaults == null ? 120 : defaults.autoResetTicks,
            defaults == null ? 300L : defaults.undoRequestTimeoutTicks,
            defaults == null ? 3 : defaults.fireworkCount
        );
    }

    public static boolean isValidRoomId(String id) {
        return id != null && ROOM_ID.matcher(id).matches();
    }

    public static String normalizeRoomId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private static ArenaConfig disabled(String id, String error) {
        return new ArenaConfig(id, false, error, RoomEnvironmentTemplate.DEFAULT_ID, "world", null, null, null, null, null, null, null, 0, false, 0L, false, Material.AIR, false, Material.AIR, Material.AIR, 1, 0.0D, 0, 300L, 0);
    }

    private static BlockPoint point(ConfigurationSection section, String path) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing point: " + path);
        }
        return new BlockPoint(value.getInt("x"), value.getInt("y"), value.getInt("z"));
    }

    private static BlockPoint optionalPoint(ConfigurationSection section, String path, BlockPoint fallback) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            return fallback;
        }
        return new BlockPoint(value.getInt("x"), value.getInt("y"), value.getInt("z"));
    }

    private static ArenaSeat optionalSeat(ConfigurationSection section, String path, ArenaSeat fallback) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            return fallback;
        }
        return new ArenaSeat(
            value.getDouble("x"),
            value.getDouble("y"),
            value.getDouble("z"),
            (float) value.getDouble("yaw", fallback.yaw()),
            (float) value.getDouble("pitch", fallback.pitch())
        );
    }

    private static ArenaSeat seat(ConfigurationSection section, String path, BlockPoint fallback, float fallbackYaw) {
        ConfigurationSection value = section.getConfigurationSection(path);
        if (value == null) {
            return new ArenaSeat(fallback.x() + 0.5D, fallback.y() + 1.0D, fallback.z() + 0.5D, fallbackYaw, 0.0F);
        }
        return new ArenaSeat(
            value.getDouble("x"),
            value.getDouble("y"),
            value.getDouble("z"),
            (float) value.getDouble("yaw", fallbackYaw),
            (float) value.getDouble("pitch", 0.0D)
        );
    }

    private static Material material(String value) {
        Material material = Material.matchMaterial(value == null ? "" : value);
        if (material == null) {
            throw new IllegalArgumentException("Unknown material: " + value);
        }
        return material;
    }

    private static long positiveTicks(ConfigurationSection section, String path, long fallback) {
        long value = section.getLong(path, fallback);
        return value > 0L ? value : fallback;
    }

    public void save(ConfigurationSection section) {
        section.set("world", worldName);
        section.set("environment-template", environmentTemplateId);
        section.set("board.size", boardSize());
        savePoint(section, "board.origin", geometry.boardOrigin());
        section.set("board.row-axis", BoardGeometry.axisName(geometry.boardRowStep()));
        section.set("board.column-axis", BoardGeometry.axisName(geometry.boardColumnStep()));
        savePoint(section, "emitters.black", blackEmitter);
        savePoint(section, "emitters.white", whiteEmitter);
        saveSeat(section, "seats.black", blackSeat);
        saveSeat(section, "seats.white", whiteSeat);
        saveSeat(section, "spectators.spawn", spectatorSpawn);
        saveSeat(section, "spectators.exit", spectatorExit);
        section.set("spectators.capacity", spectatorCapacity);
        section.set("lifecycle.open-by-default", openByDefault);
        section.set("lifecycle.disconnect-recovery-ticks", disconnectRecoveryTicks);
        section.set("lifecycle.forfeit-on-disconnect-timeout", forfeitOnDisconnectTimeout);
        section.set("materials.empty-board", emptyBoardMaterial.name());
        section.set("materials.floor", floorMaterial.name());
        section.set("materials.frame", frameMaterial.name());
        section.set("animation.ticks", animationTicks);
        section.set("animation.arc-height", animationArcHeight);
        section.set("celebration.fireworks", fireworkCount);
        section.set("gameplay.auto-reset-ticks", autoResetTicks);
        section.set("gameplay.undo-request-timeout-ticks", undoRequestTimeoutTicks);
    }

    private void savePoint(ConfigurationSection section, String path, BlockPoint point) {
        section.set(path + ".x", point.x());
        section.set(path + ".y", point.y());
        section.set(path + ".z", point.z());
    }

    private void saveSeat(ConfigurationSection section, String path, ArenaSeat seat) {
        section.set(path + ".x", seat.x());
        section.set(path + ".y", seat.y());
        section.set(path + ".z", seat.z());
        section.set(path + ".yaw", (double) seat.yaw());
        section.set(path + ".pitch", (double) seat.pitch());
    }

    public String id() {
        return id;
    }

    public boolean enabled() {
        return enabled;
    }

    public String error() {
        return error;
    }

    public String environmentTemplateId() {
        return environmentTemplateId;
    }

    public ArenaConfig withEnvironmentTemplateId(String templateId) {
        return new ArenaConfig(
            id,
            enabled,
            error,
            templateId,
            worldName,
            geometry,
            blackEmitter,
            whiteEmitter,
            blackSeat,
            whiteSeat,
            spectatorSpawn,
            spectatorExit,
            spectatorCapacity,
            openByDefault,
            disconnectRecoveryTicks,
            forfeitOnDisconnectTimeout,
            emptyBoardMaterial,
            legacyPreviewConfigured,
            floorMaterial,
            frameMaterial,
            animationTicks,
            animationArcHeight,
            autoResetTicks,
            undoRequestTimeoutTicks,
            fireworkCount
        );
    }

    public World world() {
        return Bukkit.getWorld(worldName);
    }

    public String worldName() {
        return worldName;
    }

    public BoardGeometry geometry() {
        return geometry;
    }

    public int boardSize() {
        return geometry.boardSize();
    }

    public Location location(BlockPoint point) {
        World world = world();
        return world == null ? null : new Location(world, point.x(), point.y(), point.z());
    }

    public Location centeredLocation(BlockPoint point) {
        Location location = location(point);
        return location == null ? null : location.add(0.5D, 0.5D, 0.5D);
    }

    public Location seatLocation(Stone stone) {
        World world = world();
        if (world == null) {
            return null;
        }
        return switch (stone) {
            case BLACK -> blackSeat.location(world);
            case WHITE -> whiteSeat.location(world);
            case EMPTY -> null;
        };
    }

    public Location spectatorSpawnLocation() {
        World world = world();
        return world == null ? null : spectatorSpawn.location(world);
    }

    public Location spectatorExitLocation() {
        World world = world();
        return world == null ? null : spectatorExit.location(world);
    }

    public BlockPoint blackEmitter() {
        return blackEmitter;
    }

    public BlockPoint whiteEmitter() {
        return whiteEmitter;
    }

    public ArenaSeat blackSeat() {
        return blackSeat;
    }

    public ArenaSeat whiteSeat() {
        return whiteSeat;
    }

    public ArenaSeat spectatorSpawn() {
        return spectatorSpawn;
    }

    public int spectatorCapacity() {
        return spectatorCapacity;
    }

    public boolean openByDefault() {
        return openByDefault;
    }

    public long disconnectRecoveryTicks() {
        return disconnectRecoveryTicks;
    }

    public boolean forfeitOnDisconnectTimeout() {
        return forfeitOnDisconnectTimeout;
    }

    public Material emptyBoardMaterial() {
        return emptyBoardMaterial;
    }

    public boolean legacyPreviewConfigured() {
        return legacyPreviewConfigured;
    }

    public Material floorMaterial() {
        return floorMaterial;
    }

    public Material frameMaterial() {
        return frameMaterial;
    }

    public int animationTicks() {
        return animationTicks;
    }

    public double animationArcHeight() {
        return animationArcHeight;
    }

    public int autoResetTicks() {
        return autoResetTicks;
    }

    public long undoRequestTimeoutTicks() {
        return undoRequestTimeoutTicks;
    }

    public int fireworkCount() {
        return fireworkCount;
    }
}
