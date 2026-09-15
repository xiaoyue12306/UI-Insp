package com.xiaoyue.uiinspector.ui.details

import android.widget.LinearLayout
import com.xiaoyue.uiinspector.analysis.*
import com.xiaoyue.uiinspector.interaction.*
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.overlay.*

object MeasurementDetails {
    fun fill(body: LinearLayout, a: SelectedItemAnalysis, prefs: PresentationPreferences,
             actions: Map<String, (() -> Unit)?>) = with(body) {
        val density = resources.displayMetrics.density
        fun group(title: String, fill: LinearLayout.() -> Unit) {
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((12*density).toInt(),(10*density).toInt(),(12*density).toInt(),(10*density).toInt())
                background = context.rounded(0xffffffff.toInt(),12)
                line(title,12f,true); fill()
            }
            addView(card,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=(10*density).toInt() })
        }
        actionRow(listOf("Smaller" to actions["smaller"],"Larger" to actions["larger"]))
        if(a.stale) line("Saved result. Tap the bubble to measure again.",12f,true)
        group("SIZE") {
            val row = LinearLayout(context)
            listOf("Width" to a.width,"Height" to a.height).forEach { (name,value) ->
                val cell = LinearLayout(context).apply {
                    orientation=LinearLayout.VERTICAL; line(name,13f,true); dual(prefs.lines(value,exact=true))
                }
                row.addView(cell,LinearLayout.LayoutParams(0,-2,1f))
            }
            addView(row)
            line("≈ indicates pixel rounding. Exact converted dp is shown here.",11f,true)
        }
        group("NEARBY SPACING") {
            line("Edge to edge · tap a distance to inspect that neighbor",12f,true)
            listOf(listOf(Direction.TOP,Direction.BOTTOM),listOf(Direction.LEFT,Direction.RIGHT)).forEach { directions ->
                val row=LinearLayout(context)
                directions.forEach { dir ->
                    val neighbor=a.neighbors[dir]
                    val cell=LinearLayout(context).apply {
                        orientation=LinearLayout.VERTICAL
                        setPadding((6*density).toInt(),(10*density).toInt(),(6*density).toInt(),(10*density).toInt())
                        line(SpacingPresentation.title(dir),14f)
                        if(neighbor==null) line("—",16f,true) else {
                            dual(prefs.lines(neighbor.distance,exact=true))
                            actions["neighbor:${dir.name}"]?.let { callback ->
                                isClickable=true; setOnClickListener { callback() }
                                contentDescription="Inspect ${dir.name.lowercase()} neighbor"
                            }
                        }
                    }
                    row.addView(cell,LinearLayout.LayoutParams(0,-2,1f))
                }
                addView(row)
            }
            action("Measure between two items",actions["pair"])
        }
        group("COLOR") {
            val colors=a.renderedColor?.colors
            swatch(colors?.dominantColor,a.renderedColor.colorMessage(),colors?.dominantColor?.let(::rgbDescription))
            if(colors!=null) {
                line("Center pixel",12f,true)
                swatch(colors.centerColor,colors.centerHex ?: "Unavailable",colors.centerColor?.let(::rgbDescription))
                if(colors.topColors.size>1) {
                    line("Other sampled colors",12f,true)
                    colors.topColors.filter { it.color!=colors.dominantColor }.take(3).forEach {
                        swatch(it.color,it.hex,"${formatDimension((it.fraction*100).toFloat())}%")
                    }
                }
            }
            action("Color picker",actions["picker"])
        }
    }
}
