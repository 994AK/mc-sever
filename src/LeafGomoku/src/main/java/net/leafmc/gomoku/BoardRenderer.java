package net.leafmc.gomoku;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class BoardRenderer {
    private static final int WIN_LINE_STEP_TICKS = 7;
    private static final int ENTITY_EXIT_TICKS = 18;

    private final Plugin plugin;
    private final Set<Entity> staticPieces = new HashSet<>();
    private final Map<GridCell, Entity> staticPieceByCell = new HashMap<>();
    private final Set<BukkitTask> animationTasks = new HashSet<>();
    private ArenaConfig arena;

    public BoardRenderer(Plugin plugin, ArenaConfig arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void updateArena(ArenaConfig arena) {
        this.arena = arena;
    }

    public boolean canRender() {
        return arena.enabled() && arena.world() != null;
    }

    public void renderEmpty() {
        renderEmpty(null);
    }

    public void renderEmpty(BoardTheme theme) {
        if (!canRender()) {
            return;
        }
        clearLegacyPreview();
        clearPieceLayer();
        renderRoomFloor();
        renderRoomFrame();
        for (int row = 0; row < GomokuBoard.SIZE; row++) {
            for (int column = 0; column < GomokuBoard.SIZE; column++) {
                set(arena.geometry().boardPoint(row, column), boardMaterial(theme, row, column));
            }
        }
    }

    public void renderBoard(GomokuBoard board) {
        renderBoard(board, null);
    }

    public void renderBoard(GomokuBoard board, MatchAppearance appearance) {
        if (!canRender()) {
            return;
        }
        clearLegacyPreview();
        clearPieceLayer();
        renderRoomFloor();
        renderRoomFrame();
        for (int row = 0; row < GomokuBoard.SIZE; row++) {
            for (int column = 0; column < GomokuBoard.SIZE; column++) {
                Stone stone = board.get(row, column);
                set(arena.geometry().boardPoint(row, column), boardMaterial(appearance == null ? null : appearance.boardTheme(), row, column));
                if (stone != Stone.EMPTY) {
                    if (appearance == null) {
                        renderBlockPiece(row, column, arena.materialFor(stone));
                    } else {
                        renderPiece(row, column, appearance.skinFor(stone));
                    }
                }
            }
        }
    }

    public void renderMove(int row, int column, Stone stone) {
        renderMove(row, column, arena.materialFor(stone));
    }

    public void renderMove(int row, int column, org.bukkit.Material material) {
        if (!canRender()) {
            return;
        }
        renderBlockPiece(row, column, material);
    }

    public void renderMove(int row, int column, PieceSkin skin) {
        if (!canRender()) {
            return;
        }
        renderPiece(row, column, skin);
    }

    public void clearRoom() {
        if (!canRender()) {
            return;
        }
        clearLegacyPreview();
        clearPieceLayer();
        for (BlockPoint point : arena.geometry().roomFramePoints()) {
            set(point, org.bukkit.Material.AIR);
        }
        for (BlockPoint point : arena.geometry().roomFloorPoints()) {
            set(point, org.bukkit.Material.AIR);
        }
        set(arena.blackEmitter(), org.bukkit.Material.AIR);
        set(arena.whiteEmitter(), org.bukkit.Material.AIR);
    }

    private void renderRoomFrame() {
        for (BlockPoint point : arena.geometry().roomFramePoints()) {
            set(point, arena.frameMaterial());
        }
    }

    public void cleanup() {
        cancelAnimations();
        clearStaticPieceEntities();
    }

    private void renderRoomFloor() {
        for (BlockPoint point : arena.geometry().roomFloorPoints()) {
            set(point, arena.floorMaterial());
        }
    }

    private void clearLegacyPreview() {
        if (!arena.legacyPreviewConfigured()) {
            return;
        }
        for (int row = 0; row < GomokuBoard.SIZE; row++) {
            for (int column = 0; column < GomokuBoard.SIZE; column++) {
                set(arena.geometry().previewPoint(row, column), org.bukkit.Material.AIR);
            }
        }
    }

    private void clearPieceLayer() {
        cancelAnimations();
        clearStaticPieceEntities();
        for (int row = 0; row < GomokuBoard.SIZE; row++) {
            for (int column = 0; column < GomokuBoard.SIZE; column++) {
                set(arena.geometry().piecePoint(row, column), org.bukkit.Material.AIR);
            }
        }
    }

    private void renderPiece(int row, int column, PieceSkin skin) {
        if (skin == null) {
            return;
        }
        if (skin.displayType() == PieceDisplayType.ENTITY) {
            spawnStaticEntity(row, column, skin);
            return;
        }
        renderBlockPiece(row, column, skin.boardBlockMaterial());
        if (skin.displayType() == PieceDisplayType.PLAYER_HEAD && skin.boardBlockMaterial() == org.bukkit.Material.PLAYER_HEAD && !skin.headOwner().isBlank()) {
            applyHeadOwner(arena.geometry().piecePoint(row, column), skin.headOwner());
        }
    }

    private void renderBlockPiece(int row, int column, org.bukkit.Material material) {
        removeStaticPieceAt(new GridCell(row, column));
        set(arena.geometry().piecePoint(row, column), material);
    }

    private void spawnStaticEntity(int row, int column, PieceSkin skin) {
        Location location = entityLocation(row, column);
        World world = location == null ? null : location.getWorld();
        EntityType type = entityType(skin.entityType());
        if (world == null || type == null || type.getEntityClass() == null) {
            renderBlockPiece(row, column, skin.animationMaterial());
            return;
        }
        GridCell cell = new GridCell(row, column);
        removeStaticPieceAt(cell);
        Entity entity = world.spawnEntity(location, type);
        configureStaticEntity(entity);
        staticPieces.add(entity);
        staticPieceByCell.put(cell, entity);
    }

    public void animateWinningLine(List<GridCell> cells, PieceSkin skin) {
        if (!canRender() || cells == null || cells.isEmpty()) {
            return;
        }
        animationTasks.removeIf(BukkitTask::isCancelled);
        Vector exitVector = lineExitVector(cells);
        for (int index = 0; index < cells.size(); index++) {
            GridCell cell = cells.get(index);
            long delay = (long) index * WIN_LINE_STEP_TICKS;
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (skin != null && skin.displayType() == PieceDisplayType.ENTITY) {
                    animateEntityExit(cell, exitVector);
                } else {
                    animateBlockDisappear(cell, skin);
                }
            }, delay);
            animationTasks.add(task);
        }
    }

    private void animateBlockDisappear(GridCell cell, PieceSkin skin) {
        Location location = arena.centeredLocation(arena.geometry().piecePoint(cell.row(), cell.column()));
        World world = location == null ? null : location.getWorld();
        set(arena.geometry().piecePoint(cell.row(), cell.column()), org.bukkit.Material.AIR);
        if (world == null) {
            return;
        }
        PieceAnimationType animationType = skin == null ? PieceAnimationType.ARC : skin.animationType();
        if (animationType == PieceAnimationType.EXPLOSION) {
            world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            world.spawnParticle(Particle.SMOKE, location, 18, 0.35D, 0.2D, 0.35D, 0.02D);
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.55F, 1.35F);
            return;
        }
        world.spawnParticle(Particle.CLOUD, location, 10, 0.25D, 0.18D, 0.25D, 0.01D);
        world.spawnParticle(Particle.END_ROD, location, 8, 0.18D, 0.18D, 0.18D, 0.0D);
        world.playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.65F, 1.45F);
    }

    private void animateEntityExit(GridCell cell, Vector exitVector) {
        Entity entity = staticPieceByCell.remove(cell);
        if (entity == null || !entity.isValid()) {
            animateBlockDisappear(cell, null);
            return;
        }
        Vector velocity = exitVector.clone().multiply(1.0D / ENTITY_EXIT_TICKS);
        World world = entity.getWorld();
        Location end = entity.getLocation().clone().add(exitVector);
        face(entity, end);
        BukkitTask task = new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!entity.isValid()) {
                    staticPieces.remove(entity);
                    cancel();
                    return;
                }
                tick++;
                entity.setVelocity(velocity);
                Location next = entity.getLocation();
                face(entity, end);
                if (tick % 5 == 0) {
                    world.spawnParticle(Particle.CLOUD, next, 1, 0.08D, 0.02D, 0.08D, 0.0D);
                    world.playSound(next, Sound.ENTITY_CHICKEN_STEP, 0.25F, 0.9F);
                }
                if (tick >= ENTITY_EXIT_TICKS) {
                    world.spawnParticle(Particle.POOF, next, 8, 0.18D, 0.18D, 0.18D, 0.01D);
                    entity.remove();
                    staticPieces.remove(entity);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        animationTasks.add(task);
    }

    private Vector lineExitVector(List<GridCell> cells) {
        if (cells.size() < 2) {
            return new Vector(0.0D, 0.0D, 2.25D);
        }
        GridCell first = cells.get(0);
        GridCell last = cells.get(cells.size() - 1);
        int rowDirection = Integer.compare(last.row(), first.row());
        int columnDirection = Integer.compare(last.column(), first.column());
        BlockPoint rowStep = arena.geometry().boardRowStep().multiply(rowDirection);
        BlockPoint columnStep = arena.geometry().boardColumnStep().multiply(columnDirection);
        BlockPoint step = rowStep.add(columnStep);
        Vector vector = new Vector(step.x(), 0.0D, step.z());
        if (vector.lengthSquared() < 0.0001D) {
            return new Vector(0.0D, 0.0D, 2.25D);
        }
        return vector.normalize().multiply(2.25D);
    }

    private void configureStaticEntity(Entity entity) {
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setGravity(false);
        entity.setPersistent(false);
        entity.addScoreboardTag(GomokuEntityTags.STATIC_PIECE);
        entity.addScoreboardTag(roomTag());
        if (entity instanceof Slime slime) {
            slime.setSize(1);
        }
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setCollidable(false);
            living.setRemoveWhenFarAway(false);
        }
    }

    private Location entityLocation(int row, int column) {
        Location location = arena.location(arena.geometry().piecePoint(row, column));
        return location == null ? null : location.add(0.5D, 0.0D, 0.5D);
    }

    private EntityType entityType(String value) {
        if (value == null || value.isBlank()) {
            return EntityType.SLIME;
        }
        try {
            return EntityType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private void applyHeadOwner(BlockPoint point, String owner) {
        Location location = arena.location(point);
        World world = location == null ? null : location.getWorld();
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt(location);
        if (block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            skull.update(false, false);
        }
    }

    private void clearStaticPieceEntities() {
        for (Entity entity : new HashSet<>(staticPieces)) {
            if (entity.isValid()) {
                entity.remove();
            }
        }
        staticPieces.clear();
        staticPieceByCell.clear();
        World world = arena.world();
        if (world == null) {
            return;
        }
        String roomTag = roomTag();
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains(roomTag)) {
                entity.remove();
            }
        }
    }

    private String roomTag() {
        return GomokuEntityTags.roomStaticPiece(arena.id());
    }

    private void removeStaticPieceAt(GridCell cell) {
        Entity entity = staticPieceByCell.remove(cell);
        if (entity != null) {
            if (entity.isValid()) {
                entity.remove();
            }
            staticPieces.remove(entity);
        }
    }

    private void cancelAnimations() {
        for (BukkitTask task : new HashSet<>(animationTasks)) {
            task.cancel();
        }
        animationTasks.clear();
    }

    private void face(Entity entity, Location target) {
        Location current = entity.getLocation();
        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        if (Math.abs(dx) < 0.0001D && Math.abs(dz) < 0.0001D) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        entity.setRotation(yaw, 0.0F);
    }

    private void set(BlockPoint point, org.bukkit.Material material) {
        Location location = arena.location(point);
        World world = location == null ? null : location.getWorld();
        if (world != null) {
            world.getBlockAt(location).setType(material, false);
        }
    }

    private org.bukkit.Material boardMaterial(BoardTheme theme, int row, int column) {
        return theme == null ? arena.emptyBoardMaterial() : theme.materialAt(row, column);
    }
}
