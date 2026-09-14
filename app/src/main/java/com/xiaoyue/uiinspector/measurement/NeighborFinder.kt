package com.xiaoyue.uiinspector.measurement

import com.xiaoyue.uiinspector.inspector.NodeSnapshot
import com.xiaoyue.uiinspector.util.Bounds

data class NeighborMeasurement(val node: NodeSnapshot, val direction: Direction,
                               val distance: DimensionValue, val overlapRatio: Float, val confidence: Float)
data class NeighborConfig(val minimumDimensionDp: Float = 2f, val minimumOverlap: Float = .20f,
                          val minimumConfidence: Float = .45f)

class NeighborFinder(private val config: NeighborConfig = NeighborConfig()) {
    fun find(selected: NodeSnapshot, nodes: List<NodeSnapshot>, window: Bounds,
             inspectorPackage: String = "com.xiaoyue.uiinspector"): Map<Direction, NeighborMeasurement> {
        val byId = nodes.associateBy { it.index }
        fun ancestors(node: NodeSnapshot): Set<Int> {
            val ids = mutableSetOf<Int>()
            var id = node.parentIndex
            while (id != null && ids.add(id)) id = byId[id]?.parentIndex
            return ids
        }
        val parents = ancestors(selected)
        val candidates = nodes.filter { n ->
            n.index != selected.index && n.windowId == selected.windowId && n.visibleToUser &&
                n.packageName != inspectorPackage && n.bounds.area > 0 && n.bounds != selected.bounds &&
                n.index !in parents && selected.index !in ancestors(n) &&
                !SpacingCalculator.contains(n.bounds, selected.bounds) && !SpacingCalculator.contains(selected.bounds, n.bounds) &&
                n.bounds.clip(window) == n.bounds // Partly off-window bounds aren't reliable visual neighbors.
        }
        return Direction.entries.mapNotNull { direction ->
            val options = candidates.mapNotNull { n ->
                val gap = SpacingCalculator.gap(selected.bounds, n.bounds, direction) ?: return@mapNotNull null
                val vertical = direction == Direction.TOP || direction == Direction.BOTTOM
                val overlap = if (vertical) SpacingCalculator.overlapX(selected.bounds, n.bounds) else SpacingCalculator.overlapY(selected.bounds, n.bounds)
                val selectedSpan = if (vertical) selected.bounds.width else selected.bounds.height
                val otherSpan = if (vertical) n.bounds.width else n.bounds.height
                // Require meaningful overlap with BOTH elements, avoiding corner-adjacent slivers.
                val ratio = overlap.toFloat() / maxOf(selectedSpan, otherSpan).coerceAtLeast(1)
                if (ratio < config.minimumOverlap) return@mapNotNull null
                val minDp = minOf(n.bounds.width, n.bounds.height) / selected.density
                val maxDp = maxOf(n.bounds.width, n.bounds.height) / selected.density
                // Keep long thin dividers, but reject tiny specks in both dimensions.
                if (maxDp < config.minimumDimensionDp) return@mapNotNull null
                val sizeQuality = if (minDp < config.minimumDimensionDp) .65f else 1f
                val confidence = (.55f * ratio + .30f * sizeQuality + .15f / (1f + gap / selected.density / 100f)).coerceIn(0f, 1f)
                if (confidence < config.minimumConfidence) return@mapNotNull null
                NeighborMeasurement(n, direction, DimensionValue.fromPx(gap, selected.density), ratio, confidence)
            }
            // Among reliable candidates, minimum gap wins. At equal gaps prefer overlap/quality/depth.
            options.sortedWith(compareBy<NeighborMeasurement> { it.distance.px }
                .thenByDescending { it.overlapRatio }.thenByDescending { it.confidence }
                .thenByDescending { it.node.depth }.thenBy { it.node.index }).firstOrNull()?.let { direction to it }
        }.toMap()
    }
}
