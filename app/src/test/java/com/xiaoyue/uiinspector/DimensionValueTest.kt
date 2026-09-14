package com.xiaoyue.uiinspector
import com.xiaoyue.uiinspector.measurement.*
import org.junit.Assert.*
import org.junit.Test

class DimensionValueTest {
    @Test fun bothUnitsAndTrimmedDisplay() {
        val height = DimensionValue.fromPx(96f, 2f)
        assertEquals(48f, height.dp, 0f); assertEquals(96f, height.px, 0f)
        assertEquals("48 dp\n96 px", height.lines())
        assertEquals("48.5", formatDimension(48.5f)); assertEquals("48.25", formatDimension(48.25f))
    }
    @Test fun retainsFractionUntilFormatting() {
        val value = DimensionValue.fromPx(97f, 3f)
        assertEquals(97f / 3f, value.dp, 0f); assertEquals("32.33 dp / 97 px", value.label())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsZeroDensity() { DimensionValue.fromPx(96f, 0f) }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonfiniteInput() { DimensionValue.fromPx(Float.NaN, 2f) }
}
