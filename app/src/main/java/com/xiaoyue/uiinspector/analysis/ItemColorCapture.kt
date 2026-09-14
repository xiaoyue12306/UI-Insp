package com.xiaoyue.uiinspector.analysis

import android.graphics.Bitmap
import android.graphics.ColorSpace
import com.xiaoyue.uiinspector.color.ColorAnalyzer
import com.xiaoyue.uiinspector.inspector.NodeSnapshot
import com.xiaoyue.uiinspector.inspector.NodeTree
import com.xiaoyue.uiinspector.screenshot.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ItemColorCapture(private val screenshots: ScreenshotProvider, private val hide: () -> Unit, private val restore: () -> Unit) {
    suspend fun capture(node: NodeSnapshot, tree: NodeTree): ItemColorAnalysis = when (val shot = screenshots.capture(node.windowId, tree.windowBounds, node.bounds, hide, restore)) {
        is ScreenshotResult.Unavailable -> ItemColorAnalysis(null, shot.reason)
        is ScreenshotResult.Success -> try {
            withContext(Dispatchers.Default) {
                val srgb = ColorSpace.get(ColorSpace.Named.SRGB)
                val colors = ColorAnalyzer().analyze(shot.bitmap.width, shot.bitmap.height, shot.centerX, shot.centerY) { x, y -> shot.bitmap.getColor(x, y).convert(srgb).toArgb() }
                val png = ByteArrayOutputStream().use { bytes ->
                    shot.bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                    CapturedItemImage(bytes.toByteArray())
                }
                ItemColorAnalysis(colors, shot.source, png)
            }
        } finally { shot.bitmap.recycle() }
    }
}
