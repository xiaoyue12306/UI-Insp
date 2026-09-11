package com.xiaoyue.uiinspector.util

/** Immutable screen-space rectangle, independent of Android for deterministic tests. */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = (right - left).coerceAtLeast(0)
    val height get() = (bottom - top).coerceAtLeast(0)
    val area get() = width.toLong() * height
    fun contains(x: Int, y: Int) = x >= left && x < right && y >= top && y < bottom
    fun clip(other: Bounds): Bounds? {
        val result = Bounds(maxOf(left, other.left), maxOf(top, other.top), minOf(right, other.right), minOf(bottom, other.bottom))
        return result.takeIf { it.width > 0 && it.height > 0 }
    }
    override fun toString() = "[$left,$top][$right,$bottom]"
}
