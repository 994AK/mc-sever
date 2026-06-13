package net.leafmc.chainharvest

import org.bukkit.Material

data class SowTarget(val substrate: BlockPoint, val target: BlockPoint)

class FarmPlanner {
    fun verticalHarvestTargets(clicked: BlockPoint, type: Material, lookup: BlockTypeLookup, maxTargets: Int): List<BlockPoint> {
        var base = clicked
        for (distance in 1..32) {
            val below = clicked.relative(0, -distance, 0)
            if (lookup.typeAt(below) != type) {
                break
            }
            base = below
        }

        val result = mutableListOf<BlockPoint>()
        for (distance in 1..32) {
            if (result.size >= maxTargets) {
                break
            }
            val above = base.relative(0, distance, 0)
            if (lookup.typeAt(above) != type) {
                break
            }
            result += above
        }
        return result
    }

    fun sowTargets(center: BlockPoint, radius: Int, maxTargets: Int, rule: SeedRule, lookup: BlockTypeLookup): List<SowTarget> {
        if (maxTargets <= 0) {
            return emptyList()
        }
        val result = mutableListOf<SowTarget>()
        scanCube(center, radius) { point ->
            if (result.size >= maxTargets) {
                return@scanCube false
            }
            val target = point.relative(0, 1, 0)
            if (lookup.typeAt(point) in rule.substrates && lookup.typeAt(target) == Material.AIR) {
                result += SowTarget(point, target)
            }
            true
        }
        return result
    }

    fun fertilizeTargets(
        center: BlockPoint,
        radius: Int,
        maxTargets: Int,
        fertilizable: Set<Material>,
        lookup: BlockTypeLookup,
    ): List<BlockPoint> {
        if (maxTargets <= 0) {
            return emptyList()
        }
        val result = mutableListOf<BlockPoint>()
        scanCube(center, radius) { point ->
            if (result.size >= maxTargets) {
                return@scanCube false
            }
            val type = lookup.typeAt(point)
            if (type != null && fertilizable.contains(type)) {
                result += point
            }
            true
        }
        return result
    }

    private fun scanCube(center: BlockPoint, radius: Int, visitor: (BlockPoint) -> Boolean) {
        val safeRadius = radius.coerceAtLeast(0)
        for (dy in -1..1) {
            for (dx in -safeRadius..safeRadius) {
                for (dz in -safeRadius..safeRadius) {
                    if (!visitor(center.relative(dx, dy, dz))) {
                        return
                    }
                }
            }
        }
    }
}
