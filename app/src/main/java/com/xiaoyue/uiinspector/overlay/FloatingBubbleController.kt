package com.xiaoyue.uiinspector.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ImageButton
import com.xiaoyue.uiinspector.R
import com.xiaoyue.uiinspector.interaction.InspectorPreferences
import kotlin.math.abs

class FloatingBubbleController(context: Context, private val host: OverlayController, private val prefs: InspectorPreferences,
                               select: () -> Unit, menu: () -> Unit) {
    private val size = host.dp(48)
    private val params = host.params(size,size)
    val view = ImageButton(context).apply {
        setImageResource(R.drawable.ic_bubble_inspect); setColorFilter(Color.WHITE)
        setPadding(host.dp(12),host.dp(12),host.dp(12),host.dp(12))
        background = GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(0xdf284b63.toInt()); setStroke(host.dp(1),0xff91b4d0.toInt()) }
        contentDescription="Inspect an item. Long press for more options."
        elevation=host.dp(4).toFloat()
        setOnClickListener { select() }; setOnLongClickListener { menu(); true }
    }
    init {
        var downX=0f; var downY=0f; var baseX=0; var baseY=0; var moved=false
        val slop=ViewConfiguration.get(context).scaledTouchSlop
        view.setOnTouchListener { _, e ->
            when(e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; baseX=params.x; baseY=params.y; moved=false; false }
                MotionEvent.ACTION_MOVE -> {
                    if(abs(e.rawX-downX)+abs(e.rawY-downY)>slop) moved=true
                    if(moved) {
                        view.cancelLongPress(); val screen=host.manager.maximumWindowMetrics.bounds
                        params.x=(baseX+e.rawX-downX).toInt().coerceIn(0,(screen.width()-size).coerceAtLeast(0))
                        params.y=(baseY+e.rawY-downY).toInt().coerceIn(host.dp(32),(screen.height()-size-host.dp(32)).coerceAtLeast(host.dp(32)))
                        host.update(view,params)
                    }; moved
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if(moved) {
                    val screen=host.manager.maximumWindowMetrics.bounds
                    prefs.bubbleRight=params.x+size/2>screen.width()/2
                    prefs.bubbleY=params.y.toFloat()/(screen.height()-size).coerceAtLeast(1)
                    position(); host.update(view,params); view.isPressed=false; true
                } else false
                else -> false
            }
        }
    }
    private fun position() {
        val screen=host.manager.maximumWindowMetrics.bounds
        params.x=if(prefs.bubbleRight) (screen.width()-size-host.dp(4)).coerceAtLeast(0) else host.dp(4)
        params.y=((screen.height()-size)*prefs.bubbleY).toInt().coerceIn(host.dp(32),(screen.height()-size-host.dp(32)).coerceAtLeast(host.dp(32)))
    }
    fun selecting(value: Boolean) {
        view.setImageResource(if(value) android.R.drawable.ic_menu_close_clear_cancel else R.drawable.ic_bubble_inspect)
        view.contentDescription=if(value) "Cancel selection" else "Inspect an item. Long press for more options."
    }
    fun bounds()=Rect(params.x,params.y,params.x+size,params.y+size)
    fun show(): Boolean {
        position()
        val added=host.add(view,params)
        // A bubble on the edge must not turn its drag into the system Back gesture.
        if(added) view.post { view.systemGestureExclusionRects=listOf(Rect(0,0,size,size)) }
        return added
    }
    fun remove()=host.remove(view)
}
