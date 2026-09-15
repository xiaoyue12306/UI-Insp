package com.xiaoyue.uiinspector.overlay

/** Pure screen geometry. Critical labels can overlap the item only when no outside space exists. */
data class PlacementRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    fun intersects(other: PlacementRect) = left < other.right && right > other.left && top < other.bottom && bottom > other.top
    fun intersectionArea(other: PlacementRect) = (minOf(right,other.right)-maxOf(left,other.left)).coerceAtLeast(0f) * (minOf(bottom,other.bottom)-maxOf(top,other.top)).coerceAtLeast(0f)
}

class LabelPlacementEngine(private val screen: PlacementRect, private val item: PlacementRect,
                           private val neighbors: List<PlacementRect>, reserved: List<PlacementRect>, private val gap: Float) {
    private val occupied = reserved.toMutableList()
    fun place(width: Float, height: Float, x: Float, y: Float, critical: Boolean): PlacementRect? {
        if (width > screen.width || height > screen.height) return null
        fun rect(cx: Float, cy: Float): PlacementRect {
            val l = (cx-width/2).coerceIn(screen.left, screen.right-width)
            val t = (cy-height/2).coerceIn(screen.top, screen.bottom-height)
            return PlacementRect(l,t,l+width,t+height)
        }
        val candidates = buildList {
            add(rect(x,y))
            add(rect(x,item.top-gap-height/2)); add(rect(x,item.bottom+gap+height/2))
            add(rect(item.right+gap+width/2,y)); add(rect(item.left-gap-width/2,y))
            for (ring in 1..8) {
                add(rect(x+ring*(width+gap),y)); add(rect(x-ring*(width+gap),y))
                add(rect(x,y-ring*(height+gap))); add(rect(x,y+ring*(height+gap)))
            }
        }.distinct().filter { c -> occupied.none(c::intersects) }
        val outside = candidates.filterNot { it.intersects(item) }
        val allowed = outside.ifEmpty { if (critical) candidates else emptyList() }
        val chosen = allowed.minByOrNull { c ->
            neighbors.sumOf { c.intersectionArea(it).toDouble() } * 20 +
                kotlin.math.abs((c.left+c.right)/2-x) + kotlin.math.abs((c.top+c.bottom)/2-y)
        }
        if (chosen != null) occupied.add(chosen)
        return chosen
    }
}

object ResultCardPlacement {
    fun place(screen: PlacementRect, item: PlacementRect, width: Float, height: Float, margin: Float): PlacementRect {
        val w = width.coerceAtMost(screen.width-2*margin); val h = height.coerceAtMost(screen.height-2*margin)
        val left = screen.left+margin; val right = screen.right-margin-w
        val top = screen.top+margin; val bottom = screen.bottom-margin-h
        val preferBottom = (item.top+item.bottom)/2 < (screen.top+screen.bottom)/2
        val ys = if (preferBottom) listOf(bottom,top) else listOf(top,bottom)
        val xs = if ((item.left+item.right)/2 < (screen.left+screen.right)/2) listOf(right,left) else listOf(left,right)
        return ys.flatMap { y -> xs.map { x -> PlacementRect(x,y,x+w,y+h) } }.minBy { it.intersectionArea(item) }
    }
}
