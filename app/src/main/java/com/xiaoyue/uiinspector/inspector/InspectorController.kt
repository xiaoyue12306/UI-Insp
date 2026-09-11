package com.xiaoyue.uiinspector.inspector

import android.accessibilityservice.AccessibilityService
import android.widget.Toast
import android.util.Log
import com.xiaoyue.uiinspector.BuildConfig
import com.xiaoyue.uiinspector.overlay.FloatingInspectorOverlay
import com.xiaoyue.uiinspector.overlay.OverlayController
import com.xiaoyue.uiinspector.overlay.TouchCaptureOverlay
import kotlinx.coroutines.*

class InspectorController(private val service: AccessibilityService) {
    val mode = InspectModeManager()
    private val overlays = OverlayController(service)
    private var floating: FloatingInspectorOverlay? = null
    private var capture: TouchCaptureOverlay? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var tree: NodeTree? = null
    private var candidates: List<NodeSnapshot> = emptyList()
    fun start() {
        stop()
        floating = FloatingInspectorOverlay(service, overlays, ::select, ::stop)
        if (floating?.show() == true) mode.state.value = InspectorState.Idle
    }
    fun select() {
        job?.cancel(); overlays.clear(); tree = null; candidates = emptyList()
        mode.state.value = InspectorState.Selecting
        capture = TouchCaptureOverlay(service, overlays, ::pick)
        if (capture?.show() != true) { stop(); return }
        floating?.view?.text = "Cancel"
        floating?.show()
        Toast.makeText(service, "Tap an element. Long press the floating button to stop.", Toast.LENGTH_SHORT).show()
        job = scope.launch { delay(30_000); if (mode.state.value is InspectorState.Selecting) start() }
    }
    private fun pick(x: Int, y: Int) {
        job?.cancel(); capture?.remove(); capture = null
        job = scope.launch {
            delay(80) // Let the touch-capture window detach before querying target windows.
            val found = withContext(Dispatchers.Default) { NodeTreeBuilder(service).at(x, y) }
            tree = found
            candidates = NodeFinder.candidates(found?.nodes.orEmpty(), x, y)
            val node = candidates.firstOrNull()
            if (node == null) { start(); Toast.makeText(service, "No accessible element here", Toast.LENGTH_SHORT).show(); return@launch }
            mode.state.value = InspectorState.Selected(node)
            floating?.view?.text = "Selected"
            if (BuildConfig.DEBUG) Log.d("UIInspector.Node", "Selected class=${node.className} bounds=${node.bounds} idPresent=${node.resourceId != null}; text omitted for privacy")
            Toast.makeText(service, "${node.className}\n${node.bounds}", Toast.LENGTH_SHORT).show()
        }
    }
    fun stop() { job?.cancel(); overlays.clear(); floating = null; capture = null; tree = null; candidates = emptyList(); mode.state.value = InspectorState.Stopped }
    fun destroy() { stop(); scope.cancel() }
}
