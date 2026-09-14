package com.xiaoyue.uiinspector.analysis

import com.xiaoyue.uiinspector.color.ColorResult
import com.xiaoyue.uiinspector.inspector.NodeSnapshot
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.util.Bounds

/** PNG owns no native resources; retained only for the current selection/frozen snapshot. */
class CapturedItemImage(bytes: ByteArray) {
    private val png = bytes.copyOf()
    fun copyPng() = png.copyOf()
}
data class ItemColorAnalysis(val colors: ColorResult?, val status: String, val screenshot: CapturedItemImage? = null)
data class FloatBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)
data class SelectedItemAnalysis(
    val node: NodeSnapshot, val boundsPx: Bounds, val boundsDp: FloatBounds,
    val width: DimensionValue, val height: DimensionValue,
    val x: DimensionValue, val y: DimensionValue,
    val neighbors: Map<Direction, NeighborMeasurement>,
    val windowEdges: Map<Direction, DimensionValue>,
    val renderedColor: ItemColorAnalysis? = null,
    val pair: PairMeasurement? = null, val frozen: Boolean = false, val stale: Boolean = false,
    val notice: String = ""
)

/** Prevents late color results from a superseded or frozen selection being committed. */
class SelectionGeneration {
    private var value = 0L
    fun next(): Long = ++value
    fun accepts(token: Long) = token == value
}
