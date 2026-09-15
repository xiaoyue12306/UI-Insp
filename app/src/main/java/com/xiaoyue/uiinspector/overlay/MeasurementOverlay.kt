package com.xiaoyue.uiinspector.overlay
import android.content.Context
import android.graphics.*
import android.view.View
import com.xiaoyue.uiinspector.analysis.SelectedItemAnalysis
import com.xiaoyue.uiinspector.interaction.*
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.util.Bounds
import kotlin.math.*

class MeasurementOverlay(context: Context,private val host: OverlayController,private val analysis: SelectedItemAnalysis,
                         private val reserved: Rect?=null,private val preferences: PresentationPreferences=PresentationPreferences(),private val bubble: Rect?=null): View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val origin=IntArray(2)
    private lateinit var placement: LabelPlacementEngine
    private val labels=mutableListOf<() -> Unit>()
    private val sizeColor=0xff008cff.toInt(); private val gapColor=0xffffa726.toInt(); private val pairColor=0xffab47bc.toInt()
    private fun dp(value: Int)=value*resources.displayMetrics.density
    private fun local(b: Bounds)=RectF((b.left-origin[0]).toFloat(),(b.top-origin[1]).toFloat(),(b.right-origin[0]).toFloat(),(b.bottom-origin[1]).toFloat())
    private fun RectF.geometry()=PlacementRect(left,top,right,bottom)
    override fun onDraw(canvas: Canvas) {
        labels.clear()
        getLocationOnScreen(origin)
        val b=local(analysis.boundsPx)
        val exclusions=listOfNotNull(reserved,bubble).map { RectF(it).apply { offset(-origin[0].toFloat(),-origin[1].toFloat()) }.geometry() }
        placement=LabelPlacementEngine(PlacementRect(dp(4),dp(36),width-dp(4),height-dp(36)),b.geometry(),analysis.neighbors.values.map { local(it.node.bounds).geometry() },exclusions,dp(8))
        paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(2); paint.color=sizeColor; canvas.drawRect(b,paint)
        val small=max(analysis.width.dp,analysis.height.dp)<=32
        val windowWidth=analysis.width.px+(analysis.windowEdges[Direction.LEFT]?.px ?: 0f)+(analysis.windowEdges[Direction.RIGHT]?.px ?: 0f)
        val windowHeight=analysis.height.px+(analysis.windowEdges[Direction.TOP]?.px ?: 0f)+(analysis.windowEdges[Direction.BOTTOM]?.px ?: 0f)
        val large=b.width()*b.height()>=min(width*height.toFloat(),windowWidth*windowHeight)*.75f && min(analysis.width.dp,analysis.height.dp)>200
        if(analysis.pair==null) {
        if(small || large) label(canvas,preferences.size(analysis.width,analysis.height),b.centerX(),if(large)b.top+dp(28) else b.top-dp(30),sizeColor,true)
        else {
            val y=if(b.top>dp(100)) b.top-dp(16) else b.bottom+dp(16)
            ruler(canvas,b.left,y,b.right,y,sizeColor)
            label(canvas,"W ${preferences.lines(analysis.width)}",b.centerX(),if(y<b.top)y-dp(28) else y+dp(28),sizeColor,true)
            val x=if(width-b.right>dp(110)) b.right+dp(16) else b.left-dp(16)
            ruler(canvas,x,b.top,x,b.bottom,sizeColor)
            label(canvas,"H ${preferences.lines(analysis.height)}",if(x>b.right)x+dp(48) else x-dp(48),b.centerY(),sizeColor,true)
        }
        }
        if(!large && analysis.pair==null) SpacingPresentation.visible(analysis.neighbors,preferences.showAllSpacing).forEach { n ->
            val other=local(n.node.bounds)
            val x=(max(b.left,other.left)+min(b.right,other.right))/2; val y=(max(b.top,other.top)+min(b.bottom,other.bottom))/2
            val points=when(n.direction) {
                Direction.TOP -> floatArrayOf(x,other.bottom,x,b.top)
                Direction.BOTTOM -> floatArrayOf(x,b.bottom,x,other.top)
                Direction.LEFT -> floatArrayOf(other.right,y,b.left,y)
                Direction.RIGHT -> floatArrayOf(b.right,y,other.left,y)
            }
            if(label(canvas,"${SpacingPresentation.title(n.direction)} ${preferences.lines(n.distance)}",(points[0]+points[2])/2,(points[1]+points[3])/2,gapColor,false)) {
                // Identify the neighbor even when collision avoidance moves its label.
                paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(1); paint.color=0x99ffa726.toInt()
                paint.pathEffect=DashPathEffect(floatArrayOf(dp(4),dp(3)),0f)
                canvas.drawRect(other,paint); paint.pathEffect=null
                ruler(canvas,points[0],points[1],points[2],points[3],gapColor)
            }
        }
        analysis.pair?.let { pair ->
            val a=local(pair.a); val p=local(pair.b)
            paint.color=pairColor; paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(2); canvas.drawRect(a,paint); canvas.drawRect(p,paint)
            label(canvas,"A",a.left,a.top,pairColor,true); label(canvas,"B",p.left,p.top,pairColor,true)
            if(pair.relation==PairRelation.VERTICAL) {
                val x=(max(a.left,p.left)+min(a.right,p.right))/2; val y1=if(a.bottom<=p.top)a.bottom else p.bottom; val y2=if(a.bottom<=p.top)p.top else a.top
                ruler(canvas,x,y1,x,y2,pairColor); label(canvas,"A ↕ B ${preferences.lines(pair.vertical)}",x,(y1+y2)/2,pairColor,true)
            } else if(pair.relation==PairRelation.HORIZONTAL) {
                val y=(max(a.top,p.top)+min(a.bottom,p.bottom))/2; val x1=if(a.right<=p.left)a.right else p.right; val x2=if(a.right<=p.left)p.left else a.left
                ruler(canvas,x1,y,x2,y,pairColor); label(canvas,"A ↔ B ${preferences.lines(pair.horizontal)}",(x1+x2)/2,y,pairColor,true)
            }
        }
        if(!large && analysis.pair==null) label(canvas,analysis.renderedColor.colorMessage().substringBefore('\n'),b.centerX(),b.bottom+dp(42),sizeColor,false,analysis.renderedColor?.colors?.dominantColor)
        // Draw text last so neighboring leaders and rulers cannot run across its glyphs.
        labels.forEach { it() }
    }
    private fun ruler(canvas: Canvas,x1: Float,y1: Float,x2: Float,y2: Float,color: Int) {
        paint.style=Paint.Style.STROKE; paint.color=color; paint.strokeWidth=dp(1)
        canvas.drawLine(x1,y1,x2,y2,paint); val tick=dp(4)
        if(abs(y1-y2)<.5f) { canvas.drawLine(x1,y1-tick,x1,y1+tick,paint); canvas.drawLine(x2,y2-tick,x2,y2+tick,paint) }
        else { canvas.drawLine(x1-tick,y1,x1+tick,y1,paint); canvas.drawLine(x2-tick,y2,x2+tick,y2,paint) }
    }
    private fun label(canvas: Canvas,text: String,x: Float,y: Float,accent: Int,critical: Boolean,swatch: Int?=null): Boolean {
        val lines=text.split('\n'); val inset=if(swatch==null)dp(7) else dp(37)
        paint.typeface=Typeface.DEFAULT_BOLD; paint.textSize=dp(15)
        val w=(lines.maxOf { paint.measureText(it) }+inset+dp(7)).coerceAtMost(width-dp(8)); val h=max(if(swatch==null)0f else dp(36),if(lines.size==1)dp(32) else dp(52))
        val p=placement.place(w,h,x,y,critical) ?: return false
        val r=RectF(p.left,p.top,p.right,p.bottom)
        if(!r.contains(x,y)) { paint.style=Paint.Style.STROKE; paint.color=accent; paint.strokeWidth=dp(1); canvas.drawLine(x,y,x.coerceIn(r.left,r.right),y.coerceIn(r.top,r.bottom),paint) }
        labels.add {
        paint.style=Paint.Style.FILL; paint.color=0xff102334.toInt(); canvas.drawRoundRect(r,dp(5),dp(5),paint)
        paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(1); paint.color=accent; canvas.drawRoundRect(r,dp(5),dp(5),paint)
        paint.style=Paint.Style.FILL
        lines.forEachIndexed { i,line -> paint.color=if(i==0)Color.WHITE else 0xffb9c9d8.toInt(); paint.textSize=if(i==0)dp(15) else dp(12); paint.typeface=if(i==0)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(line,r.left+inset,r.top+dp(21)+i*dp(20),paint) }
        swatch?.let { val box=RectF(r.left+dp(6),r.centerY()-dp(12),r.left+dp(30),r.centerY()+dp(12)); paint.color=it; canvas.drawRect(box,paint); paint.color=0xffb9c9d8.toInt(); paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(1); canvas.drawRect(box,paint) }
        }
        return true
    }
    fun show()=host.add(this,host.params(-1,-1,false))
}
