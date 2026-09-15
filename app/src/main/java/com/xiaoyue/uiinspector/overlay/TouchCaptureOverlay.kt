package com.xiaoyue.uiinspector.overlay
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
class TouchCaptureOverlay(context: Context, private val host: OverlayController, picked: (Int, Int) -> Unit,
                          back: () -> Unit = {}, private val preview: ((View, Float, Float) -> Unit)? = null) {
    val view = InspectorSurface(context, back).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        setOnTouchListener { _, event ->
            // rawX/rawY are screen coordinates, unlike coordinates relative to this overlay.
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) preview?.invoke(this,event.rawX,event.rawY)
            if (event.actionMasked == MotionEvent.ACTION_UP) picked(event.rawX.toInt(), event.rawY.toInt())
            true
        }
    }
    fun show() = host.add(view, host.focusableParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT))
    fun remove() = host.remove(view)
}
