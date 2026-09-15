package com.xiaoyue.uiinspector
import com.xiaoyue.uiinspector.overlay.*
import com.xiaoyue.uiinspector.measurement.*
import org.junit.Assert.*
import org.junit.Test
class OverlayPresentationTest {
    private fun neighbor(dir: Direction,gap: Float)=NeighborMeasurement(item(dir.ordinal),dir,DimensionValue.fromPx(gap*2,2f),1f,.95f)
    @Test fun verticalLayoutShowsVerticalOnlyButRetainsAllData() {
        val all=listOf(neighbor(Direction.TOP,16f),neighbor(Direction.BOTTOM,24f),neighbor(Direction.LEFT,100f),neighbor(Direction.RIGHT,100f)).associateBy { it.direction }
        assertEquals(LayoutDirectionHint.VERTICAL,SpacingPresentation.hint(all.values))
        assertEquals(setOf(Direction.TOP,Direction.BOTTOM),SpacingPresentation.visible(all,false).map { it.direction }.toSet())
        assertEquals(4,SpacingPresentation.visible(all,true).size); assertEquals(4,all.size)
    }
    @Test fun horizontalLayoutAndMixedNearestTwo() {
        val all=listOf(neighbor(Direction.TOP,100f),neighbor(Direction.BOTTOM,100f),neighbor(Direction.LEFT,12f),neighbor(Direction.RIGHT,16f)).associateBy { it.direction }
        assertEquals(LayoutDirectionHint.HORIZONTAL,SpacingPresentation.hint(all.values))
        assertEquals(2,SpacingPresentation.visible(all,false).size)
        assertTrue(SpacingPresentation.visible(emptyMap(),false).isEmpty())
    }
    @Test fun cardsAvoidTopAndBottomSelectedItems() {
        val screen=PlacementRect(0f,0f,400f,800f)
        val top=PlacementRect(50f,50f,350f,200f); val bottom=PlacementRect(50f,600f,350f,750f)
        val a=ResultCardPlacement.place(screen,top,250f,180f,8f); val b=ResultCardPlacement.place(screen,bottom,250f,180f,8f)
        assertFalse(a.intersects(top)); assertFalse(b.intersects(bottom)); assertTrue(a.top>b.top)
    }
    @Test fun labelsStayOutsideTinyItemAndDoNotOverlap() {
        val item=PlacementRect(180f,380f,204f,404f)
        val engine=LabelPlacementEngine(PlacementRect(0f,0f,400f,800f),item,emptyList(),emptyList(),8f)
        val size=engine.place(110f,40f,192f,380f,true)!!
        val spacing=engine.place(100f,40f,192f,380f,false)!!
        assertFalse(size.intersects(item)); assertFalse(spacing.intersects(item)); assertFalse(size.intersects(spacing))
    }
    @Test fun impossibleSpacingIsOmittedInsteadOfOverlappingSize() {
        val screen=PlacementRect(0f,0f,100f,100f)
        val engine=LabelPlacementEngine(screen,screen,emptyList(),emptyList(),4f)
        assertNotNull(engine.place(70f,35f,50f,20f,true))
        assertNull(engine.place(70f,35f,50f,20f,false))
    }
    @Test fun labelsAvoidReservedCardAtScreenEdge() {
        val card=PlacementRect(200f,400f,400f,800f)
        val engine=LabelPlacementEngine(PlacementRect(0f,0f,400f,800f),PlacementRect(350f,380f,390f,420f),emptyList(),listOf(card),8f)
        val p=engine.place(120f,40f,400f,400f,true)!!
        assertFalse(p.intersects(card)); assertTrue(p.left>=0f && p.right<=400f)
    }
}
