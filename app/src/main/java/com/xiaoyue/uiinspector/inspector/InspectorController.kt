package com.xiaoyue.uiinspector.inspector

import android.accessibilityservice.AccessibilityService
import android.graphics.ColorSpace
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.xiaoyue.uiinspector.analysis.*
import com.xiaoyue.uiinspector.color.rgbHex
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.overlay.*
import com.xiaoyue.uiinspector.screenshot.*
import com.xiaoyue.uiinspector.util.*
import kotlinx.coroutines.*

/** Coordinates selection and lifecycle only. Geometry/color work lives in SelectedItemAnalyzer. */
class InspectorController(private val service: AccessibilityService) {
    val mode = InspectModeManager()
    private val overlays = OverlayController(service)
    private val panel = InspectorPanelOverlay(service, overlays)
    private val screenshots = ScreenshotProvider(service)
    private val colorCapture = ItemColorCapture(screenshots, overlays::hideAll, overlays::showAll)
    private val analyzer = SelectedItemAnalyzer(colorCapture::capture)
    private val generation = SelectionGeneration()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error ->
        Log.e("UIInspector", "Inspection operation failed", error)
        stop(); toast("Inspector operation failed; start again")
    })
    private var selectionJob: Job? = null
    private var colorJob: Job? = null
    private var invalidationJob: Job? = null
    private var floating: FloatingInspectorOverlay? = null
    private var capture: TouchCaptureOverlay? = null
    private var measurement: MeasurementOverlay? = null
    private var tree: NodeTree? = null
    private var candidates: List<NodeSnapshot> = emptyList()
    private var details = false
    private var advanced = false

    private fun current() = (mode.state.value as? InspectorState.Selected)?.analysis
    private fun toast(message: String) { Toast.makeText(service, message, Toast.LENGTH_SHORT).show() }
    fun start() {
        stop()
        floating = FloatingInspectorOverlay(service, overlays, ::select, ::stop, ::pair, ::picker)
        if (floating?.show() == true) mode.state.value = InspectorState.Idle
    }
    fun select() {
        if (mode.state.value is InspectorState.Selecting) { start(); return }
        beginSelection(SelectionPurpose.ITEM)
    }
    private fun pair() {
        val a = current()?.takeIf { !it.stale && !it.frozen }
        beginSelection(if (a == null) SelectionPurpose.PAIR_A else SelectionPurpose.PAIR_B, a)
    }
    private fun picker() = beginSelection(SelectionPurpose.PIXEL)

    private fun beginSelection(purpose: SelectionPurpose, anchor: SelectedItemAnalysis? = null) {
        generation.next(); selectionJob?.cancel(); colorJob?.cancel(); invalidationJob?.cancel()
        overlays.clear(); panel.remove(); measurement = null; details = false; advanced = false
        mode.state.value = InspectorState.Selecting(purpose, anchor)
        capture = TouchCaptureOverlay(service, overlays, ::pick)
        if (capture?.show() != true) { stop(); return }
        floating?.label(when (purpose) { SelectionPurpose.PAIR_A -> "Pick A"; SelectionPurpose.PAIR_B -> "Pick B"; SelectionPurpose.PIXEL -> "Pixel"; else -> "Cancel" })
        floating?.show()
        toast(when (purpose) { SelectionPurpose.PAIR_A -> "Select item A"; SelectionPurpose.PAIR_B -> "Select item B"; SelectionPurpose.PIXEL -> "Tap any visible pixel"; else -> "Tap an item for size, spacing and color" })
        selectionJob = scope.launch { delay(30_000); if (mode.state.value is InspectorState.Selecting) start() }
    }

    private fun pick(x: Int, y: Int) {
        val selecting = mode.state.value as? InspectorState.Selecting ?: return
        selectionJob?.cancel(); capture?.remove(); capture = null
        val token = generation.next()
        selectionJob = scope.launch {
            delay(100)
            val found = withContext(Dispatchers.Default) { NodeTreeBuilder(service).at(x, y) }
            if (!generation.accepts(token)) return@launch
            if (selecting.purpose == SelectionPurpose.PIXEL) { samplePixel(x, y, found, token); return@launch }
            tree = found
            candidates = NodeFinder.candidates(found?.nodes.orEmpty(), x, y)
            val node = candidates.firstOrNull()
            if (node == null || found == null) { start(); toast("No accessible item here"); return@launch }
            if (selecting.purpose == SelectionPurpose.PAIR_A) {
                val a = withContext(Dispatchers.Default) { analyzer.measure(node, found) }
                beginSelection(SelectionPurpose.PAIR_B, a)
                return@launch
            }
            val anchor = selecting.anchor
            if (anchor != null && (anchor.node.windowId != node.windowId || anchor.node.density != node.density)) {
                start(); toast("A and B must belong to the same unchanged window and display density"); return@launch
            }
            // Refresh A's geometry in B's snapshot to avoid silently measuring against an old frame.
            if (anchor != null && found.nodes.none { it.bounds == anchor.boundsPx && it.resourceId == anchor.node.resourceId && it.className == anchor.node.className }) {
                start(); toast("Item A changed; select A and B again"); return@launch
            }
            choose(node, anchor?.boundsPx)
        }
    }

    private fun choose(node: NodeSnapshot, anchor: Bounds? = null) {
        val snapshot = tree ?: return
        colorJob?.cancel(); invalidationJob?.cancel()
        val token = generation.next()
        val measured = analyzer.measure(node, snapshot).let {
            if (anchor == null) it else it.copy(pair = SpacingCalculator.between(anchor, node.bounds, node.density))
        }
        mode.state.value = InspectorState.Selected(measured)
        floating?.label("Select")
        render()
        startColor(measured, snapshot, token)
    }

    private fun startColor(a: SelectedItemAnalysis, snapshot: NodeTree, token: Long) {
        colorJob = scope.launch {
            val completed = analyzer.color(a, snapshot)
            // Cancellation plus generation prevent old A/B results from overwriting C, even after Freeze.
            if (generation.accepts(token) && current()?.node == a.node && current()?.frozen == false) {
                mode.state.value = InspectorState.Selected(completed)
                render()
            }
        }
    }
    private fun refreshColor() {
        val a = current()?.takeIf { !it.stale && !it.frozen } ?: return
        val snapshot = tree ?: return
        colorJob?.cancel(); val token = generation.next()
        val pending = a.copy(renderedColor = null)
        mode.state.value = InspectorState.Selected(pending); render()
        startColor(pending, snapshot, token)
    }
    private fun freeze() {
        val a = current() ?: return
        if (a.frozen) { beginSelection(SelectionPurpose.ITEM); return }
        if (a.stale || a.renderedColor == null) return
        generation.next(); colorJob?.cancel(); invalidationJob?.cancel()
        mode.state.value = InspectorState.Selected(a.copy(frozen = true, notice = "Frozen snapshot — geometry and captured item pixels do not follow live UI"))
        render()
    }

    private fun render() {
        val a = current() ?: return
        val node = a.node
        val nodes = tree?.nodes.orEmpty()
        val index = candidates.indexOfFirst { it.index == node.index }
        val position = if (index >= 0) "Candidate ${index + 1} / ${candidates.size}" else "Tree node"
        fun action(target: NodeSnapshot?): (() -> Unit)? = if (a.stale || a.frozen) null else target?.let { { choose(it) } }
        val actions = mutableMapOf<String, (() -> Unit)?>(
            "details" to { details = !details; render() }, "advanced" to { advanced = !advanced; render() },
            "freeze" to if (a.frozen || (!a.stale && a.renderedColor != null)) ::freeze else null,
            "summary" to { copyText(service, "Measurement summary", a.summary()) },
            "refresh" to if (!a.frozen && !a.stale) ::refreshColor else null,
            "parent" to action(nodes.firstOrNull { it.index == node.parentIndex }),
            "child" to action(nodes.firstOrNull { it.parentIndex == node.index }),
            "previous" to action(candidates.getOrNull(index - 1)), "next" to action(candidates.getOrNull(index + 1)),
            "id" to node.resourceId?.let { id -> { copyText(service, "Resource ID", id) } },
            "bounds" to { copyText(service, "Bounds", node.bounds.toString()) },
            "appium" to { copyText(service, "Appium Python", LocatorUtils.appium(node)) }
        )
        a.neighbors.forEach { (direction, neighbor) -> actions["neighbor:${direction.name}"] = action(neighbor.node) }
        overlays.remove(measurement)
        if (!panel.show(a, details, advanced, position, actions)) { stop(); toast("Unable to show measurement card"); return }
        measurement = if (!a.stale) MeasurementOverlay(service, overlays, a, panel.screenBounds).also { it.show() } else null
    }

    private suspend fun samplePixel(x: Int, y: Int, target: NodeTree?, token: Long) {
        val screen = overlays.manager.maximumWindowMetrics.bounds
        val point = Bounds(x, y, x + 1, y + 1)
        val shot = screenshots.capture(target?.windowId, target?.windowBounds ?: Bounds(screen.left, screen.top, screen.right, screen.bottom), point, overlays::hideAll, overlays::showAll)
        var color: Int? = null
        val message = when (shot) {
            is ScreenshotResult.Unavailable -> shot.reason
            is ScreenshotResult.Success -> try {
                color = withContext(Dispatchers.Default) { shot.bitmap.getColor(0, 0).convert(ColorSpace.get(ColorSpace.Named.SRGB)).toArgb() }
                "${rgbHex(color)}\n${rgbDescription(color)}\nScreen pixel ($x, $y)"
            } finally { shot.bitmap.recycle() }
        }
        if (!generation.accepts(token)) return
        mode.state.value = InspectorState.PickedColor(x, y, color, message)
        floating?.label("Select")
        panel.showPicker(message, buildMap {
            if (color != null) put("Copy color") { copyText(service, "Pixel color", message) }
            put("Pick again", ::picker); put("Close", ::start)
        })
    }

    fun onEvent(event: AccessibilityEvent) {
        val a = current() ?: return
        if (a.frozen || a.stale || event.packageName?.toString() == service.packageName) return
        val node = a.node
        val changed = event.windowId == node.windowId && (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED || (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            event.windowChanges and (AccessibilityEvent.WINDOWS_CHANGE_BOUNDS or AccessibilityEvent.WINDOWS_CHANGE_REMOVED) != 0))
        val switched = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.packageName != null && event.packageName.toString() != node.packageName
        if (!changed && !switched) return
        invalidationJob?.cancel()
        invalidationJob = scope.launch {
            delay(250)
            val current = current()?.takeIf { !it.frozen } ?: return@launch
            generation.next(); colorJob?.cancel()
            mode.state.value = InspectorState.Selected(current.copy(stale = true, notice = "Target changed. Select again for current measurements."))
            render()
        }
    }
    fun configurationChanged() {
        val a = current()
        if (a?.frozen == true) {
            // Frozen coordinates remain historical; don't draw them over a different display geometry.
            mode.state.value = InspectorState.Selected(a.copy(stale = true, notice = "Frozen snapshot; display changed. Coordinates belong to the captured orientation."))
            render()
        } else start()
    }
    fun stop() {
        generation.next(); selectionJob?.cancel(); colorJob?.cancel(); invalidationJob?.cancel()
        overlays.clear(); panel.remove(); floating = null; capture = null; measurement = null
        tree = null; candidates = emptyList(); details = false; advanced = false
        mode.state.value = InspectorState.Stopped
    }
    fun destroy() { stop(); scope.cancel() }
}
