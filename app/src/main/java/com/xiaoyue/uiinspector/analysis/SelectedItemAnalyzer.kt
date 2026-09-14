package com.xiaoyue.uiinspector.analysis

import com.xiaoyue.uiinspector.inspector.NodeSnapshot
import com.xiaoyue.uiinspector.inspector.NodeTree
import com.xiaoyue.uiinspector.measurement.*

class SelectedItemAnalyzer(
    private val captureColor: suspend (NodeSnapshot, NodeTree) -> ItemColorAnalysis,
    private val neighborFinder: NeighborFinder = NeighborFinder()
) {
    fun measure(node: NodeSnapshot, tree: NodeTree): SelectedItemAnalysis {
        val b = node.bounds
        fun dimension(value: Int) = DimensionValue.fromPx(value.toFloat(), node.density)
        return SelectedItemAnalysis(node, b, FloatBounds(b.left / node.density, b.top / node.density, b.right / node.density, b.bottom / node.density),
            dimension(b.width), dimension(b.height), dimension(b.left), dimension(b.top),
            neighborFinder.find(node, tree.nodes, tree.windowBounds), SpacingCalculator.windowEdges(b, tree.windowBounds, node.density),
            notice = if (tree.truncated) "Partial accessibility tree: some neighbors may be missing" else "")
    }
    suspend fun color(analysis: SelectedItemAnalysis, tree: NodeTree) =
        analysis.copy(renderedColor = captureColor(analysis.node, tree))
}
