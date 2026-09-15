package com.xiaoyue.uiinspector.overlay
import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.roundToInt

/** Magnifies real pixels captured on picker entry; the final pixel is sampled again on release. */
class PixelMagnifierOverlay(context: Context,private val host: OverlayController,private val bitmap: Bitmap): View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var px=bitmap.width/2f; private var py=bitmap.height/2f
    private val origin=IntArray(2)
    fun point(x: Float,y: Float) { px=x; py=y; invalidate() }
    override fun onDraw(canvas: Canvas) {
        if(bitmap.isRecycled) return
        getLocationOnScreen(origin)
        val d=resources.displayMetrics.density; val size=88*d
        val x=(px-origin[0]-size/2).coerceIn(4*d,(width-size-4*d).coerceAtLeast(4*d))
        val y=(py-origin[1]-size-32*d).coerceIn(110*d,(height-size-4*d).coerceAtLeast(110*d))
        val destination=RectF(x,y,x+size,y+size)
        val sx=px.roundToInt().coerceIn(0,bitmap.width-1); val sy=py.roundToInt().coerceIn(0,bitmap.height-1)
        val source=Rect((sx-14).coerceAtLeast(0),(sy-14).coerceAtLeast(0),(sx+15).coerceAtMost(bitmap.width),(sy+15).coerceAtMost(bitmap.height))
        canvas.save(); canvas.clipPath(Path().apply { addOval(destination,Path.Direction.CW) }); paint.isFilterBitmap=false
        canvas.drawBitmap(bitmap,source,destination,paint); canvas.restore()
        paint.color=0xff284b63.toInt(); paint.style=Paint.Style.STROKE; paint.strokeWidth=2*d; canvas.drawOval(destination,paint)
        val cx=destination.centerX(); val cy=destination.centerY()
        paint.color=Color.WHITE; paint.strokeWidth=3*d; canvas.drawLine(cx-10*d,cy,cx+10*d,cy,paint); canvas.drawLine(cx,cy-10*d,cx,cy+10*d,paint)
        paint.color=Color.BLACK; paint.strokeWidth=d; canvas.drawLine(cx-10*d,cy,cx+10*d,cy,paint); canvas.drawLine(cx,cy-10*d,cx,cy+10*d,paint)
        paint.style=Paint.Style.FILL
    }
    fun show()=host.add(this,host.params(-1,-1,false))
}
