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
    text=value; textSize=(size*1.12f).coerceAtLeast(14f)
    setTextColor(if(muted) 0xff526579.toInt() else 0xff172b42.toInt())
    typeface=Typeface.create("sans-serif",if(size>=19f) Typeface.BOLD else Typeface.NORMAL)
    val inset=(4*resources.displayMetrics.density).toInt()
    setPadding(0,inset,0,inset); setLineSpacing(0f,1.12f)
}.also { addView(it) }
fun LinearLayout.dual(value: String) { value.split('\n').forEachIndexed { i,s -> line(s,if(i==0) 22f else 14f,i>0) } }
private fun Context.control(label: String, callback: (() -> Unit)?): Button=Button(this).apply {
    text=label; textSize=15f; isAllCaps=false; isEnabled=callback!=null
    typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
    val primary=label=="Details" || label=="Done"
    setTextColor(if(callback==null) 0xff8995a3.toInt() else if(primary) Color.WHITE else 0xff285b91.toInt())
    val fill=if(callback==null) 0xffedf0f4.toInt() else if(primary) 0xff2864a4.toInt() else 0xffeaf2fc.toInt()
    background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x222864a4),rounded(fill,12),null)
    elevation=0f; stateListAnimator=null
    val d=resources.displayMetrics.density
    setPadding((10*d).toInt(),(8*d).toInt(),(10*d).toInt(),(8*d).toInt())
    minHeight=(48*d).toInt(); minimumWidth=0; minWidth=0
    setOnClickListener { callback?.invoke() }
}
fun LinearLayout.action(label: String, action: (() -> Unit)?): Button=context.control(label,action).also {
    addView(it,LayoutParams(-1,-2).apply { topMargin=(8*resources.displayMetrics.density).toInt() })
}
fun LinearLayout.actionRow(items: List<Pair<String, (() -> Unit)?>>) {
    val row=LinearLayout(context)
    items.forEachIndexed { index,(label,action) -> row.addView(context.control(label,action),LayoutParams(0,-2,1f).apply {
        if(index>0) marginStart=(8*resources.displayMetrics.density).toInt()
    }) }
    addView(row,LayoutParams(-1,-2).apply { topMargin=(8*resources.displayMetrics.density).toInt(); bottomMargin=(8*resources.displayMetrics.density).toInt() })
}
fun LinearLayout.swatch(color: Int?, label: String, subtitle: String?=null) {
    val row=LinearLayout(context).apply { gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(0,8,0,8) }
    val size=(36*resources.displayMetrics.density).toInt()
    row.addView(android.view.View(context).apply { background=context.rounded(color ?: Color.TRANSPARENT,9,0xffbcc9d6.toInt()) },LayoutParams(size,size))
    val words=LinearLayout(context).apply { orientation=VERTICAL; setPadding(size/3,0,0,0) }
    words.line(label,18f); subtitle?.let { words.line(it,14f,true) }; row.addView(words,LayoutParams(0,-2,1f)); addView(row)
}
