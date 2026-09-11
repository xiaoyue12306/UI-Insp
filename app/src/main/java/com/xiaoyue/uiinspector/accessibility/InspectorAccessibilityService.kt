package com.xiaoyue.uiinspector.accessibility
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.res.Configuration
import com.xiaoyue.uiinspector.inspector.InspectorController
import com.xiaoyue.uiinspector.inspector.InspectorState
import kotlinx.coroutines.*
class InspectorAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: InspectorController? = null
    override fun onServiceConnected() {
        AccessibilityServiceState.service = this; AccessibilityServiceState.connected.value = true
        controller = InspectorController(this)
        scope.launch { controller!!.mode.state.collect { AccessibilityServiceState.running.value = it !is InspectorState.Stopped } }
    }
    fun startInspector() { controller?.start() }
    fun stopInspector() { controller?.stop(); AccessibilityServiceState.running.value = false }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { if (event != null) controller?.onEvent(event) }
    override fun onInterrupt() { stopInspector() }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); if (AccessibilityServiceState.running.value) controller?.start() }
    override fun onDestroy() { controller?.destroy(); scope.cancel(); AccessibilityServiceState.running.value = false; AccessibilityServiceState.service = null; AccessibilityServiceState.connected.value = false; super.onDestroy() }
}
