package com.xiaoyue.uiinspector.overlay
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.widget.*
import com.xiaoyue.uiinspector.analysis.*
import com.xiaoyue.uiinspector.interaction.*
import com.xiaoyue.uiinspector.inspector.NodeSnapshot
import com.xiaoyue.uiinspector.ui.details.MeasurementDetails
class InspectorPanelOverlay(private val context: Context,private val host: OverlayController,private val back: ()->Unit) {
    private var panel: View?=null
    private var scroller: ScrollView?=null
    private var shown: Pair<NodeSnapshot,Boolean>?=null
    var screenBounds: Rect?=null; private set
    private fun root()=InspectorSurface(context,back).apply {
        setPadding(host.dp(12),host.dp(12),host.dp(12),host.dp(12)); background=context.rounded(0xfff0f5fa.toInt(),16,0xffd2dee9.toInt()); elevation=host.dp(6).toFloat()
    }
    fun show(a: SelectedItemAnalysis,details: Boolean,advanced: Boolean,prefs: PresentationPreferences,actions: Map<String, (() -> Unit)?>): Boolean {
        val previousScroll=if(shown==(a.node to details)) scroller?.scrollY ?: 0 else 0
        remove(); shown=a.node to details
        val root=root(); val screen=host.manager.maximumWindowMetrics.bounds
        val tablet=screen.width()/context.resources.displayMetrics.density>=600
        val width=if(details) { if(tablet) host.dp(360) else screen.width()-host.dp(16) } else host.dp(252)
        val maxHeight=if(details) (screen.height()*(if(tablet) .85f else .65f)).toInt() else (screen.height()-host.dp(80)).coerceAtLeast(host.dp(48))
        if(details) {
            root.line("Measurement",20f); root.actionRow(listOf("Back" to actions["details"],"Copy" to actions["summary"],"×" to actions["close"]))
            val body=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL }
            val scroll=ScrollView(context).apply { addView(body) }; scroller=scroll
            root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f)); MeasurementDetails.fill(body,a,prefs,advanced,actions)
            scroll.post { scroll.scrollTo(0,previousScroll) }
        } else QuickResultCard.fill(root,a,prefs,actions)
        val w=width.coerceAtMost(screen.width()-host.dp(16))
        root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(maxHeight,View.MeasureSpec.AT_MOST))
        val h=if(details) maxHeight else root.measuredHeight.coerceAtMost(maxHeight)
        val safe=PlacementRect(0f,host.dp(40).toFloat(),screen.width().toFloat(),(screen.height()-host.dp(40)).toFloat()); val b=a.boundsPx
        val place=if(details&&!tablet) PlacementRect(host.dp(8).toFloat(),safe.bottom-h,host.dp(8)+w.toFloat(),safe.bottom)
            else ResultCardPlacement.place(safe,PlacementRect(b.left.toFloat(),b.top.toFloat(),b.right.toFloat(),b.bottom.toFloat()),w.toFloat(),h.toFloat(),host.dp(8).toFloat())
        screenBounds=Rect(place.left.toInt(),place.top.toInt(),place.right.toInt(),place.bottom.toInt()); panel=root
        return host.add(root,host.focusableParams(w,h).apply { x=place.left.toInt(); y=place.top.toInt() })
    }
    fun simple(title: String,anchor: Rect?,fill: LinearLayout.()->Unit): Boolean {
        remove(); val root=root(); if(title.isNotBlank()) root.line(title,18f); root.fill()
        val screen=host.manager.maximumWindowMetrics.bounds; val w=host.dp(288).coerceAtMost(screen.width()-host.dp(16))
        root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(screen.height()-host.dp(96),View.MeasureSpec.AT_MOST))
        val h=root.measuredHeight; val x=if(anchor!=null && anchor.centerX()<screen.width()/2) host.dp(8) else screen.width()-w-host.dp(8)
        val y=(anchor?.bottom ?: host.dp(100)).coerceIn(host.dp(40),(screen.height()-h-host.dp(40)).coerceAtLeast(host.dp(40)))
        panel=root; screenBounds=Rect(x,y,x+w,y+h)
        return host.add(root,host.focusableParams(w,h).apply { this.x=x; this.y=y })
    }
    fun remove() { host.remove(panel); panel=null; scroller=null; shown=null; screenBounds=null }
}
