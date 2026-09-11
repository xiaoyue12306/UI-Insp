package com.xiaoyue.uiinspector.inspector

import android.accessibilityservice.AccessibilityService
import android.widget.Toast
import android.util.Log
import com.xiaoyue.uiinspector.BuildConfig
import com.xiaoyue.uiinspector.overlay.FloatingInspectorOverlay
import com.xiaoyue.uiinspector.overlay.OverlayController
import com.xiaoyue.uiinspector.overlay.TouchCaptureOverlay
import com.xiaoyue.uiinspector.overlay.HighlightOverlay
import com.xiaoyue.uiinspector.overlay.InspectorPanelOverlay
import com.xiaoyue.uiinspector.screenshot.ScreenshotProvider
import com.xiaoyue.uiinspector.screenshot.ScreenshotResult
import com.xiaoyue.uiinspector.color.ColorAnalyzer
import android.graphics.ColorSpace
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
    private var highlight: HighlightOverlay? = null
    private val panel = InspectorPanelOverlay(service, overlays)
    private var colorText = "Screenshot analysis pending"
    private var notice = ""
    private val screenshots = ScreenshotProvider(service)
    private var colorJob: Job? = null
    fun start() {
        stop()
        floating = FloatingInspectorOverlay(service, overlays, ::select, ::stop)
        if (floating?.show() == true) mode.state.value = InspectorState.Idle
    }
    fun select() {
        if (mode.state.value is InspectorState.Selecting) { start(); return }
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
            notice = if (found?.truncated == true) "Tree truncated to protect performance" else ""
            showSelected(node)
            refreshColor()
        }
    }
    private fun refreshColor() {
        val node = (mode.state.value as? InspectorState.Selected)?.node ?: return
        val currentTree = tree ?: return
        colorJob?.cancel()
        colorJob = scope.launch {
            colorText = "Analyzing…"; showSelected(node)
            val result = screenshots.capture(node.windowId, currentTree.windowBounds, node.bounds, overlays::hideAll, overlays::showAll)
            colorText = when (result) {
                is ScreenshotResult.Unavailable -> result.reason
                is ScreenshotResult.Success -> try {
                    val colors = withContext(Dispatchers.Default) {
                        val srgb = ColorSpace.get(ColorSpace.Named.SRGB)
                        ColorAnalyzer().analyze(result.bitmap.width, result.bitmap.height, result.centerX, result.centerY) { x, y ->
                            result.bitmap.getColor(x, y).convert(srgb).toArgb()
                        }
                    }
                    "${result.source}\n${colors.description()}"
                } finally { result.bitmap.recycle() }
            }
            if ((mode.state.value as? InspectorState.Selected)?.node == node) showSelected(node)
        }
    }
    private fun showSelected(node: NodeSnapshot) {
        mode.state.value = InspectorState.Selected(node)
        overlays.remove(highlight)
        highlight = HighlightOverlay(service, overlays, node).also { it.show() }
        val position = candidates.indexOfFirst { it.index == node.index }.let { if (it >= 0) "${it + 1} / ${candidates.size}" else "Tree node" }
        panel.show(node, position, colorText, notice, listOf("Inspect" to ::select, "Close" to ::start, "Stop" to ::stop))
    }
    fun stop() { job?.cancel(); colorJob?.cancel(); overlays.clear(); panel.remove(); highlight = null; floating = null; capture = null; tree = null; candidates = emptyList(); mode.state.value = InspectorState.Stopped }
    fun destroy() { stop(); scope.cancel() }
}
