package com.xiaoyue.uiinspector.ui.details
import android.widget.LinearLayout
import com.xiaoyue.uiinspector.analysis.*
import com.xiaoyue.uiinspector.interaction.*
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.overlay.*
object MeasurementDetails {
    fun fill(body: LinearLayout,a: SelectedItemAnalysis,prefs: PresentationPreferences,advanced: Boolean,actions: Map<String, (() -> Unit)?>) = with(body) {
        fun group(title: String,fill: LinearLayout.()->Unit) {
            val card=LinearLayout(context).apply {
                orientation=LinearLayout.VERTICAL; val d=resources.displayMetrics.density
                setPadding((12*d).toInt(),(10*d).toInt(),(12*d).toInt(),(10*d).toInt()); background=context.rounded(0xffffffff.toInt(),12)
                line(title,12f,true); fill()
            }
            addView(card,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=(10*resources.displayMetrics.density).toInt() })
        }
        actionRow(listOf("Smaller" to actions["smaller"],"Larger" to actions["larger"]))
        if(a.stale) line("Saved result. Tap the bubble to measure the current screen.",12f,true)
        group("SIZE") { line("Width",13f,true); dual(prefs.lines(a.width)); line("Height",13f,true); dual(prefs.lines(a.height)) }
        group("SPACING") {
            a.pair?.let { line("A ↔ B · ${it.relation.name.lowercase()}"); line("Horizontal",12f,true); dual(prefs.lines(it.horizontal)); line("Vertical",12f,true); dual(prefs.lines(it.vertical)); actionRow(listOf("Done" to actions["done"],"Measure again" to actions["pair"])) }
            Direction.entries.forEach { dir ->
                val neighbor=a.neighbors[dir]; val name=dir.name.lowercase().replaceFirstChar { it.uppercase() }
                val row=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(0,10,0,10) }
                row.line(name,14f)
                if(neighbor==null) row.line("No nearby item",12f,true) else {
                    row.dual(prefs.lines(neighbor.distance))
                    row.line(neighbor.node.text?.takeIf { it.isNotBlank() } ?: neighbor.node.className?.substringAfterLast('.') ?: "Item",12f,true)
                    actions["neighbor:${dir.name}"]?.let { callback -> row.isClickable=true; row.setOnClickListener { callback() }; row.contentDescription="Select $name neighbor" }
                }; addView(row)
            }
        }
        group("COLOR") {
            val colors=a.renderedColor?.colors
            swatch(colors?.dominantColor,a.renderedColor.colorMessage(),colors?.dominantColor?.let(::rgbDescription))
            line("Rendered pixels, not a source background property.",11f,true)
            if(colors!=null) {
                line("Center pixel",12f,true); swatch(colors.centerColor,colors.centerHex ?: "Unavailable")
                line("Top colors",12f,true); colors.topColors.forEach { swatch(it.color,it.hex,"${formatDimension((it.fraction*100).toFloat())}%") }
            }
        }
        group("POSITION") {
            line("X",12f,true); dual(prefs.lines(a.x)); line("Y",12f,true); dual(prefs.lines(a.y)); line("Window edges",12f,true)
            a.windowEdges.forEach { (dir,value) -> line(dir.name.lowercase(),12f,true); dual(prefs.lines(value)) }
        }
        group("MORE") {
            action("Measure between two items",actions["pair"]); action("Color picker",actions["picker"]); action("Settings",actions["settings"])
            action(if(advanced) "Advanced ▾" else "Advanced ▸",actions["advanced"])
            if(advanced) {
                val n=a.node
                line("Resource ID",12f,true); line(n.resourceId ?: "Not provided").setOnLongClickListener { actions["id"]?.invoke(); true }
                line("Class\n${n.className}\nText\n${n.text ?: "—"}\nContent Description\n${n.contentDescription ?: "—"}\nPackage\n${n.packageName}\nBounds\n${n.bounds}",13f)
                line("clickable ${n.clickable}\nenabled ${n.enabled}\nfocusable ${n.focusable}\nfocused ${n.focused}\nselected ${n.selected}\ncheckable ${n.checkable}\nchecked ${n.checked}\nscrollable ${n.scrollable}\neditable ${n.editable}\nvisibleToUser ${n.visibleToUser}\npassword ${n.password}",12f)
                line("Window ${n.windowId} · Depth ${n.depth} · Children ${n.childCount}\n${n.actions.joinToString("\n")}",12f,true)
                a.neighbors.values.forEach { line("${it.direction}: confidence ${formatDimension(it.confidence*100)}%",12f,true) }
                line(a.renderedColor?.status ?: "Pending screenshot",12f,true)
                actionRow(listOf("Parent" to actions["parent"],"Child" to actions["child"]))
                actionRow(listOf("Copy bounds" to actions["bounds"],"Copy Appium" to actions["appium"]))
            }
        }
    }
}
