package com.xiaoyue.uiinspector.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.widget.*
import com.xiaoyue.uiinspector.analysis.*
import com.xiaoyue.uiinspector.measurement.*
import java.util.Locale

/** Compact measurement card by default. Accessibility data is explicitly opt-in. */
class InspectorPanelOverlay(private val context: Context, private val host: OverlayController) {
    private var panel: LinearLayout? = null
    private var scroll: ScrollView? = null
    private var shownNode: com.xiaoyue.uiinspector.inspector.NodeSnapshot? = null
    private var shownDetails = false
    var screenBounds: Rect? = null
        private set

    fun show(a: SelectedItemAnalysis, details: Boolean, advanced: Boolean, candidatePosition: String,
             actions: Map<String, (() -> Unit)?>): Boolean {
        val previousScroll = if (shownNode == a.node && shownDetails == details) scroll?.scrollY ?: 0 else 0
        remove()
        shownNode = a.node; shownDetails = details
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(12), host.dp(10), host.dp(12), host.dp(10))
            setBackgroundColor(0xfff3f7fb.toInt())
            elevation = host.dp(8).toFloat()
        }
        var content = root
        fun text(value: String, size: Float = 14f) {
            content.addView(TextView(context).apply {
                text = value; textSize = size; setTextColor(0xff132d43.toInt())
                setPadding(0, host.dp(4), 0, host.dp(4)); setTextIsSelectable(true)
            })
        }
        fun buttons(items: List<Pair<String, String>>) {
            items.chunked(3).forEach { group ->
                val row = LinearLayout(context)
                group.forEach { (label, key) -> row.addView(Button(context).apply {
                    text = label; textSize = 11f; isAllCaps = false; isEnabled = actions[key] != null
                    setOnClickListener { actions[key]?.invoke() }
                }, LinearLayout.LayoutParams(0, host.dp(44), 1f)) }
                content.addView(row)
            }
        }
        text("${a.node.className?.substringAfterLast('.') ?: "Item"} · ${if (a.frozen) "Frozen" else "Measurement"}", 18f)
        buttons(listOf((if (details) "Compact" else "Details") to "details", (if (a.frozen) "Unfreeze" else "Freeze") to "freeze", "Copy" to "summary"))
        val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val scroller = ScrollView(context).apply { addView(body) }
        scroll = scroller
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        scroller.post { scroller.scrollTo(0, previousScroll) }
        content = body
        if (a.notice.isNotBlank()) text(a.notice, 12f)
        text(if (details) "MEASUREMENT / SIZE" else "${formatDimension(a.width.dp)} × ${formatDimension(a.height.dp)} dp", 18f)
        if (details) text("Width\n${a.width.lines()}\n\nHeight\n${a.height.lines()}")
        else text("${formatDimension(a.width.px)} × ${formatDimension(a.height.px)} px")

        if (a.pair != null) {
            val pair = a.pair
            text("A ↔ B · ${pair.relation.name.lowercase()}", 16f)
            when (pair.relation) {
                PairRelation.HORIZONTAL -> text("Horizontal gap: ${pair.horizontal.label()}")
                PairRelation.VERTICAL -> text("Vertical gap: ${pair.vertical.label()}")
                PairRelation.DIAGONAL -> text("Horizontal separation: ${pair.horizontal.label()}\nVertical separation: ${pair.vertical.label()}")
                PairRelation.OVERLAPPING -> text("Items overlap. Horizontal and vertical separation: 0 dp / 0 px")
            }
        }
        if (details) text("SPACING", 16f)
        if (!details) text(a.neighbors.values.joinToString("\n") {
            "${it.direction.name.lowercase().replaceFirstChar { c -> c.uppercase() }}: ${it.distance.label()}"
        }.ifEmpty { "No reliable neighbors exposed" }, 12f)
        else if (a.neighbors.isEmpty()) text("No reliable neighbors exposed", 12f)
        Direction.entries.forEach { direction ->
            val n = a.neighbors[direction]
            if (n != null && details) {
                val label = direction.name.lowercase().replaceFirstChar { it.uppercase() }
                text("$label: ${n.distance.label()}")
                if (details) {
                    text("${n.node.resourceId ?: n.node.text ?: n.node.className}\nConfidence ${formatDimension(n.confidence * 100)}%", 12f)
                    buttons(listOf("Select $label" to "neighbor:${direction.name}"))
                }
            } else if (details) text("${direction.name.lowercase()}: No reliable neighbor", 12f)
        }
        if (details) text("COLOR", 16f)
        val colors = a.renderedColor?.colors
        fun swatch(label: String, color: Int?, hex: String?) {
            text("$label\n${hex ?: "Unavailable / no single main color"}")
            if (color != null) {
                content.addView(TextView(context).apply {
                    text = "  ${rgbDescription(color)}"; textSize = 13f; setBackgroundColor(color)
                    val brightness = Color.red(color) * .299 + Color.green(color) * .587 + Color.blue(color) * .114
                    setTextColor(if (brightness > 140) Color.BLACK else Color.WHITE)
                    setPadding(0, host.dp(8), 0, host.dp(8))
                })
            }
        }
        if (a.renderedColor == null) text("Analyzing color…")
        else {
            if (details) swatch("Rendered Main Color", colors?.dominantColor, colors?.dominantHex)
            else {
                val row = LinearLayout(context).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
                colors?.dominantColor?.let { color -> row.addView(android.view.View(context).apply { setBackgroundColor(color) }, LinearLayout.LayoutParams(host.dp(20), host.dp(20))) }
                row.addView(TextView(context).apply {
                    text = "  ${colors?.dominantHex ?: "No single main color"}"; textSize = 16f; setTextColor(0xff132d43.toInt())
                    contentDescription = "Rendered Main Color ${colors?.dominantHex ?: "unavailable"}"
                })
                content.addView(row)
            }
            if (details) {
                swatch("Center pixel", colors?.centerColor, colors?.centerHex)
                colors?.topColors?.forEach { share -> swatch("${String.format(Locale.US, "%.2f", share.fraction * 100)}%", share.color, share.hex) }
                text(a.renderedColor.status, 12f)
                text("Screenshot estimate, not a source background property.", 12f)
                if (a.frozen) text(if (a.renderedColor.screenshot != null) "Captured item screenshot retained in this frozen snapshot" else "No screenshot available for this frozen snapshot", 12f)
                buttons(listOf("Refresh color" to "refresh"))
            } else if (colors == null) text(a.renderedColor.status, 12f)
        }
        if (details) {
            text("POSITION", 16f)
            text("X: ${a.x.label()}\nY: ${a.y.label()}\nBounds: ${a.boundsPx}")
            text("Window edges (not neighbor gaps)", 13f)
            a.windowEdges.forEach { (dir, distance) -> text("${dir.name.lowercase()}: ${distance.label()}", 12f) }
            buttons(listOf("Parent" to "parent", "Child" to "child", "Next candidate" to "next", "Previous" to "previous"))
            text(candidatePosition, 12f)
            buttons(listOf((if (advanced) "Hide Advanced" else "Advanced ▸") to "advanced"))
            if (advanced) {
                val n = a.node
                text("ADVANCED / ACCESSIBILITY", 16f)
                text("Package\n${n.packageName}\n\nResource ID\n${n.resourceId ?: "—"}\n\nClass\n${n.className}\n\nText\n${n.text ?: "—"}\n\nContent Description\n${n.contentDescription ?: "—"}")
                text("clickable ${n.clickable}\nenabled ${n.enabled}\nfocusable ${n.focusable}\nfocused ${n.focused}\nselected ${n.selected}\ncheckable ${n.checkable}\nchecked ${n.checked}\nscrollable ${n.scrollable}\neditable ${n.editable}\nvisibleToUser ${n.visibleToUser}\npassword ${n.password}")
                text("Window ${n.windowId} · Depth ${n.depth} · Children ${n.childCount}\nActions\n${n.actions.joinToString("\n")}")
                buttons(listOf("Copy ID" to "id", "Copy bounds" to "bounds", "Copy Appium" to "appium"))
            }
        }
        val screen = host.manager.currentWindowMetrics.bounds
        val w = host.dp(if (details) 350 else 280).coerceAtMost(screen.width() - host.dp(16))
        val h = if (details) (screen.height() * .78f).toInt() else host.dp(300).coerceAtMost((screen.height() * .48f).toInt())
        val x = if (a.boundsPx.left + a.boundsPx.width / 2 > screen.width() / 2) host.dp(8) else screen.width() - w - host.dp(8)
        val y = (screen.height() - h - host.dp(38)).coerceAtLeast(host.dp(8))
        screenBounds = Rect(x, y, x + w, y + h)
        panel = root
        return host.add(root, host.params(w, h).apply { this.x = x; this.y = y })
    }
    fun showPicker(value: String, actions: Map<String, () -> Unit>) {
        remove()
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(12), host.dp(12), host.dp(12), host.dp(12)); setBackgroundColor(0xfff3f7fb.toInt()) }
        root.addView(TextView(context).apply { text = "Color Picker\n$value"; textSize = 18f; setTextColor(Color.BLACK) })
        actions.forEach { (name, action) -> root.addView(Button(context).apply { text = name; setOnClickListener { action() } }) }
        panel = root
        val b = host.manager.currentWindowMetrics.bounds
        host.add(root, host.params(host.dp(280), -2).apply { x = (b.width() - host.dp(288)).coerceAtLeast(0); y = host.dp(180) })
    }
    fun remove() { host.remove(panel); panel = null; scroll = null; shownNode = null; screenBounds = null }
}
