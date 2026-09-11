package com.xiaoyue.uiinspector.screenshot

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.WindowManager
import com.xiaoyue.uiinspector.util.Bounds
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class ScreenshotProvider(private val service: AccessibilityService) {
    private val mutex = Mutex()
    private var lastRequest = 0L
    private sealed interface Raw {
        data class Image(val value: AccessibilityService.ScreenshotResult) : Raw
        data class Error(val code: Int) : Raw
    }
    private suspend fun request(windowId: Int?): Raw {
        delay((350L - (SystemClock.elapsedRealtime() - lastRequest)).coerceAtLeast(0L))
        lastRequest = SystemClock.elapsedRealtime()
        return withTimeoutOrNull(4000) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                        continuation.resume(Raw.Image(screenshot)) { _, value, _ ->
                            (value as? Raw.Image)?.value?.hardwareBuffer?.close()
                        }
                    }
                    override fun onFailure(errorCode: Int) { if (continuation.isActive) continuation.resume(Raw.Error(errorCode)) }
                }
                try {
                    if (Build.VERSION.SDK_INT >= 34 && windowId != null) {
                        service.takeScreenshotOfWindow(windowId, service.mainExecutor, callback)
                    } else service.takeScreenshot(Display.DEFAULT_DISPLAY, service.mainExecutor, callback)
                } catch (e: RuntimeException) {
                    Log.w("UIInspector.Screenshot", "Screenshot request rejected", e)
                    if (continuation.isActive) continuation.resume(Raw.Error(-1))
                }
            }
        } ?: Raw.Error(-2)
    }
    /** Hide overlays before display capture; secure-window failures are never bypassed. */
    suspend fun capture(windowId: Int?, windowBounds: Bounds, nodeBounds: Bounds,
                        hide: () -> Unit, restore: () -> Unit): ScreenshotResult = mutex.withLock {
        var windowCapture = Build.VERSION.SDK_INT >= 34 && windowId != null
        var hidden = false
        try {
            if (!windowCapture) { hide(); hidden = true; delay(120) }
            var raw = request(if (windowCapture) windowId else null)
            if (raw is Raw.Error && raw.code == AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT) {
                delay(400); raw = request(if (windowCapture) windowId else null)
            }
            if (raw is Raw.Error && windowCapture && raw.code != AccessibilityService.ERROR_TAKE_SCREENSHOT_SECURE_WINDOW) {
                Log.w("UIInspector.Screenshot", "Window capture failed (${raw.code}); using display fallback")
                hide(); hidden = true; delay(120); windowCapture = false
                raw = request(null)
            }
            when (raw) {
                is Raw.Error -> ScreenshotResult.Unavailable(errorMessage(raw.code))
                is Raw.Image -> {
                    // No suspension between receiving the hardware buffer and taking ownership in finally.
                    try {
                        var converted: ScreenshotResult? = null
                        try {
                            withContext(Dispatchers.Default) { converted = convert(raw.value, windowCapture, windowBounds, nodeBounds) }
                            converted!!
                        } catch (e: CancellationException) {
                            (converted as? ScreenshotResult.Success)?.bitmap?.recycle(); throw e
                        }
                    } finally { raw.value.hardwareBuffer.close() }
                }
            }
        } finally { if (hidden) restore() }
    }
    private fun convert(result: AccessibilityService.ScreenshotResult, windowCapture: Boolean,
                        window: Bounds, node: Bounds): ScreenshotResult {
        var hardware: Bitmap? = null
        var cropped: Bitmap? = null
        try {
            hardware = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                ?: return ScreenshotResult.Unavailable("Hardware buffer conversion failed")
            val display = service.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            // Only use a known 1:1 mapping. Reject unrecognized vendor buffer sizes instead of inventing coordinates.
            val origin = if (windowCapture && hardware.width == window.width && hardware.height == window.height) window
                else if (hardware.width == display.width() && hardware.height == display.height()) Bounds(display.left, display.top, display.right, display.bottom)
                else return ScreenshotResult.Unavailable("Screenshot geometry changed; select the element again")
            val clipped = node.clip(origin) ?: return ScreenshotResult.Unavailable("Element is outside screenshot")
            val x = clipped.left - origin.left; val y = clipped.top - origin.top
            cropped = Bitmap.createBitmap(hardware, x, y, clipped.width, clipped.height)
            val software = cropped.copy(Bitmap.Config.ARGB_8888, false)
                ?: return ScreenshotResult.Unavailable("Pixel conversion failed")
            val cx = node.left + node.width / 2; val cy = node.top + node.height / 2
            val centerValid = clipped.contains(cx, cy)
            return ScreenshotResult.Success(software, if (centerValid) cx - clipped.left else null,
                if (centerValid) cy - clipped.top else null, if (windowCapture) "Window screenshot" else "Display screenshot (overlays hidden)")
        } catch (e: RuntimeException) {
            Log.w("UIInspector.Screenshot", "Pixel conversion failed", e)
            return ScreenshotResult.Unavailable("Screenshot conversion failed")
        } finally { if (cropped !== hardware) cropped?.recycle(); hardware?.recycle() }
    }
    private fun errorMessage(code: Int) = when (code) {
        AccessibilityService.ERROR_TAKE_SCREENSHOT_SECURE_WINDOW -> "Color unavailable. Target window prevents screenshots."
        AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS -> "Screenshot unavailable: accessibility access disconnected"
        AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY -> "Screenshot unavailable: invalid display"
        AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_WINDOW -> "Screenshot unavailable: target window no longer exists"
        AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT -> "Screenshot unavailable: too many requests; try Refresh color"
        -2 -> "Screenshot unavailable: request timed out"
        else -> "Screenshot unavailable (error $code)"
    }
}
