package com.xiaoyue.uiinspector.interaction

import android.content.Context
import com.xiaoyue.uiinspector.measurement.DimensionValue
import com.xiaoyue.uiinspector.measurement.formatDimension

enum class PrimaryUnit { DP, PX }
data class PresentationPreferences(val primaryUnit: PrimaryUnit = PrimaryUnit.DP, val showAllSpacing: Boolean = false, val expandedResults: Boolean = false) {
    fun lines(value: DimensionValue, exact: Boolean = false): String {
        val dp = if(exact) formatDimension(value.dp) else value.displayDp()
        val px = formatDimension(value.px)
        return if(primaryUnit == PrimaryUnit.DP) "$dp dp\n$px px" else "$px px\n$dp dp"
    }
    fun size(width: DimensionValue, height: DimensionValue): String {
        val dp = "${width.displayDp()} × ${height.displayDp()} dp"
        val px = "${formatDimension(width.px)} × ${formatDimension(height.px)} px"
        return if (primaryUnit == PrimaryUnit.DP) "$dp\n$px" else "$px\n$dp"
    }
}

class InspectorPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("inspector-interaction", Context.MODE_PRIVATE)
    fun read() = PresentationPreferences(if (prefs.getString("primary", "DP") == "PX") PrimaryUnit.PX else PrimaryUnit.DP,
        prefs.getBoolean("all-spacing", false), prefs.getBoolean("expanded", false))
    fun save(value: PresentationPreferences) { prefs.edit().putString("primary", value.primaryUnit.name)
        .putBoolean("all-spacing", value.showAllSpacing).putBoolean("expanded", value.expandedResults)
        .putBoolean("secondary-unit", true).apply() }
    var welcomeSeen: Boolean
        get() = prefs.getBoolean("welcome", false)
        set(value) { prefs.edit().putBoolean("welcome", value).apply() }
    var bubbleRight: Boolean
        get() = prefs.getBoolean("bubble-right", false)
        set(value) { prefs.edit().putBoolean("bubble-right", value).apply() }
    var bubbleY: Float
        get() = prefs.getFloat("bubble-y", .25f)
        set(value) { prefs.edit().putFloat("bubble-y", value.coerceIn(0f, 1f)).apply() }
}
