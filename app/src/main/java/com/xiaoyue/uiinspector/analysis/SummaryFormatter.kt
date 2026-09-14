package com.xiaoyue.uiinspector.analysis

import com.xiaoyue.uiinspector.measurement.*

fun rgbDescription(color: Int) = "RGB(${(color ushr 16) and 255}, ${(color ushr 8) and 255}, ${color and 255})"

fun SelectedItemAnalysis.summary(): String = buildString {
    appendLine("Item: ${node.resourceId ?: node.className ?: "Item"}")
    if (frozen) appendLine("Frozen snapshot")
    if (stale) appendLine("Stale snapshot — reselect before using measurements")
    appendLine("\nSize:\n${formatDimension(width.dp)} dp × ${formatDimension(height.dp)} dp\n${formatDimension(width.px)} px × ${formatDimension(height.px)} px")
    appendLine("\nSpacing:")
    Direction.entries.forEach { direction -> appendLine("${direction.name.lowercase().replaceFirstChar { it.uppercase() }}: ${neighbors[direction]?.distance?.label() ?: "No reliable neighbor"}") }
    pair?.let { appendLine("\nA/B (${it.relation}):\nHorizontal: ${it.horizontal.label()}\nVertical: ${it.vertical.label()}") }
    val colors = renderedColor?.colors
    val mainDescription = colors?.dominantHex ?: if (colors != null) "No single main color" else renderedColor?.status ?: "Analyzing color…"
    appendLine("\nMain rendered color:\n$mainDescription")
    colors?.dominantColor?.let { appendLine(rgbDescription(it)) }
    appendLine("\nPosition:\nX: ${x.label()}\nY: ${y.label()}\nBounds: $boundsPx")
    if (notice.isNotEmpty()) appendLine(notice)
}
