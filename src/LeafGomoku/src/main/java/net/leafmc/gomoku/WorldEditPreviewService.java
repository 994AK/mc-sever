package net.leafmc.gomoku;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class WorldEditPreviewService {
    private final LeafGomokuPlugin plugin;

    public WorldEditPreviewService(LeafGomokuPlugin plugin) {
        this.plugin = plugin;
    }

    public String show(Player player, ArenaConfig config) {
        org.bukkit.World bukkitWorld = config.world();
        if (bukkitWorld == null) {
            return "§e无法设置 WorldEdit 预览：世界未加载 " + config.worldName() + "。";
        }
        Plugin candidate = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        if (candidate == null || !candidate.isEnabled()) {
            BoardGeometry.BoardRegion region = config.geometry().boardRegion();
            return "§e未检测到 WorldEdit，已给出文字预览坐标: §7" + region.minimum() + " -> " + region.maximum();
        }

        try {
            BoardGeometry.BoardRegion region = config.geometry().boardRegion();
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> worldType = Class.forName("com.sk89q.worldedit.world.World");
            Class<?> vectorType = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Class<?> selectorType = Class.forName("com.sk89q.worldedit.regions.selector.CuboidRegionSelector");
            Class<?> regionSelectorType = Class.forName("com.sk89q.worldedit.regions.RegionSelector");
            Class<?> actorType = Class.forName("com.sk89q.worldedit.extension.platform.Actor");

            Method adaptWorld = bukkitAdapter.getMethod("adapt", org.bukkit.World.class);
            Method adaptPlayer = bukkitAdapter.getMethod("adapt", Player.class);
            Object world = adaptWorld.invoke(null, bukkitWorld);
            Object actor = adaptPlayer.invoke(null, player);
            Object session = candidate.getClass().getMethod("getSession", Player.class).invoke(candidate, player);

            Constructor<?> selectorConstructor = selectorType.getConstructor(worldType, vectorType, vectorType);
            Object selector = selectorConstructor.newInstance(world, vector(vectorType, region.minimum()), vector(vectorType, region.maximum()));
            session.getClass().getMethod("setRegionSelector", worldType, regionSelectorType).invoke(session, world, selector);
            session.getClass().getMethod("dispatchCUISelection", actorType).invoke(session, actor);
            return "§aWorldEdit 已选中棋盘预览区域。";
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            plugin.getLogger().warning("Could not show LeafGomoku WorldEdit preview: " + rootMessage(error));
            BoardGeometry.BoardRegion region = config.geometry().boardRegion();
            return "§eWorldEdit 预览设置失败，文字坐标: §7" + region.minimum() + " -> " + region.maximum();
        }
    }

    private Object vector(Class<?> vectorType, BlockPoint point) throws ReflectiveOperationException {
        return vectorType.getMethod("at", int.class, int.class, int.class).invoke(null, point.x(), point.y(), point.z());
    }

    private String rootMessage(Throwable error) {
        Throwable root = error instanceof InvocationTargetException invocation && invocation.getTargetException() != null
            ? invocation.getTargetException()
            : error;
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
