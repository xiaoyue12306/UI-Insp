package com.xiaoyue.uiinspector

import android.app.UiAutomation
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaoyue.uiinspector.accessibility.AccessibilityServiceState
import com.xiaoyue.uiinspector.color.ColorAnalyzer
import com.xiaoyue.uiinspector.overlay.OverlayController
import com.xiaoyue.uiinspector.screenshot.*
import com.xiaoyue.uiinspector.util.Bounds
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Requires qa-target installed and UI Inspector manually enabled before running. */
@RunWith(AndroidJUnit4::class)
class ScreenshotDeviceTest {
    private suspend fun capture(secure: Boolean, display: Boolean, obstruct: Boolean): ScreenshotResult {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName("com.xiaoyue.inspectorfixture", "com.xiaoyue.inspectorfixture.FixtureActivity")
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
                    assertTrue(host.add(View(service).apply { setBackgroundColor(0xffff0000.toInt()) }, host.params(-1, -1, false)))
                    delay(150)
                }
                ScreenshotProvider(service).capture(if (display) null else windowId!!, windowBounds!!, nodeBounds!!, host::hideAll, host::showAll)
            } finally { host.clear() }
        }
    }
    private fun assertBackground(result: ScreenshotResult, source: String) {
        assertTrue("Expected screenshot, got $result", result is ScreenshotResult.Success)
        val image = result as ScreenshotResult.Success
        try {
            assertTrue(image.source.startsWith(source))
            val colors = ColorAnalyzer().analyze(image.bitmap.width, image.bitmap.height, image.centerX, image.centerY, image.bitmap::getPixel)
            val actual = colors.dominantColor ?: error("Missing dominant color")
            for (shift in listOf(16, 8, 0)) assertTrue("Unexpected dominant ${colors.dominantHex}", kotlin.math.abs(((actual ushr shift) and 255) - ((0xc7c6ca ushr shift) and 255)) <= 5)
        } finally { image.bitmap.recycle() }
    }
    @Test fun windowCaptureExcludesAccessibilityOverlay() = runBlocking {
        if (Build.VERSION.SDK_INT >= 34) assertBackground(capture(false, false, true), "Window")
    }
    @Test fun displayCaptureHidesAccessibilityOverlay() = runBlocking { assertBackground(capture(false, true, true), "Display") }
    @Test fun secureWindowIsUnavailable() = runBlocking {
        val result = capture(true, false, false)
        if (result is ScreenshotResult.Success) { result.bitmap.recycle(); fail("Secure window must not be captured") }
        assertTrue(result is ScreenshotResult.Unavailable)
    }
}
