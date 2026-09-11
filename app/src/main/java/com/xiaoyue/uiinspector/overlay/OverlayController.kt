package com.xiaoyue.uiinspector.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/** Owns window attachment; all calls are made on the service main thread. */
class OverlayController(private val service: AccessibilityService) {
    val manager = service.getSystemService(WindowManager::class.java)
    private val attached = linkedSetOf<View>()
    fun params(width: Int, height: Int, touchable: Boolean = true) = WindowManager.LayoutParams(
        width, height, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE), PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        setFitInsetsTypes(0)
    }
    fun add(view: View, params: WindowManager.LayoutParams): Boolean = try {
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        manager.addView(view, params); attached.add(view); true
    } catch (e: RuntimeException) { Log.e("UIInspector.Overlay", "Unable to attach overlay", e); false }
    fun update(view: View, params: WindowManager.LayoutParams) {
        if (view in attached) try { manager.updateViewLayout(view, params) }
        catch (e: RuntimeException) { Log.w("UIInspector.Overlay", "Unable to update overlay", e) }
    }
    fun remove(view: View?) {
        if (view != null && attached.remove(view)) try { manager.removeViewImmediate(view) }
        catch (e: RuntimeException) { Log.w("UIInspector.Overlay", "Unable to remove overlay", e) }
    }
    fun hideAll() { attached.forEach { it.visibility = View.INVISIBLE } }
    fun showAll() { attached.forEach { it.visibility = View.VISIBLE } }
    fun clear() { attached.toList().forEach(::remove) }
    fun dp(value: Int) = (value * service.resources.displayMetrics.density).toInt()
}
