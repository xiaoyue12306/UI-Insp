package com.xiaoyue.uiinspector.screenshot
import android.graphics.Bitmap
sealed interface ScreenshotResult {
    /** Caller owns and must recycle bitmap. It contains only the clipped element region. */
    data class Success(val bitmap: Bitmap, val centerX: Int?, val centerY: Int?, val source: String) : ScreenshotResult
    data class Unavailable(val reason: String) : ScreenshotResult
}
