package com.xiaoyue.uiinspector

import com.xiaoyue.uiinspector.measurement.DimensionValue
import com.xiaoyue.uiinspector.interaction.PresentationPreferences
import com.xiaoyue.uiinspector.interaction.PrimaryUnit
import org.junit.Assert.*
import org.junit.Test

class PixelRoundingTest {
    @Test fun vantageLogoKeepsActualPixelsAndMarksEstimatedDp() {
        val value = DimensionValue.fromPx(61f, 306f / 160f)
        assertEquals(61f, value.px, 0f)
        assertEquals(31.895424f, value.dp, .00001f)
        assertEquals("≈32", value.displayDp())
        assertEquals("31.9 dp\n61 px", PresentationPreferences().lines(value, exact=true))
        assertEquals("61 px\n≈32 dp", PresentationPreferences(PrimaryUnit.PX).lines(value))
    }
    @Test fun doesNotRoundBeyondHalfPixelOrInflateSubpixelSizes() {
        assertEquals("31.37", DimensionValue.fromPx(60f, 1.9125f).displayDp())
        assertEquals("0.33", DimensionValue.fromPx(1f, 3f).displayDp())
        assertEquals("47.58", DimensionValue.fromPx(91f, 1.9125f).displayDp())
    }
    @Test fun exactDimensionsAndZeroHaveNoApproximationMarker() {
        assertEquals("32", DimensionValue.fromPx(64f, 2f).displayDp())
        assertEquals("0", DimensionValue.fromPx(0f, 2f).displayDp())
    }
}
