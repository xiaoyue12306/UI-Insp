package com.xiaoyue.uiinspector.measurement

import com.xiaoyue.uiinspector.util.Bounds

data class PairMeasurement(val a: Bounds, val b: Bounds, val horizontal: DimensionValue,
                           val vertical: DimensionValue, val relation: PairRelation)
enum class PairRelation { HORIZONTAL, VERTICAL, DIAGONAL, OVERLAPPING }

object SpacingCalculator {
    fun overlapX(a: Bounds, b: Bounds) = (minOf(a.right, b.right) - maxOf(a.left, b.left)).coerceAtLeast(0)
    fun overlapY(a: Bounds, b: Bounds) = (minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)).coerceAtLeast(0)
    fun contains(a: Bounds, b: Bounds) = a.left <= b.left && a.top <= b.top && a.right >= b.right && a.bottom >= b.bottom

    fun gap(a: Bounds, b: Bounds, direction: Direction): Float? = when (direction) {
        Direction.TOP -> if (b.bottom <= a.top && overlapX(a, b) > 0) (a.top - b.bottom).toFloat() else null
        Direction.BOTTOM -> if (b.top >= a.bottom && overlapX(a, b) > 0) (b.top - a.bottom).toFloat() else null
        Direction.LEFT -> if (b.right <= a.left && overlapY(a, b) > 0) (a.left - b.right).toFloat() else null
        Direction.RIGHT -> if (b.left >= a.right && overlapY(a, b) > 0) (b.left - a.right).toFloat() else null
    }

    fun between(a: Bounds, b: Bounds, density: Float): PairMeasurement {
        val horizontal = maxOf(0, b.left - a.right, a.left - b.right).toFloat()
        val vertical = maxOf(0, b.top - a.bottom, a.top - b.bottom).toFloat()
        val relation = when {
            overlapX(a, b) > 0 && overlapY(a, b) > 0 -> PairRelation.OVERLAPPING
            overlapY(a, b) > 0 -> PairRelation.HORIZONTAL
            overlapX(a, b) > 0 -> PairRelation.VERTICAL
            else -> PairRelation.DIAGONAL
        }
        return PairMeasurement(a, b, DimensionValue.fromPx(horizontal, density), DimensionValue.fromPx(vertical, density), relation)
    }

    fun windowEdges(a: Bounds, window: Bounds, density: Float): Map<Direction, DimensionValue> = mapOf(
        Direction.TOP to (a.top - window.top), Direction.BOTTOM to (window.bottom - a.bottom),
        Direction.LEFT to (a.left - window.left), Direction.RIGHT to (window.right - a.right)
    ).mapValues { DimensionValue.fromPx(it.value.toFloat(), density) }
}
