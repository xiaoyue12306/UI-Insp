package com.xiaoyue.uiinspector.accessibility
import kotlinx.coroutines.flow.MutableStateFlow
object AccessibilityServiceState {
    val connected = MutableStateFlow(false)
    val running = MutableStateFlow(false)
    var service: InspectorAccessibilityService? = null
        internal set
}
