package net.leafmc.gomoku;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class PieceAnimator {
    private static final double MIN_DROP_HEIGHT = 2.0D;

    private final Plugin plugin;
    private final Set<Entity> active = new HashSet<>();
    private final Set<BukkitTask> tasks = new HashSet<>();
    private ArenaConfig arena;

    public PieceAnimator(Plugin plugin, ArenaConfig arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void updateArena(ArenaConfig arena) {
        this.arena = arena;
        cleanup();
    }

    public void animate(Stone stone, int row, int column, Runnable finished) {
        animate(stone, arena.materialFor(stone), row, column, finished);
    }

    public void animate(Stone stone, PieceSkin skin, int row, int column, Runnable finished) {
        if (skin == null) {
            animate(stone, row, column, finished);
            return;
        }
        if (skin.displayType() == PieceDisplayType.ENTITY) {
            animateEntity(stone, skin, row, column, finished);
            return;
        }
        animateBlock(stone, skin.animationMaterial(), skin, row, column, finished);
    }

    public void animate(Stone stone, Material material, int row, int column, Runnable finished) {
        animateBlock(stone, material, null, row, column, finished);
    }

    private void animateBlock(Stone stone, Material material, PieceSkin skin, int row, int column, Runnable finished) {
        tasks.removeIf(BukkitTask::isCancelled);
        if (!arena.enabled() || arena.world() == null) {
            finished.run();
            return;
        }
        Location end = arena.centeredLocation(arena.geometry().piecePoint(row, column));
        if (end == null) {
            finished.run();
            return;
        }
        Location start = dropStart(end);

        BlockDisplay display = start.getWorld().spawn(start, BlockDisplay.class, entity -> {
            entity.setBlock(material.createBlockData());
            entity.setGravity(false);
            entity.setPersistent(false);
            entity.addScoreboardTag(GomokuEntityTags.ANIMATION);
            entity.setViewRange(64.0F);
            entity.setDisplayWidth(1.0F);
            entity.setDisplayHeight(1.0F);
            entity.setInterpolationDelay(0);
            entity.setInterpolationDuration(2);
            entity.setTeleportDuration(1);
        });
        active.add(display);

        int duration = Math.max(1, arena.animationTicks());
        World world = start.getWorld();
        world.playSound(start, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 0.7F, 1.45F);
        world.spawnParticle(Particle.CLOUD, start, 8, 0.16D, 0.16D, 0.16D, 0.01D);
        BukkitTask task = new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!display.isValid()) {
                    active.remove(display);
                    land(end, skin);
                    finished.run();
                    cancel();
                    return;
                }
                tick++;
                double rawProgress = Math.min(1.0D, (double) tick / duration);
                Location next = verticalDropLocation(start, end, rawProgress);
                display.teleport(next);
                if (tick % 3 == 0) {
                    world.spawnParticle(Particle.END_ROD, next, 1, 0.02D, 0.04D, 0.02D, 0.0D);
                }
                if (tick >= duration) {
                    display.remove();
                    active.remove(display);
                    land(end, skin);
                    finished.run();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        tasks.add(task);
    }

    private void animateEntity(Stone stone, PieceSkin skin, int row, int column, Runnable finished) {
        tasks.removeIf(BukkitTask::isCancelled);
        if (!arena.enabled() || arena.world() == null) {
            finished.run();
            return;
        }
        Location end = entityLocation(row, column);
        Location start = end == null ? null : dropStart(end);
        EntityType type = entityType(skin.entityType());
        if (start == null || end == null || type == null || type.getEntityClass() == null) {
            animateBlock(stone, skin.animationMaterial(), skin, row, column, finished);
            return;
        }
        Entity entity = start.getWorld().spawnEntity(start, type);
        configureMovingEntity(entity);
        face(entity, end);
        active.add(entity);

        int duration = Math.max(1, arena.animationTicks());
        World world = start.getWorld();
        world.playSound(start, Sound.ENTITY_CHICKEN_STEP, 0.7F, 1.2F);
        BukkitTask task = new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!entity.isValid()) {
                    active.remove(entity);
                    land(end, skin);
                    finished.run();
                    cancel();
                    return;
                }
                tick++;
                double progress = Math.min(1.0D, (double) tick / duration);
                Location next = verticalDropLocation(start, end, progress);
                entity.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
                entity.teleport(next);
                face(entity, end);
                if (tick % 5 == 0) {
                    world.spawnParticle(Particle.CLOUD, next, 1, 0.08D, 0.02D, 0.08D, 0.0D);
                    world.playSound(next, Sound.ENTITY_CHICKEN_STEP, 0.25F, 0.9F);
                }
                if (tick >= duration) {
                    entity.remove();
                    active.remove(entity);
                    land(end, skin);
                    finished.run();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        tasks.add(task);
    }

    private Location entityLocation(int row, int column) {
        Location location = arena.location(arena.geometry().piecePoint(row, column));
        return location == null ? null : location.add(0.5D, 0.0D, 0.5D);
    }

    private Location dropStart(Location end) {
        return end.clone().add(0.0D, dropHeight(), 0.0D);
    }

    private Location verticalDropLocation(Location start, Location end, double progress) {
        double eased = easeIn(progress);
        double y = start.getY() + (end.getY() - start.getY()) * eased;
        Location next = end.clone();
        next.setX(start.getX());
        next.setY(y);
        next.setZ(start.getZ());
        return next;
    }

    private double easeIn(double progress) {
        return progress * progress;
    }

    private double dropHeight() {
        return Math.max(MIN_DROP_HEIGHT, arena.animationArcHeight());
    }

    private void land(Location end, PieceSkin skin) {
        World world = end.getWorld();
        if (world == null) {
            return;
        }
        PieceAnimationType animationType = skin == null ? PieceAnimationType.ARC : skin.animationType();
        if (animationType == PieceAnimationType.EXPLOSION) {
            world.spawnParticle(Particle.EXPLOSION_EMITTER, end, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            world.spawnParticle(Particle.SMOKE, end, 18, 0.35D, 0.25D, 0.35D, 0.02D);
            world.playSound(end, Sound.ENTITY_GENERIC_EXPLODE, 0.45F, 1.25F);
            return;
        }
        if (animationType == PieceAnimationType.WALK) {
            world.spawnParticle(Particle.CLOUD, end, 8, 0.2D, 0.08D, 0.2D, 0.0D);
            world.playSound(end, Sound.ENTITY_CHICKEN_STEP, 0.45F, 0.8F);
            return;
        }
        if (animationType == PieceAnimationType.POP) {
            world.spawnParticle(Particle.CLOUD, end, 10, 0.25D, 0.2D, 0.25D, 0.01D);
            world.spawnParticle(Particle.END_ROD, end, 6, 0.18D, 0.18D, 0.18D, 0.0D);
            world.playSound(end, Sound.BLOCK_NOTE_BLOCK_PLING, 0.75F, 1.65F);
            return;
        }
        world.spawnParticle(Particle.FIREWORK, end, 18, 0.35D, 0.35D, 0.35D, 0.02D);
        world.playSound(end, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9F, 1.2F);
    }

    private void configureMovingEntity(Entity entity) {
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setGravity(false);
        entity.setPersistent(false);
        entity.addScoreboardTag(GomokuEntityTags.ANIMATION);
        if (entity instanceof Slime slime) {
            slime.setSize(1);
        }
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setCollidable(false);
            living.setRemoveWhenFarAway(false);
        }
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

    public void cleanup() {
        for (BukkitTask task : new HashSet<>(tasks)) {
            task.cancel();
        }
        tasks.clear();
        for (Entity entity : new HashSet<>(active)) {
            if (entity.isValid()) {
                entity.remove();
            }
        }
        active.clear();
    }
}
