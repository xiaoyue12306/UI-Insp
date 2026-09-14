package com.xiaoyue.uiinspector
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.util.Bounds
import org.junit.Assert.*
import org.junit.Test

class SpacingCalculatorTest {
    private val a = Bounds(100, 100, 300, 196)
    @Test fun bottomGapUsesEdgesNotCenters() {
        val b = Bounds(100, 244, 300, 340)
        assertEquals(48f, SpacingCalculator.gap(a, b, Direction.BOTTOM)!!, 0f)
        val pair = SpacingCalculator.between(a, b, 2f)
        assertEquals(PairRelation.VERTICAL, pair.relation); assertEquals(24f, pair.vertical.dp, 0f)
    }
    @Test fun horizontalAndReversedPairAreSymmetric() {
        val b = Bounds(332, 110, 432, 190)
        val pair = SpacingCalculator.between(a, b, 2f)
        assertEquals(PairRelation.HORIZONTAL, pair.relation); assertEquals(16f, pair.horizontal.dp, 0f)
        assertEquals(pair.horizontal, SpacingCalculator.between(b, a, 2f).horizontal)
    }
    @Test fun diagonalHasIndependentAxes() {
        val pair = SpacingCalculator.between(a, Bounds(320, 230, 420, 300), 2f)
        assertEquals(PairRelation.DIAGONAL, pair.relation)
        assertEquals(10f, pair.horizontal.dp, 0f); assertEquals(17f, pair.vertical.dp, 0f)
        assertNull(SpacingCalculator.gap(a, pair.b, Direction.BOTTOM))
    }
    @Test fun overlapAndTouchAreNotNegativeGaps() {
        val overlap = SpacingCalculator.between(a, Bounds(200, 150, 400, 250), 2f)
        assertEquals(PairRelation.OVERLAPPING, overlap.relation); assertEquals(0f, overlap.horizontal.px, 0f)
        assertEquals(0f, SpacingCalculator.gap(a, Bounds(300, 100, 400, 196), Direction.RIGHT)!!, 0f)
    }
    @Test fun freeformOriginDoesNotChangeItemGaps() {
        val moved = Bounds(600, 400, 800, 496)
        assertEquals(48f, SpacingCalculator.gap(moved, Bounds(600, 544, 800, 640), Direction.BOTTOM)!!, 0f)
        assertEquals(50f, SpacingCalculator.windowEdges(moved, Bounds(500, 300, 1500, 1300), 2f)[Direction.LEFT]!!.dp, 0f)
    }
}
