package com.xiaoyue.uiinspector.measurement

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class DimensionValue(val px: Float, val dp: Float) {
    companion object {
        fun fromPx(px: Float, density: Float): DimensionValue {
            require(density.isFinite() && density > 0 && px.isFinite())
            return DimensionValue(px, px / density)
        }
    }
    fun label() = "${formatDimension(dp)} dp / ${formatDimension(px)} px"
    /** An integer dp within half a rendered pixel is an estimate, never a recovered layout value. */
    fun displayDp(): String {
        if (px <= 0f || dp <= 0f) return formatDimension(dp)
        val nearest = round(dp)
        val formatted = formatDimension(dp)
        return if (nearest > 0f && formatted != formatDimension(nearest) && abs(dp-nearest)*(px/dp) <= .5001f)
            "≈${formatDimension(nearest)}" else formatted
    }
    fun lines() = "${displayDp()} dp\n${formatDimension(px)} px"
}

fun formatDimension(value: Float): String = String.format(Locale.US, "%.2f", value)
    .trimEnd('0').trimEnd('.').let { if (it == "-0") "0" else it }

enum class Direction { TOP, BOTTOM, LEFT, RIGHT }
