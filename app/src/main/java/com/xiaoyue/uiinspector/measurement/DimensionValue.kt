package com.xiaoyue.uiinspector.measurement

import java.util.Locale

data class DimensionValue(val px: Float, val dp: Float) {
    companion object {
        fun fromPx(px: Float, density: Float): DimensionValue {
            require(density.isFinite() && density > 0 && px.isFinite())
            return DimensionValue(px, px / density)
        }
    }
    fun label() = "${formatDimension(dp)} dp / ${formatDimension(px)} px"
    fun lines() = "${formatDimension(dp)} dp\n${formatDimension(px)} px"
}

fun formatDimension(value: Float): String = String.format(Locale.US, "%.2f", value)
    .trimEnd('0').trimEnd('.').let { if (it == "-0") "0" else it }

enum class Direction { TOP, BOTTOM, LEFT, RIGHT }
