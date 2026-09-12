package com.xiaoyue.uiinspector

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.view.View
import com.xiaoyue.uiinspector.accessibility.AccessibilityServiceState
import com.xiaoyue.uiinspector.color.ColorAnalyzer
import com.xiaoyue.uiinspector.overlay.OverlayController
import com.xiaoyue.uiinspector.screenshot.*
import com.xiaoyue.uiinspector.util.Bounds
import kotlinx.coroutines.*
import java.io.File

/** Runs in the already-authorized service process. Instrumentation would force-stop that process. */
class ScreenshotValidationActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = TextView(this).apply { text = "Running screenshot validation…" }
        setContentView(status)
        val report = File(filesDir, "screenshot-validation.txt")
        report.writeText("RUNNING\n")
        scope.launch {
            val results = mutableListOf<String>()
            val checks: List<Pair<String, suspend () -> Unit>> = listOf(
                "window excludes overlay" to { if (Build.VERSION.SDK_INT >= 34) assertBackground(capture(false, false, true), "Window") else error("API 34+ required") },
                "display hides overlay" to { assertBackground(capture(false, true, true), "Display") },
                "secure window rejected" to {
                    val result = capture(true, false, false)
                    if (result is ScreenshotResult.Success) { result.bitmap.recycle(); error("Secure window must not be captured") }
                    check(result is ScreenshotResult.Unavailable && result.reason.contains("prevents screenshots")) { "Unexpected result: $result" }
                }
            )
            for ((name, test) in checks) {
                val result = try { test(); "PASS $name" } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { Log.e("UIInspector.Validation", name, e); "FAIL $name: ${e.message}" }
                results.add(result); Log.i("UIInspector.Validation", result)
                report.writeText(results.joinToString("\n") + "\nRUNNING\n")
            }
            val summary = "${results.count { it.startsWith("PASS") }} / ${checks.size} passed"
            report.writeText(results.joinToString("\n") + "\n$summary\nDONE\n")
            status.text = summary
            Log.i("UIInspector.Validation", summary)
            finish()
        }
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private suspend fun capture(secure: Boolean, display: Boolean, obstruct: Boolean): ScreenshotResult {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName("com.xiaoyue.inspectorfixture", "com.xiaoyue.inspectorfixture.FixtureActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("secure", secure))
        delay(1500)
        val service = AccessibilityServiceState.service ?: error("Manually enable UI Inspector accessibility service first")
        return withContext(Dispatchers.Main) {
            service.stopInspector()
            val windows = service.windows
            var windowId: Int? = null; var windowBounds: Bounds? = null; var nodeBounds: Bounds? = null
            @Suppress("DEPRECATION")
            for (window in windows) {
                val root = window.root
                try {
                    if (root?.packageName?.toString() != "com.xiaoyue.inspectorfixture") continue
                    val nodes = root.findAccessibilityNodeInfosByViewId("com.xiaoyue.inspectorfixture:id/color_button")
                    try {
                        val button = nodes.firstOrNull() ?: continue
                        val b = Rect(); window.getBoundsInScreen(b); windowBounds = Bounds(b.left, b.top, b.right, b.bottom)
                        button.getBoundsInScreen(b); nodeBounds = Bounds(b.left, b.top, b.right, b.bottom); windowId = window.id
                    } finally { if (Build.VERSION.SDK_INT < 33) nodes.forEach { it.recycle() } }
                } finally { if (Build.VERSION.SDK_INT < 33) { root?.recycle(); window.recycle() } }
            }
            val host = OverlayController(service)
            try {
                if (obstruct) {
                    check(host.add(View(service).apply { setBackgroundColor(0xffff0000.toInt()) }, host.params(-1, -1, false)))
                    delay(150)
                }
                ScreenshotProvider(service).capture(if (display) null else windowId!!, windowBounds!!, nodeBounds!!, host::hideAll, host::showAll)
            } finally { host.clear() }
        }
    }
    private fun assertBackground(result: ScreenshotResult, source: String) {
        check(result is ScreenshotResult.Success) { "Expected screenshot, got $result" }
        val image = result as ScreenshotResult.Success
        try {
            check(image.source.startsWith(source)) { "Expected $source, got ${image.source}" }
            val colors = ColorAnalyzer().analyze(image.bitmap.width, image.bitmap.height, image.centerX, image.centerY, image.bitmap::getPixel)
            val actual = colors.dominantColor ?: error("Missing dominant color")
            for (shift in listOf(16, 8, 0)) check(kotlin.math.abs(((actual ushr shift) and 255) - ((0xc7c6ca ushr shift) and 255)) <= 5) { "Unexpected dominant ${colors.dominantHex}" }
            Log.i("UIInspector.Validation", "$source dominant=${colors.dominantHex} center=${colors.centerHex}")
        } finally { image.bitmap.recycle() }
    }
}
