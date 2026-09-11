package com.xiaoyue.uiinspector.inspector

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.xiaoyue.uiinspector.util.Bounds

class NodeTreeBuilder(private val service: AccessibilityService) {
    private fun Rect.bounds() = Bounds(left, top, right, bottom)
    @Suppress("DEPRECATION")
    private fun release(node: AccessibilityNodeInfo) { if (Build.VERSION.SDK_INT < 33) node.recycle() }
    @Suppress("DEPRECATION")
    fun at(x: Int, y: Int): NodeTree? {
        val windows = service.windows.orEmpty()
        try {
            val eligible = windows.filter { w ->
                val b = Rect(); w.getBoundsInScreen(b)
                w.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY && b.contains(x, y)
            }.sortedWith(compareByDescending<AccessibilityWindowInfo> { it.isActive }
                .thenByDescending { it.isFocused }.thenByDescending { it.layer })
            for (window in eligible) {
                val root = window.root ?: continue
                if (root.packageName?.toString() == service.packageName) { release(root); continue }
                val b = Rect(); window.getBoundsInScreen(b)
                return build(root, window.id, b.bounds())
            }
            val root = service.rootInActiveWindow ?: return null
            if (root.packageName?.toString() == service.packageName) { release(root); return null }
            val b = Rect(); root.getBoundsInScreen(b)
            return build(root, root.windowId, b.bounds())
        } catch (e: RuntimeException) {
            Log.w("UIInspector.Node", "Window became unavailable", e); return null
        } finally { if (Build.VERSION.SDK_INT < 33) windows.forEach { it.recycle() } }
    }
    private fun build(root: AccessibilityNodeInfo, windowId: Int, windowBounds: Bounds): NodeTree {
        val result = mutableListOf<NodeSnapshot>()
        var truncated = false
        val density = service.resources.displayMetrics.density
        fun visit(node: AccessibilityNodeInfo, parent: Int?, depth: Int) {
            try {
                if (result.size >= 5000 || depth > 100) { truncated = true; return }
                val b = Rect(); node.getBoundsInScreen(b)
                val index = result.size
                result.add(NodeSnapshot(index, parent, depth, node.packageName?.toString(), node.viewIdResourceName,
                    node.className?.toString(), if (node.isPassword) "[password redacted]" else node.text?.toString(),
                    if (node.isPassword) null else node.contentDescription?.toString(), b.bounds(), density,
                    node.isClickable, node.isEnabled, node.isFocusable, node.isFocused, node.isSelected,
                    node.isCheckable, node.isChecked, node.isScrollable, node.isEditable, node.isVisibleToUser,
                    node.isPassword, node.actionList.map { action -> action.label?.toString() ?: action.toString() }, windowId, node.childCount))
                for (i in 0 until node.childCount) {
                    if (result.size >= 5000) { truncated = true; break }
                    val child = node.getChild(i) ?: continue
                    visit(child, index, depth + 1)
                }
            } catch (e: RuntimeException) { Log.w("UIInspector.Node", "Stale node skipped", e) }
            finally { release(node) }
        }
        visit(root, null, 0)
        return NodeTree(result.toList(), windowId, windowBounds, truncated)
    }
}
