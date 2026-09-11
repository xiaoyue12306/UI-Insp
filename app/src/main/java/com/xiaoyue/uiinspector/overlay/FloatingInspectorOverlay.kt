package com.xiaoyue.uiinspector.overlay

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Button
import kotlin.math.abs

class FloatingInspectorOverlay(context: Context, private val host: OverlayController, select: () -> Unit, stop: () -> Unit) {
    val view = Button(context).apply { text = "Inspect"; setTextColor(Color.WHITE); setBackgroundColor(0xff284b63.toInt()) }
    private val params = host.params(host.dp(96), host.dp(52)).apply { x = host.dp(12); y = host.dp(100) }
    init {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var startX = 0f; var startY = 0f; var originX = 0; var originY = 0; var moved = false
        view.setOnClickListener { select() }
        view.setOnLongClickListener { stop(); true }
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX; startY = event.rawY; originX = params.x; originY = params.y; moved = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(event.rawX - startX) + abs(event.rawY - startY) > slop) moved = true
                    if (moved) {
                        val b = host.manager.currentWindowMetrics.bounds
                        params.x = (originX + event.rawX - startX).toInt().coerceIn(0, (b.width() - params.width).coerceAtLeast(0))
                        params.y = (originY + event.rawY - startY).toInt().coerceIn(0, (b.height() - params.height).coerceAtLeast(0))
                        host.update(view, params)
                    }
                    moved
                }
                MotionEvent.ACTION_UP -> if (moved) { view.isPressed = false; true } else false
                else -> false
            }
        }
    }
    fun show() = host.add(view, params)
    fun remove() = host.remove(view)
}
