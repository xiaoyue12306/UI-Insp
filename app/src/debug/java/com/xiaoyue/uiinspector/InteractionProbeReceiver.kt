package com.xiaoyue.uiinspector
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xiaoyue.uiinspector.accessibility.AccessibilityServiceState
import com.xiaoyue.uiinspector.interaction.*
import java.io.File

/** Read-only debug probe for ADB touch tests. No activity launch or permission changes. */
class InteractionProbeReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val service=AccessibilityServiceState.service
        val report=buildString {
            appendLine("connected=${service!=null}")
            if(service!=null) {
                val field=service.javaClass.getDeclaredField("controller").apply { isAccessible=true }
                val controller=field.get(service) as? InspectorInteractionController
                val state=controller?.state?.value
                appendLine("state=${state?.javaClass?.simpleName}")
                if(state is InspectorUiState.ShowingResult) {
                    appendLine("details=${state.detailsExpanded}"); appendLine("stale=${state.analysis.stale}")
                    appendLine("bounds=${state.analysis.boundsPx}"); appendLine("color=${state.analysis.renderedColor?.colors?.dominantHex}")
                    appendLine("pair=${state.analysis.pair?.relation}")
                    if(state.analysis.node.packageName=="com.xiaoyue.inspectorfixture") appendLine("fixtureId=${state.analysis.node.resourceId}")
                }
                if(state is InspectorUiState.Selecting) appendLine("purpose=${state.purpose}")
                if(state is InspectorUiState.PickedColor) appendLine("color=${state.color}")
                val prefs=InspectorPreferences(context); appendLine("bubbleRight=${prefs.bubbleRight}"); appendLine("bubbleY=${prefs.bubbleY}"); appendLine("preferences=${prefs.read()}")
            }
        }
        File(context.filesDir,"interaction-validation.txt").writeText(report)
    }
}
