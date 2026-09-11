package com.xiaoyue.uiinspector.inspector
import kotlinx.coroutines.flow.MutableStateFlow
sealed interface InspectorState {
    data object Stopped : InspectorState
    data object Idle : InspectorState
    data object Selecting : InspectorState
    data class Selected(val node: NodeSnapshot) : InspectorState
}
class InspectModeManager {
    val state = MutableStateFlow<InspectorState>(InspectorState.Stopped)
}
