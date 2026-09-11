package com.xiaoyue.uiinspector.overlay

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import com.xiaoyue.uiinspector.inspector.NodeSnapshot
import com.xiaoyue.uiinspector.util.formatDp

/** Native overlay avoids manufacturing Compose lifecycle owners inside a service. Home uses Compose. */
class InspectorPanelOverlay(private val context: Context, private val host: OverlayController) {
    private var panel: ScrollView? = null
    fun show(node: NodeSnapshot, position: String, colorText: String, notice: String,
             actions: List<Pair<String, (() -> Unit)?>>) {
        host.remove(panel)
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(host.dp(16), host.dp(12), host.dp(16), host.dp(16))
            setBackgroundColor(0xfff4f7fb.toInt())
        }
        fun text(value: String, size: Float = 14f) { column.addView(TextView(context).apply {
            text = value; textSize = size; setTextColor(0xff142c40.toInt()); setPadding(0, host.dp(4), 0, host.dp(4)); setTextIsSelectable(true)
        }) }
        text("${node.className?.substringAfterLast('.') ?: "Element"}    $position", 20f)
        if (notice.isNotBlank()) text(notice)
        // Keep navigation and dismissal reachable even with very long properties.
        actions.chunked(3).forEach { group ->
            val row = LinearLayout(context)
            group.forEach { (label, action) -> row.addView(Button(context).apply {
                text = label; textSize = 11f; isAllCaps = false; isEnabled = action != null
                setOnClickListener { action?.invoke() }
            }, LinearLayout.LayoutParams(0, host.dp(48), 1f)) }
            column.addView(row)
        }
        text("Package\n${node.packageName ?: "—"}")
        text("Resource ID\n${node.resourceId ?: "—"}")
        text("Class\n${node.className ?: "—"}")
        text("Text\n${node.text ?: "—"}")
        text("Content Description\n${node.contentDescription ?: "—"}")
        text("POSITION", 16f)
        text("X ${node.x} px    Y ${node.y} px\n${node.widthPx} × ${node.heightPx} px\n${formatDp(node.widthDp)} × ${formatDp(node.heightDp)} dp\nBounds ${node.bounds}\nWindow ${node.windowId} · Depth ${node.depth} · Children ${node.childCount}")
        text("STATE", 16f)
        text("clickable ${node.clickable}\nenabled ${node.enabled}\nfocusable ${node.focusable}\nfocused ${node.focused}\nselected ${node.selected}\ncheckable ${node.checkable}\nchecked ${node.checked}\nscrollable ${node.scrollable}\neditable ${node.editable}\nvisibleToUser ${node.visibleToUser}\npassword ${node.password}")
        text("Accessibility actions\n${node.actions.joinToString("\n").ifEmpty { "None" }}")
        text("RENDERED COLOR", 16f); text(colorText)
        text("Visual estimate from pixels, not the source background property.", 12f)
        val b = host.manager.currentWindowMetrics.bounds
        val tablet = b.width() / context.resources.displayMetrics.density >= 600
        val width = if (tablet) host.dp(380).coerceAtMost(b.width()) else b.width()
        val height = if (tablet) (b.height() * .80f).toInt() else (b.height() * .52f).toInt()
        panel = ScrollView(context).apply { addView(column); elevation = host.dp(8).toFloat() }
        val params = host.params(width, height).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = if (tablet) b.width() - width else 0
            y = if (tablet) host.dp(32) else b.height() - height - host.dp(24)
        }
        host.add(panel!!, params)
    }
    fun remove() { host.remove(panel); panel = null }
}
