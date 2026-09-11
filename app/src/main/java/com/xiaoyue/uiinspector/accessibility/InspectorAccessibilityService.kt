package com.xiaoyue.uiinspector.accessibility
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
class InspectorAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { AccessibilityServiceState.service = this; AccessibilityServiceState.connected.value = true }
    fun startInspector() { AccessibilityServiceState.running.value = true }
    fun stopInspector() { AccessibilityServiceState.running.value = false }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { stopInspector() }
    override fun onDestroy() { stopInspector(); AccessibilityServiceState.service = null; AccessibilityServiceState.connected.value = false; super.onDestroy() }
}
