package com.xiaoyue.uiinspector.interaction

import com.xiaoyue.uiinspector.analysis.SelectedItemAnalysis
import com.xiaoyue.uiinspector.inspector.SelectionPurpose

sealed interface InspectorUiState {
    data object Stopped : InspectorUiState
    data object Idle : InspectorUiState
    data object Welcome : InspectorUiState
    data class Selecting(val purpose: SelectionPurpose, val anchor: SelectedItemAnalysis? = null) : InspectorUiState
    data class ShowingResult(val analysis: SelectedItemAnalysis, val detailsExpanded: Boolean = false, val advancedExpanded: Boolean = false) : InspectorUiState
    data class PickedColor(val color: Int?, val message: String) : InspectorUiState
    data class Menu(val previous: InspectorUiState) : InspectorUiState
    data class Settings(val previous: InspectorUiState) : InspectorUiState
}

fun InspectorUiState.back(): InspectorUiState = when (this) {
    is InspectorUiState.ShowingResult -> if (detailsExpanded) copy(detailsExpanded = false) else InspectorUiState.Idle
    is InspectorUiState.Menu -> previous
    is InspectorUiState.Settings -> previous
    InspectorUiState.Stopped, InspectorUiState.Idle -> this
    else -> InspectorUiState.Idle
}
