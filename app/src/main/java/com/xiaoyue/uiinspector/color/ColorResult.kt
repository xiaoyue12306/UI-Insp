package com.xiaoyue.uiinspector.color
import java.util.Locale
data class ColorShare(val color: Int, val fraction: Double) { val hex get() = rgbHex(color) }
data class ColorResult(val centerColor: Int?, val dominantColor: Int?, val topColors: List<ColorShare>) {
    val centerHex get() = centerColor?.let(::rgbHex)
    val dominantHex get() = dominantColor?.let(::rgbHex)
    fun description() = "Center  ${centerHex ?: "Outside visible region"}\nDominant  ${dominantHex ?: "No single dominant color"}\n" +
        topColors.joinToString("\n") { "${it.hex}  ${String.format(Locale.US, "%.1f", it.fraction * 100)}%" }
}
fun rgbHex(color: Int) = String.format(Locale.US, "#%06X", color and 0xffffff)
