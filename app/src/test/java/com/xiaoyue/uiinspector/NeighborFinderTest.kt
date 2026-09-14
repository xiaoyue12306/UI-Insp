package com.xiaoyue.uiinspector
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.inspector.NodeSnapshot
import com.xiaoyue.uiinspector.util.Bounds
import org.junit.Assert.*
import org.junit.Test

class NeighborFinderTest {
    private val selected = item()
    private val window = Bounds(0, 0, 1000, 1000)
    private fun find(vararg nodes: NodeSnapshot) = NeighborFinder().find(selected, listOf(selected) + nodes, window)
    @Test fun allFourDirections() {
        val result = find(item(1, Bounds(100, 20, 300, 52)), item(2, Bounds(100, 244, 300, 340)),
            item(3, Bounds(20, 100, 68, 196)), item(4, Bounds(332, 100, 400, 196)))
        assertEquals(4, result.size)
        assertEquals(24f, result[Direction.TOP]!!.distance.dp, 0f)
        assertEquals(24f, result[Direction.BOTTOM]!!.distance.dp, 0f)
        assertEquals(16f, result[Direction.LEFT]!!.distance.dp, 0f)
        assertEquals(16f, result[Direction.RIGHT]!!.distance.dp, 0f)
    }
    @Test fun positivePartialProjectionAccepted() {
        assertEquals(1, find(item(1, Bounds(150, 220, 250, 300)))[Direction.BOTTOM]!!.node.index)
    }
    @Test fun diagonalAndCornerSliverCannotStealBottomNeighbor() {
        val result = find(item(1, Bounds(301, 197, 330, 210)), item(2, Bounds(299, 197, 399, 250)), item(3, Bounds(100, 244, 300, 300)))
        assertEquals(3, result[Direction.BOTTOM]!!.node.index)
    }
    @Test fun rejectsContainersChildrenAndSameBounds() {
        assertTrue(find(item(1, Bounds(0, 0, 500, 500)), item(2, Bounds(120, 120, 280, 170)), item(3)).isEmpty())
    }
    @Test fun rejectsParentAndChildEvenWithMisleadingGeometry() {
        val parent = item(1, Bounds(100, 20, 300, 80))
        val child = item(2, Bounds(100, 210, 300, 300)).copy(parentIndex = 0)
        assertTrue(NeighborFinder().find(selected.copy(parentIndex = 1), listOf(selected.copy(parentIndex = 1), parent, child), window).isEmpty())
    }
    @Test fun rejectsHiddenEmptyInspectorAndDifferentWindow() {
        val b = Bounds(100, 210, 300, 300)
        assertTrue(find(item(1,b).copy(visibleToUser=false), item(2, Bounds(100,210,100,300)),
            item(3,b).copy(packageName="com.xiaoyue.uiinspector"), item(4,b).copy(windowId=2)).isEmpty())
    }
    @Test fun nearestReliableThenBestOverlapWins() {
        val result = find(item(1, Bounds(100, 260, 300, 300)), item(2, Bounds(150, 220, 250, 240)), item(3, Bounds(100, 220, 300, 240)))
        assertEquals(3, result[Direction.BOTTOM]!!.node.index)
        assertTrue(result[Direction.BOTTOM]!!.confidence > .9f)
    }
    @Test fun retainsDividerButRejectsSpeckAndClippedBounds() {
        val result = find(item(1, Bounds(100, 220, 300, 221)), item(2, Bounds(180, 200, 181, 201)), item(3, Bounds(-20, 210, 300, 230)))
        assertEquals(1, result[Direction.BOTTOM]!!.node.index)
    }
}
