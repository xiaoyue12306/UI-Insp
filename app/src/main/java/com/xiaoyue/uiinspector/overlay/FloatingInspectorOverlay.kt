package com.xiaoyue.uiinspector.overlay

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.LinearLayout
import kotlin.math.abs

class FloatingInspectorOverlay(context: Context, private val host: OverlayController, select: () -> Unit, stop: () -> Unit,
                               pair: () -> Unit = {}, picker: () -> Unit = {}) {
    val view = LinearLayout(context).apply { setBackgroundColor(0xff284b63.toInt()) }
    private val selectButton = Button(context).apply { text = "Select"; textSize = 12f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(0xff284b63.toInt()) }
    private val params = host.params(host.dp(280), host.dp(48)).apply { x = host.dp(12); y = host.dp(90) }
    fun label(text: String) { selectButton.text = text }
    init {
        view.addView(selectButton, LinearLayout.LayoutParams(0, -1, 1f))
        listOf("A ↔ B" to pair, "Picker" to picker, "×" to stop).forEach { (label, action) ->
            view.addView(Button(context).apply {
                text = label; textSize = 12f; isAllCaps = false
                contentDescription = if (label == "×") "Close Inspector" else if (label == "Picker") "Color Picker" else "Measure between items"
                setTextColor(Color.WHITE); setBackgroundColor(0xff284b63.toInt()); setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, -1, 1f))
        }
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var startX = 0f; var startY = 0f; var originX = 0; var originY = 0; var moved = false
        selectButton.setOnClickListener { select() }
        selectButton.setOnLongClickListener { stop(); true }
        selectButton.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX; startY = event.rawY; originX = params.x; originY = params.y; moved = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(event.rawX - startX) + abs(event.rawY - startY) > slop) moved = true
                    if (moved) {
                        selectButton.cancelLongPress()
                        val b = host.manager.currentWindowMetrics.bounds
                        params.x = (originX + event.rawX - startX).toInt().coerceIn(0, (b.width() - params.width).coerceAtLeast(0))
                        params.y = (originY + event.rawY - startY).toInt().coerceIn(0, (b.height() - params.height).coerceAtLeast(0))
                        host.update(view, params)
                    }
                    moved
                }
                MotionEvent.ACTION_UP -> if (moved) { selectButton.isPressed = false; true } else false
                else -> false
            }
        }
    }
    fun show() = host.add(view, params)
    fun remove() = host.remove(view)
}
