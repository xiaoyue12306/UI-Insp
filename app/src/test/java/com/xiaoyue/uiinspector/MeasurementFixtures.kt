package com.xiaoyue.uiinspector

import com.xiaoyue.uiinspector.inspector.*
import com.xiaoyue.uiinspector.util.Bounds

internal fun item(index: Int = 0, bounds: Bounds = Bounds(100, 100, 300, 196)) = NodeSnapshot(
    index, null, 1, "test.app", "test:id/item$index", "android.widget.Button", "Item $index", null, bounds, 2f,
    true, true, true, false, false, false, false, false, false, true, false, emptyList(), 1, 0)
internal fun tree(vararg nodes: NodeSnapshot) = NodeTree(nodes.toList(), 1, Bounds(0, 0, 1000, 1000), false)
