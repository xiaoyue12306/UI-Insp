package com.xiaoyue.uiinspector.overlay
import android.widget.LinearLayout
import com.xiaoyue.uiinspector.analysis.SelectedItemAnalysis
import com.xiaoyue.uiinspector.interaction.*
import com.xiaoyue.uiinspector.measurement.PairRelation
object QuickResultCard {
    fun fill(root: LinearLayout,a: SelectedItemAnalysis,prefs: PresentationPreferences,actions: Map<String, (() -> Unit)?>) = with(root) {
        if(a.pair != null) {
            line("A ↔ B",14f)
            when(a.pair.relation) {
                PairRelation.HORIZONTAL -> dual(prefs.lines(a.pair.horizontal))
                PairRelation.VERTICAL -> dual(prefs.lines(a.pair.vertical))
                PairRelation.DIAGONAL -> { line("Horizontal / Vertical",12f,true); line("${prefs.lines(a.pair.horizontal)}\n${prefs.lines(a.pair.vertical)}",12f) }
                PairRelation.OVERLAPPING -> line("Items overlap",14f)
            }
            actionRow(listOf("Done" to actions["done"],"Measure again" to actions["pair"]))
            return@with
        }
        line("SIZE",12f,true)
        dual(prefs.size(a.width,a.height))
        if(a.neighbors.isNotEmpty()) line("NEARBY SPACING",12f,true)
        SpacingPresentation.visible(a.neighbors,prefs.showAllSpacing).take(2).forEach {
            val row=LinearLayout(context).apply { gravity=android.view.Gravity.CENTER_VERTICAL }
            row.line(SpacingPresentation.title(it.direction),12f,true).layoutParams=
                LinearLayout.LayoutParams((76*resources.displayMetrics.density).toInt(),-2)
            val units=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding((20*resources.displayMetrics.density).toInt(),0,0,0) }
            prefs.lines(it.distance).split('\n').forEachIndexed { i,s -> units.line(s,if(i==0)18f else 14f,i>0) }
            row.addView(units); addView(row)
        }
        line("COLOR",12f,true)
        swatch(a.renderedColor?.colors?.dominantColor,a.renderedColor.colorMessage())
        if(a.stale) line("Saved result · tap the bubble to update",11f,true)
        actionRow(listOf("Details" to actions["details"],"×" to actions["close"]))
    }
}
