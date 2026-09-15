package com.xiaoyue.uiinspector
import com.xiaoyue.uiinspector.interaction.*
import com.xiaoyue.uiinspector.analysis.SelectedItemAnalyzer
import com.xiaoyue.uiinspector.inspector.SelectionPurpose
import com.xiaoyue.uiinspector.measurement.DimensionValue
import com.xiaoyue.uiinspector.util.Bounds
import org.junit.Assert.*
import org.junit.Test

class InteractionPolicyTest {
    private fun analysis()=SelectedItemAnalyzer({ _,_->error("unused") }).measure(item(),tree(item()))
    @Test fun backCollapsesBeforeHidingAndNeverStopsService() {
        val details=InspectorUiState.ShowingResult(analysis(),true)
        val quick=details.back() as InspectorUiState.ShowingResult
        assertFalse(quick.detailsExpanded); assertSame(details.analysis,quick.analysis)
        assertEquals(InspectorUiState.Idle,quick.back()); assertEquals(InspectorUiState.Idle,InspectorUiState.Idle.back())
    }
    @Test fun selectingAndSecondaryToolsCancelToIdle() {
        SelectionPurpose.entries.forEach { assertEquals(InspectorUiState.Idle,InspectorUiState.Selecting(it).back()) }
        assertEquals(InspectorUiState.Idle,InspectorUiState.PickedColor(null,"blocked").back())
    }
    @Test fun menusRestoreSavedResult() {
        val result=InspectorUiState.ShowingResult(analysis())
        assertEquals(result,InspectorUiState.Menu(result).back()); assertEquals(result,InspectorUiState.Settings(result).back())
    }
    @Test fun largerSmallerSkipDuplicateBoundsAndFollowSamePointCandidates() {
        val small=item(1,Bounds(120,120,150,150)); val middle=item(2); val large=item(3,Bounds(0,0,500,500))
        val candidates=listOf(small,middle,middle.copy(index=4),large)
        assertEquals(large,SelectionAlternatives.larger(middle,candidates))
        assertEquals(small,SelectionAlternatives.smaller(middle,candidates))
        assertNull(SelectionAlternatives.smaller(small,candidates)); assertNull(SelectionAlternatives.larger(large,candidates))
    }
    @Test fun primaryUnitNeverHidesOtherUnit() {
        val value=DimensionValue.fromPx(96f,2f)
        assertEquals("48 dp\n96 px",PresentationPreferences().lines(value))
        assertEquals("96 px\n48 dp",PresentationPreferences(primaryUnit=PrimaryUnit.PX).lines(value))
        assertFalse(PresentationPreferences().showAllSpacing); assertFalse(PresentationPreferences().expandedResults)
    }
}
