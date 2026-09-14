package com.xiaoyue.uiinspector

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.xiaoyue.uiinspector.accessibility.AccessibilityServiceState
import com.xiaoyue.uiinspector.analysis.*
import com.xiaoyue.uiinspector.inspector.NodeTreeBuilder
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.overlay.OverlayController
import com.xiaoyue.uiinspector.screenshot.ScreenshotProvider
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.abs

/** Optional deterministic fixture verification, in the already-authorized service process. */
class MeasurementValidationActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = File(filesDir, "measurement-validation.txt")
        report.writeText("RUNNING\n")
        scope.launch {
            try {
                val service = withTimeout(10_000) {
                    while (AccessibilityServiceState.service == null) delay(100)
                    requireNotNull(AccessibilityServiceState.service)
                }
                service.stopInspector()
                startActivity(Intent().setClassName("com.xiaoyue.inspectorfixture", "com.xiaoyue.inspectorfixture.FixtureActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("measurement", true))
                delay(1500)
                var point: Rect? = null
                @Suppress("DEPRECATION")
                for (window in service.windows) {
                    val root = window.root
                    try {
                        if (root?.packageName?.toString() != "com.xiaoyue.inspectorfixture") continue
                        val matches = root.findAccessibilityNodeInfosByViewId("com.xiaoyue.inspectorfixture:id/color_button")
                        try { matches.firstOrNull()?.let { node -> point = Rect().also(node::getBoundsInScreen) } }
                        finally { if (Build.VERSION.SDK_INT < 33) matches.forEach { it.recycle() } }
                    } finally { if (Build.VERSION.SDK_INT < 33) { root?.recycle(); window.recycle() } }
                }
                val p = point ?: error("Fixture button not exposed")
                val snapshot = withContext(Dispatchers.Default) { NodeTreeBuilder(service).at(p.centerX(), p.centerY()) } ?: error("No node tree")
                val node = snapshot.nodes.first { it.resourceId?.endsWith(":id/color_button") == true }
                val host = OverlayController(service)
                val capture = ItemColorCapture(ScreenshotProvider(service), host::hideAll, host::showAll)
                val analyzer = SelectedItemAnalyzer(capture::capture)
                val a = withContext(Dispatchers.Default) { analyzer.measure(node, snapshot) }
                check(abs(a.width.dp - 328f) <= 1 / node.density && abs(a.height.dp - 48f) <= 1 / node.density) { "Unexpected size ${a.width} ${a.height}" }
                for (direction in Direction.entries) {
                    val n = a.neighbors[direction] ?: error("Missing $direction")
                    check(n.node.resourceId?.endsWith(":id/${direction.name.lowercase()}_neighbor") == true) { "Wrong $direction ${n.node.resourceId}" }
                    val expected = if (direction == Direction.TOP || direction == Direction.BOTTOM) 24f else 16f
                    check(abs(n.distance.dp - expected) <= 2 / node.density) { "Wrong gap ${n.distance}" }
                }
                val complete = analyzer.color(a, snapshot)
                check(complete.renderedColor?.colors?.dominantHex == "#C7C6CA") { "Wrong color ${complete.renderedColor}" }
                check(complete.renderedColor.screenshot?.copyPng()?.size ?: 0 > 0) { "Missing frozen image" }
                report.writeText("PASS bounds and dual units\nPASS all four reliable neighbors and gaps\nPASS exact rendered color and retained PNG\n${complete.summary()}\n3 / 3 passed\nDONE\n")
                service.startInspector()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { report.writeText("FAIL ${e.message}\nDONE\n"); Log.e("UIInspector.Validation", "Measurement", e) }
            finish()
        }
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
