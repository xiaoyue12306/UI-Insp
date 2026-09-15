package com.xiaoyue.uiinspector.overlay

import com.xiaoyue.uiinspector.measurement.*

enum class LayoutDirectionHint { VERTICAL, HORIZONTAL, MIXED }
object SpacingPresentation {
    private fun vertical(n: NeighborMeasurement) = n.direction == Direction.TOP || n.direction == Direction.BOTTOM
    fun hint(neighbors: Collection<NeighborMeasurement>): LayoutDirectionHint {
        fun score(items: List<NeighborMeasurement>) = if (items.isEmpty()) 0f else items.sumOf { (it.overlapRatio * it.confidence / (1f + it.distance.dp / 24f)).toDouble() }.toFloat()
        val v = score(neighbors.filter(::vertical)); val h = score(neighbors.filterNot(::vertical))
        return when { v > h * 1.4f -> LayoutDirectionHint.VERTICAL; h > v * 1.4f -> LayoutDirectionHint.HORIZONTAL; else -> LayoutDirectionHint.MIXED }
    }
    fun visible(neighbors: Map<Direction, NeighborMeasurement>, all: Boolean): List<NeighborMeasurement> {
        if (all) return neighbors.values.toList()
        val subset = when (hint(neighbors.values)) {
            LayoutDirectionHint.VERTICAL -> neighbors.values.filter(::vertical)
            LayoutDirectionHint.HORIZONTAL -> neighbors.values.filterNot(::vertical)
            LayoutDirectionHint.MIXED -> neighbors.values.toList()
        }
        return subset.sortedWith(compareBy<NeighborMeasurement> { it.distance.dp }.thenByDescending { it.confidence }).take(2)
    }
}
