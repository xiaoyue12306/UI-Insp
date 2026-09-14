package com.xiaoyue.uiinspector.inspector
import kotlinx.coroutines.flow.MutableStateFlow
import com.xiaoyue.uiinspector.analysis.SelectedItemAnalysis
enum class SelectionPurpose { ITEM, PAIR_A, PAIR_B, PIXEL }
sealed interface InspectorState {
    data object Stopped : InspectorState
    data object Idle : InspectorState
    data class Selecting(val purpose: SelectionPurpose = SelectionPurpose.ITEM, val anchor: SelectedItemAnalysis? = null) : InspectorState
    data class Selected(val analysis: SelectedItemAnalysis) : InspectorState
    data class PickedColor(val x: Int, val y: Int, val color: Int?, val message: String) : InspectorState
}
class InspectModeManager {
    val state = MutableStateFlow<InspectorState>(InspectorState.Stopped)
}
