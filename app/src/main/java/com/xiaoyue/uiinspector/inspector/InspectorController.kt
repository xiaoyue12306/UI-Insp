package com.xiaoyue.uiinspector.inspector

import android.accessibilityservice.AccessibilityService
import android.widget.Toast
import com.xiaoyue.uiinspector.overlay.FloatingInspectorOverlay
import com.xiaoyue.uiinspector.overlay.OverlayController

class InspectorController(private val service: AccessibilityService) {
    val mode = InspectModeManager()
    private val overlays = OverlayController(service)
    private var floating: FloatingInspectorOverlay? = null
    fun start() {
        stop()
        floating = FloatingInspectorOverlay(service, overlays, { Toast.makeText(service, "Inspector ready", Toast.LENGTH_SHORT).show() }, ::stop)
        if (floating?.show() == true) mode.state.value = InspectorState.Idle
    }
    fun stop() { overlays.clear(); floating = null; mode.state.value = InspectorState.Stopped }
}
