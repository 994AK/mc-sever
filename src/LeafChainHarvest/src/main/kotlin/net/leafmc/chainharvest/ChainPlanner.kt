package net.leafmc.chainharvest

import org.bukkit.Material

data class BlockPoint(val x: Int, val y: Int, val z: Int) {
    fun relative(dx: Int, dy: Int, dz: Int): BlockPoint = BlockPoint(x + dx, y + dy, z + dz)
}

fun interface BlockTypeLookup {
    fun typeAt(point: BlockPoint): Material?
}

class ChainPlanner {
    fun plan(
        start: BlockPoint,
        type: Material,
        settings: ChainGroupSettings,
        lookup: BlockTypeLookup,
        bypassLimit: Boolean = false,
    ): List<BlockPoint> {
        if (!settings.enabled || !settings.blocks.contains(type)) {
            return emptyList()
        }

        val queue = ArrayDeque<BlockPoint>()
        val seen = linkedSetOf<BlockPoint>()
        val result = mutableListOf<BlockPoint>()
        queue += start
        seen += start

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current != start) {
                if (!bypassLimit && result.size >= settings.maxBlocks) {
                    break
                }
                if (lookup.typeAt(current) != type) {
                    continue
                }
                result += current
            }

            for (neighbor in neighbors(current, settings.diagonals)) {
                if (neighbor !in seen && lookup.typeAt(neighbor) == type) {
                    seen += neighbor
                    queue += neighbor
                }
            }
        }

        return result
    }

    private fun neighbors(point: BlockPoint, diagonals: Boolean): List<BlockPoint> {
        if (!diagonals) {
            return listOf(
                point.relative(1, 0, 0),
                point.relative(-1, 0, 0),
                point.relative(0, 1, 0),
                point.relative(0, -1, 0),
                point.relative(0, 0, 1),
                point.relative(0, 0, -1),
            )
        }
        val result = ArrayList<BlockPoint>(26)
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue
                    }
                    result += point.relative(dx, dy, dz)
                }
            }
        }
        return result
    }
}
