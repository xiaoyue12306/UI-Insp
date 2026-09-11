package com.xiaoyue.uiinspector.inspector
import com.xiaoyue.uiinspector.util.Bounds
import com.xiaoyue.uiinspector.util.pxToDp

data class NodeSnapshot(
    val index: Int, val parentIndex: Int?, val depth: Int,
    val packageName: String?, val resourceId: String?, val className: String?,
    val text: String?, val contentDescription: String?, val bounds: Bounds,
    val density: Float, val clickable: Boolean, val enabled: Boolean,
    val focusable: Boolean, val focused: Boolean, val selected: Boolean,
    val checkable: Boolean, val checked: Boolean, val scrollable: Boolean,
    val editable: Boolean, val visibleToUser: Boolean, val password: Boolean,
    val actions: List<String>, val windowId: Int, val childCount: Int
) {
    val x get() = bounds.left
    val y get() = bounds.top
    val widthPx get() = bounds.width
    val heightPx get() = bounds.height
    val widthDp get() = pxToDp(widthPx, density)
    val heightDp get() = pxToDp(heightPx, density)
}
data class NodeTree(val nodes: List<NodeSnapshot>, val windowId: Int, val windowBounds: Bounds, val truncated: Boolean)
