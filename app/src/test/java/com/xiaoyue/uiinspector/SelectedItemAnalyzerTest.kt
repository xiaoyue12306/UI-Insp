package com.xiaoyue.uiinspector
import com.xiaoyue.uiinspector.analysis.*
import com.xiaoyue.uiinspector.measurement.Direction
import com.xiaoyue.uiinspector.util.Bounds
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SelectedItemAnalyzerTest {
    @Test fun measurementReadyBeforeColorCapture() = runBlocking {
        var calls = 0
        val analyzer = SelectedItemAnalyzer({ _, _ -> calls++; ItemColorAnalysis(null, "secure") })
        val node = item(); val snapshot = tree(node, item(1, Bounds(100,244,300,340)))
        val measured = analyzer.measure(node, snapshot)
        assertEquals(0, calls); assertNull(measured.renderedColor)
        assertEquals(48f, measured.height.dp, 0f); assertEquals(24f, measured.neighbors[Direction.BOTTOM]!!.distance.dp, 0f)
        val complete = analyzer.color(measured, snapshot)
        assertEquals(1, calls); assertEquals(measured.width, complete.width); assertEquals("secure", complete.renderedColor!!.status)
    }
    @Test fun lateAAndBCannotOverwriteC() = runBlocking {
        val gates = List(3) { CompletableDeferred<Unit>() }
        val generation = SelectionGeneration()
        val analyzer = SelectedItemAnalyzer({ node, _ -> gates[node.index].await(); ItemColorAnalysis(null, "color ${node.index}") })
        var published: SelectedItemAnalysis? = null
        val jobs = (0..2).map { index ->
            val token = generation.next(); val node = item(index); val snapshot = tree(node)
            launch { val complete = analyzer.color(analyzer.measure(node, snapshot), snapshot)
                if (generation.accepts(token)) published = complete }
        }
        gates[2].complete(Unit); jobs[2].join()
        gates[1].complete(Unit); gates[0].complete(Unit); jobs.joinAll()
        assertEquals(2, published!!.node.index); assertEquals("color 2", published!!.renderedColor!!.status)
    }
    @Test fun freezeInvalidatesPendingGenerationAndOwnsPixels() {
        val generation = SelectionGeneration(); val token = generation.next(); generation.next()
        assertFalse(generation.accepts(token))
        val bytes = byteArrayOf(1,2,3); val screenshot = CapturedItemImage(bytes)
        bytes[0] = 9; screenshot.copyPng()[1] = 9
        assertArrayEquals(byteArrayOf(1,2,3), screenshot.copyPng())
        val analysis = SelectedItemAnalyzer({ _, _ -> error("unused") }).measure(item(), tree(item())).copy(frozen=true)
        assertTrue(analysis.summary().contains("96 px")); assertTrue(analysis.summary().contains("Frozen"))
    }
}
