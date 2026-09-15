package com.xiaoyue.uiinspector.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VERTICAL
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

class InspectorSurface(context: Context, private val back: () -> Unit) : LinearLayout(context) {
    private var unregisterBack: (() -> Unit)?=null
    init { orientation=VERTICAL }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if(event.keyCode==KeyEvent.KEYCODE_BACK || event.keyCode==KeyEvent.KEYCODE_ESCAPE) {
            if(event.action==KeyEvent.ACTION_UP && !event.isCanceled) back()
            return true
        }; return super.dispatchKeyEvent(event)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if(Build.VERSION.SDK_INT>=33) unregisterBack=registerPlatformBack()
    }
    @androidx.annotation.RequiresApi(33)
    private fun registerPlatformBack(): (() -> Unit)? {
        val dispatcher=findOnBackInvokedDispatcher() ?: return null
        val callback=OnBackInvokedCallback { back() }
        dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,callback)
        return { dispatcher.unregisterOnBackInvokedCallback(callback) }
    }
    override fun onDetachedFromWindow() {
        unregisterBack?.invoke(); unregisterBack=null
        super.onDetachedFromWindow()
    }
}
fun OverlayController.focusableParams(width: Int,height: Int)=params(width,height).apply {
    flags=(flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()) or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
}
fun Context.rounded(color: Int, radiusDp: Int=14, stroke: Int?=null)=GradientDrawable().apply {
    setColor(color); cornerRadius=radiusDp*resources.displayMetrics.density
    stroke?.let { setStroke((resources.displayMetrics.density).toInt().coerceAtLeast(1),it) }
}
fun LinearLayout.line(value: String, size: Float=14f, muted: Boolean=false): TextView = TextView(context).apply {
    text=value; textSize=size; setTextColor(if(muted) 0xff577086.toInt() else 0xff132d43.toInt())
    setPadding(0,6,0,6)
}.also { addView(it) }
fun LinearLayout.dual(value: String) { value.split('\n').forEachIndexed { i,s -> line(s,if(i==0) 19f else 12f,i>0) } }
fun LinearLayout.action(label: String, action: (() -> Unit)?): Button=Button(context).apply {
    text=label; textSize=12f; isAllCaps=false; isEnabled=action!=null; setOnClickListener { action?.invoke() }
}.also { addView(it,LayoutParams(-1,(44*resources.displayMetrics.density).toInt())) }
fun LinearLayout.actionRow(items: List<Pair<String, (() -> Unit)?>>) {
    val row=LinearLayout(context)
    items.forEach { (label,action) -> row.addView(Button(context).apply { text=label; textSize=12f; isAllCaps=false; isEnabled=action!=null; setOnClickListener { action?.invoke() } },LayoutParams(0,(44*resources.displayMetrics.density).toInt(),1f)) }
    addView(row)
}
fun LinearLayout.swatch(color: Int?, label: String, subtitle: String?=null) {
    val row=LinearLayout(context).apply { gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(0,8,0,8) }
    val size=(24*resources.displayMetrics.density).toInt()
    row.addView(android.view.View(context).apply { background=context.rounded(color ?: Color.TRANSPARENT,3,0xff728497.toInt()) },LayoutParams(size,size))
    val words=LinearLayout(context).apply { orientation=VERTICAL; setPadding(size/3,0,0,0) }
    words.line(label,16f); subtitle?.let { words.line(it,12f,true) }; row.addView(words,LayoutParams(0,-2,1f)); addView(row)
}
