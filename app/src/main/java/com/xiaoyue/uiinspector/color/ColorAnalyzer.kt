package com.xiaoyue.uiinspector.color
import kotlin.math.*

data class ColorConfig(val horizontalInset: Double = .10, val verticalInset: Double = .15,
                       val quantizationStep: Int = 8, val maxSamples: Int = 4096, val dominanceThreshold: Double = .30) {
    init { require(horizontalInset in 0.0..0.4 && verticalInset in 0.0..0.4); require(quantizationStep in 1..256); require(maxSamples > 0); require(dominanceThreshold in 0.0..1.0) }
}
class ColorAnalyzer(private val config: ColorConfig = ColorConfig()) {
    fun quantize(color: Int): Int {
        fun channel(shift: Int) = (((color ushr shift) and 255) / config.quantizationStep * config.quantizationStep) shl shift
        return channel(16) or channel(8) or channel(0)
    }
    private class Bucket { var count = 0; var red = 0L; var green = 0L; var blue = 0L }
    /** Bins nearby RGB colors, then returns their actual mean (not the lower bin edge).
     * This preserves ±5 accuracy on a flat surface while reducing anti-aliasing noise. */
    fun analyze(width: Int, height: Int, centerX: Int?, centerY: Int?, pixel: (Int, Int) -> Int): ColorResult {
        if (width <= 0 || height <= 0) return ColorResult(null, null, emptyList())
        val center = if (centerX != null && centerY != null && centerX in 0 until width && centerY in 0 until height) pixel(centerX, centerY) else null
        val insetX = (width * config.horizontalInset).toInt().coerceAtMost((width - 1) / 2)
        val insetY = (height * config.verticalInset).toInt().coerceAtMost((height - 1) / 2)
        val sampleWidth = width - 2 * insetX; val sampleHeight = height - 2 * insetY
        val step = ceil(sqrt(sampleWidth.toDouble() * sampleHeight / config.maxSamples)).toInt().coerceAtLeast(1)
        val bins = mutableMapOf<Int, Bucket>()
        var samples = 0
        for (y in insetY until height - insetY step step) for (x in insetX until width - insetX step step) {
            val color = pixel(x, y)
            if ((color ushr 24) < 128) continue
            val bucket = bins.getOrPut(quantize(color)) { Bucket() }
            bucket.count++; bucket.red += (color ushr 16) and 255; bucket.green += (color ushr 8) and 255; bucket.blue += color and 255
            samples++
        }
        if (samples == 0) return ColorResult(center, null, emptyList())
        val top = bins.entries.sortedWith(compareByDescending<Map.Entry<Int, Bucket>> { it.value.count }.thenBy { it.key }).take(3).map { (_, b) ->
            val color = (0xff shl 24) or ((b.red / b.count).toInt() shl 16) or ((b.green / b.count).toInt() shl 8) or (b.blue / b.count).toInt()
            ColorShare(color, b.count.toDouble() / samples)
        }
        return ColorResult(center, top.firstOrNull()?.takeIf { it.fraction > config.dominanceThreshold }?.color, top)
    }
}
