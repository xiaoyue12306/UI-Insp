package com.xiaoyue.uiinspector.overlay
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.WindowManager
import com.xiaoyue.uiinspector.inspector.NodeSnapshot
import com.xiaoyue.uiinspector.util.formatDp

class HighlightOverlay(context: Context, private val host: OverlayController, private val node: NodeSnapshot) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val origin = IntArray(2); getLocationOnScreen(origin)
        val b = node.bounds
        val l = (b.left - origin[0]).toFloat(); val t = (b.top - origin[1]).toFloat()
        paint.color = 0xff008cff.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = host.dp(2).toFloat()
        canvas.drawRect(l, t, (b.right - origin[0]).toFloat(), (b.bottom - origin[1]).toFloat(), paint)
        val label = "${node.className?.substringAfterLast('.')} ${node.resourceId?.substringAfterLast('/')?.let { "#$it" }.orEmpty()} · ${formatDp(node.widthDp)} × ${formatDp(node.heightDp)} dp"
        paint.style = Paint.Style.FILL; paint.textSize = host.dp(12).toFloat()
        val labelWidth = paint.measureText(label) + host.dp(12)
        val labelX = l.coerceIn(0f, (width - labelWidth).coerceAtLeast(0f))
        val labelY = (t - host.dp(6)).coerceIn(host.dp(22).toFloat(), height.toFloat())
        canvas.drawRect(labelX, labelY - host.dp(19), labelX + labelWidth, labelY + host.dp(4), paint)
        paint.color = Color.WHITE; canvas.drawText(label, labelX + host.dp(6), labelY, paint)
    }
    fun show() = host.add(this, host.params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, false))
}
