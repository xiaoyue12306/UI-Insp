package com.xiaoyue.uiinspector.util
import java.util.Locale
fun pxToDp(px: Int, density: Float): Float { require(density > 0); return px / density }
fun dpToPx(dp: Float, density: Float): Float { require(density > 0); return dp * density }
fun formatDp(value: Float): String = String.format(Locale.US, "%.1f", value).removeSuffix(".0")
