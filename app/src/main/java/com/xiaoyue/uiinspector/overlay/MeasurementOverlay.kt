package com.xiaoyue.uiinspector.overlay

import android.content.Context
import android.graphics.*
import android.view.View
import com.xiaoyue.uiinspector.analysis.SelectedItemAnalysis
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.util.Bounds
import kotlin.math.*

/** Screen-space geometry is converted to this window's local coordinates for every draw. */
class MeasurementOverlay(context: Context, private val host: OverlayController,
                         private val analysis: SelectedItemAnalysis, private val reserved: Rect? = null) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val origin = IntArray(2)
    private val labels = mutableListOf<RectF>()
    private val sizeColor = 0xff008cff.toInt()
    private val gapColor = 0xffffa726.toInt()
    private fun dp(value: Int) = host.dp(value).toFloat()
    private fun local(b: Bounds) = RectF((b.left - origin[0]).toFloat(), (b.top - origin[1]).toFloat(), (b.right - origin[0]).toFloat(), (b.bottom - origin[1]).toFloat())

    override fun onDraw(canvas: Canvas) {
        getLocationOnScreen(origin)
        labels.clear()
        reserved?.let { labels.add(RectF(it).apply { offset(-origin[0].toFloat(), -origin[1].toFloat()) }) }
        val b = local(analysis.boundsPx)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(2); paint.color = sizeColor
        canvas.drawRect(b, paint)
        val widthY = if (b.top > dp(76)) b.top - dp(16) else (b.bottom + dp(16)).coerceAtMost(height - dp(8))
        ruler(canvas, b.left, widthY, b.right, widthY, sizeColor)
        label(canvas, "W ${analysis.width.lines()}", b.centerX(), widthY - dp(27), sizeColor)
        val heightX = if (b.right + dp(90) < width) b.right + dp(16) else (b.left - dp(16)).coerceAtLeast(dp(8))
        ruler(canvas, heightX, b.top, heightX, b.bottom, sizeColor)
        label(canvas, "H ${analysis.height.lines()}", heightX + dp(40), b.centerY(), sizeColor)

        analysis.neighbors.values.forEach { n ->
            val other = local(n.node.bounds)
            val x = (max(b.left, other.left) + min(b.right, other.right)) / 2f
            val y = (max(b.top, other.top) + min(b.bottom, other.bottom)) / 2f
            val points = when (n.direction) {
                Direction.TOP -> floatArrayOf(x, other.bottom, x, b.top)
                Direction.BOTTOM -> floatArrayOf(x, b.bottom, x, other.top)
                Direction.LEFT -> floatArrayOf(other.right, y, b.left, y)
                Direction.RIGHT -> floatArrayOf(b.right, y, other.left, y)
            }
            ruler(canvas, points[0], points[1], points[2], points[3], gapColor)
            label(canvas, "${n.direction.name.lowercase().replaceFirstChar { it.uppercase() }} ${n.distance.lines()}",
                (points[0] + points[2]) / 2, (points[1] + points[3]) / 2, gapColor)
        }
        analysis.pair?.let { pair ->
            val a = local(pair.a); val p = local(pair.b)
            paint.style = Paint.Style.STROKE; paint.color = 0xffab47bc.toInt(); canvas.drawRect(a, paint); canvas.drawRect(p, paint)
            label(canvas, "A", a.left + dp(12), a.top + dp(12), paint.color)
            label(canvas, "B", p.left + dp(12), p.top + dp(12), 0xffab47bc.toInt())
            if (pair.relation == PairRelation.VERTICAL) {
                val x = (max(a.left, p.left) + min(a.right, p.right)) / 2
                val top = if (a.bottom <= p.top) a.bottom else p.bottom
                val bottom = if (a.bottom <= p.top) p.top else a.top
                ruler(canvas, x, top, x, bottom, 0xffab47bc.toInt())
                label(canvas, "A ↔ B ${pair.vertical.lines()}", x, (top + bottom) / 2, 0xffab47bc.toInt())
            } else if (pair.relation == PairRelation.HORIZONTAL) {
                val y = (max(a.top, p.top) + min(a.bottom, p.bottom)) / 2
                val left = if (a.right <= p.left) a.right else p.right
                val right = if (a.right <= p.left) p.left else a.left
                ruler(canvas, left, y, right, y, 0xffab47bc.toInt())
                label(canvas, "A ↔ B ${pair.horizontal.lines()}", (left + right) / 2, y, 0xffab47bc.toInt())
            }
        }
        val main = analysis.renderedColor?.colors?.dominantHex
        val colorLabel = main ?: if (analysis.renderedColor == null) "Analyzing color…" else "Color unavailable / no single main color"
        label(canvas, colorLabel + if (analysis.frozen) " · Frozen" else "", b.centerX(), b.bottom - dp(12), sizeColor, analysis.renderedColor?.colors?.dominantColor)
    }

    private fun ruler(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        paint.style = Paint.Style.STROKE; paint.color = color; paint.strokeWidth = dp(2)
        canvas.drawLine(x1, y1, x2, y2, paint)
        val horizontal = abs(y2 - y1) < .5f
        val tick = dp(5)
        if (horizontal) { canvas.drawLine(x1, y1 - tick, x1, y1 + tick, paint); canvas.drawLine(x2, y2 - tick, x2, y2 + tick, paint) }
        else { canvas.drawLine(x1 - tick, y1, x1 + tick, y1, paint); canvas.drawLine(x2 - tick, y2, x2 + tick, y2, paint) }
    }

    private fun label(canvas: Canvas, text: String, x: Float, y: Float, accent: Int, swatch: Int? = null) {
        paint.textSize = dp(12); paint.typeface = Typeface.DEFAULT_BOLD
        val lines = text.split('\n')
        val inset = if (swatch == null) dp(6) else dp(24)
        val w = (lines.maxOf { paint.measureText(it) } + inset + dp(6)).coerceAtMost(width.toFloat())
        val h = lines.size * dp(17) + dp(6)
        fun at(cx: Float, cy: Float) = RectF(0f, 0f, w, h).apply {
            offset((cx - w / 2).coerceIn(0f, (width - w).coerceAtLeast(0f)), (cy - h / 2).coerceIn(dp(4), (height - h).coerceAtLeast(dp(4))))
        }
        val choices = buildList {
            add(at(x, y))
            for (ring in 1..8) {
                add(at(x + ring * (w + dp(6)), y)); add(at(x - ring * (w + dp(6)), y))
                add(at(x, y - ring * (h + dp(5)))); add(at(x, y + ring * (h + dp(5))))
            }
        }
        val rect = choices.firstOrNull { r -> labels.none { RectF.intersects(it, r) } } ?: choices.first()
        labels.add(rect)
        if (!rect.contains(x, y)) {
            paint.style = Paint.Style.STROKE; paint.color = accent; paint.strokeWidth = dp(1)
            canvas.drawLine(x, y, x.coerceIn(rect.left, rect.right), y.coerceIn(rect.top, rect.bottom), paint)
        }
        paint.style = Paint.Style.FILL; paint.color = 0xee102334.toInt(); canvas.drawRoundRect(rect, dp(4), dp(4), paint)
        paint.color = accent; paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(1); canvas.drawRoundRect(rect, dp(4), dp(4), paint)
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        lines.forEachIndexed { i, line -> canvas.drawText(line, rect.left + inset, rect.top + dp(15) + i * dp(17), paint) }
        swatch?.let { paint.color = it; canvas.drawRect(rect.left + dp(5), rect.top + dp(5), rect.left + dp(19), rect.top + dp(19), paint) }
    }
    fun show() = host.add(this, host.params(-1, -1, false))
}
