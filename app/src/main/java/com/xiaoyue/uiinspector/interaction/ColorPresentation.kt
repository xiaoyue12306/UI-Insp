package com.xiaoyue.uiinspector.interaction
import com.xiaoyue.uiinspector.analysis.ItemColorAnalysis
fun ItemColorAnalysis?.colorMessage(): String = when {
    this == null -> "Analyzing…"
    colors != null && colors.dominantColor == null -> "Mixed colors"
    colors?.dominantHex != null -> colors.dominantHex!!
    status.contains("secure",true) || status.contains("prevent",true) || status.contains("blocked",true) -> "Color unavailable\nScreenshot is blocked by this app."
    else -> "Color unavailable\nTry selecting the item again."
}
