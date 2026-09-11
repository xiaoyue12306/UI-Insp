package com.xiaoyue.uiinspector
import com.xiaoyue.uiinspector.color.*
import com.xiaoyue.uiinspector.inspector.*
import com.xiaoyue.uiinspector.util.*
import org.junit.Assert.*
import org.junit.Test

class CoreLogicTest {
    private fun node(index: Int = 0, bounds: Bounds = Bounds(0, 0, 100, 100), depth: Int = 0,
                     visible: Boolean = true, id: String? = null, text: String? = null, description: String? = null) = NodeSnapshot(
        index, null, depth, "test.app", id, "android.widget.Button", text, description, bounds, 2f,
        false, true, true, false, false, false, false, false, false, visible, false, emptyList(), 1, 0)
    @Test fun densityConversion() { assertEquals(48f, pxToDp(96, 2f), 0f); assertEquals(96f, dpToPx(48f, 2f), 0f); assertEquals("48", formatDp(48f)); assertEquals("48.3", formatDp(48.25f)) }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidDensity() { pxToDp(20, 0f) }
    @Test fun clipsNegativeBounds() { assertEquals(Bounds(0, 0, 10, 20), Bounds(-5, -7, 10, 20).clip(Bounds(0, 0, 100, 100))) }
    @Test fun disjointAndZeroBounds() { assertNull(Bounds(-10, -10, 0, 0).clip(Bounds(0, 0, 100, 100))); assertFalse(Bounds(0, 0, 10, 10).contains(10, 5)) }
    @Test fun areaDoesNotOverflow() { assertEquals(10000000000L, Bounds(0, 0, 100000, 100000).area) }
    @Test fun visibleBeforeSmallerInvisible() {
        val big = node(index = 1); val hidden = node(index = 2, bounds = Bounds(2, 2, 8, 8), visible = false)
        assertEquals(big, NodeFinder.candidates(listOf(hidden, big), 5, 5).first())
    }
    @Test fun smallestThenDeepest() {
        val root = node(index = 0); val shallow = node(index = 1, bounds = Bounds(0, 0, 20, 20), depth = 1)
        val deep = shallow.copy(index = 2, depth = 3)
        assertEquals(listOf(deep, shallow, root), NodeFinder.candidates(listOf(root, shallow, deep), 5, 5))
    }
    @Test fun filtersMissesAndUsesSemanticTieBreak() {
        val plain = node(index = 1); val identified = node(index = 2, id = "test:id/save")
        assertEquals(identified, NodeFinder.candidates(listOf(plain, identified), 5, 5).first())
        assertTrue(NodeFinder.candidates(listOf(plain), 101, 101).isEmpty())
    }
    @Test fun quantizesChannels() { assertEquals(0xc0c0c8, ColorAnalyzer().quantize(0xffc7c6ca.toInt())) }
    @Test fun preservesExactFlatColor() {
        val result = ColorAnalyzer().analyze(525, 144, 262, 72) { _, _ -> 0xffc7c6ca.toInt() }
        assertEquals("#C7C6CA", result.dominantHex); assertEquals("#C7C6CA", result.centerHex); assertEquals(1.0, result.topColors[0].fraction, 0.0)
    }
    @Test fun ignoresTextAtCenterAndOuterEdge() {
        val result = ColorAnalyzer().analyze(100, 100, 50, 50) { x, y ->
            if (x < 9 || y < 14 || (x in 40..60 && y in 45..55)) 0xff202124.toInt() else 0xffc7c6ca.toInt()
        }
        assertEquals("#202124", result.centerHex); assertEquals("#C7C6CA", result.dominantHex)
    }
    @Test fun handlesOnePixelAndInvalidCenter() {
        val result = ColorAnalyzer().analyze(1, 1, -1, 4) { _, _ -> 0xff123456.toInt() }
        assertNull(result.centerColor); assertEquals("#123456", result.dominantHex)
        assertTrue(ColorAnalyzer().analyze(0, 0, null, null) { _, _ -> error("Must not sample") }.topColors.isEmpty())
    }
    @Test fun transparentPixelsDoNotDominate() { assertNull(ColorAnalyzer().analyze(10, 10, 5, 5) { _, _ -> 0 }.dominantColor) }
    @Test fun fragmentedColorsHaveNoDominant() {
        val palette = intArrayOf(0xff000000.toInt(), 0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt())
        val result = ColorAnalyzer(ColorConfig(horizontalInset = 0.0, verticalInset = 0.0)).analyze(4, 4, 0, 0) { x, _ -> palette[x] }
        assertNull(result.dominantColor); assertEquals(3, result.topColors.size); assertEquals(.25, result.topColors.first().fraction, 0.0)
    }
    @Test fun boundsSamplingWork() {
        var reads = 0
        ColorAnalyzer().analyze(4000, 3000, null, null) { _, _ -> reads++; 0xffc7c6ca.toInt() }
        assertTrue("Sampling must be bounded", reads <= 4300)
    }
    @Test fun hexHasLeadingZeros() { assertEquals("#00000A", rgbHex(0xff00000a.toInt())) }
    @Test fun locatorPriorityAndEscaping() {
        val result = LocatorUtils.appium(node(id = "test:id/a\"b\\c\n", description = "fallback"))
        assertTrue(result.contains("AppiumBy.ID")); assertTrue(result.contains("a\\\"b\\\\c\\n")); assertFalse(result.contains("ACCESSIBILITY_ID"))
    }
    @Test fun accessibilityLocatorAndTextFallback() {
        assertTrue(LocatorUtils.appium(node(description = "Save")).contains("AppiumBy.ACCESSIBILITY_ID"))
        val result = LocatorUtils.appium(node(text = "Save")); assertTrue(result.contains("No stable ID found")); assertFalse(result.contains("XPath"))
    }
}
