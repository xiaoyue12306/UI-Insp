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
        dual(prefs.size(a.width,a.height))
        SpacingPresentation.visible(a.neighbors,prefs.showAllSpacing).take(2).forEach {
            val row=LinearLayout(context).apply { gravity=android.view.Gravity.CENTER_VERTICAL }
            row.line(it.direction.name.lowercase().replaceFirstChar { c -> c.uppercase() },12f,true)
            val units=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(16,0,0,0) }
            prefs.lines(it.distance).split('\n').forEachIndexed { i,s -> units.line(s,if(i==0)15f else 11f,i>0) }
            row.addView(units); addView(row)
        }
        swatch(a.renderedColor?.colors?.dominantColor,a.renderedColor.colorMessage())
        if(a.stale) line("Saved result · tap the bubble to update",11f,true)
        actionRow(listOf("Details" to actions["details"],"×" to actions["close"]))
    }
}
