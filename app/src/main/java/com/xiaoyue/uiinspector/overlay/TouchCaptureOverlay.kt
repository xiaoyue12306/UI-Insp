package com.xiaoyue.uiinspector.overlay
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
class TouchCaptureOverlay(context: Context, private val host: OverlayController, picked: (Int, Int) -> Unit) {
    val view = View(context).apply {
        setBackgroundColor(0x142196f3)
        setOnTouchListener { _, event ->
            // rawX/rawY are screen coordinates, unlike coordinates relative to this overlay.
            if (event.actionMasked == MotionEvent.ACTION_DOWN) picked(event.rawX.toInt(), event.rawY.toInt())
            true
        }
    }
    fun show() = host.add(view, host.params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT))
    fun remove() = host.remove(view)
}
