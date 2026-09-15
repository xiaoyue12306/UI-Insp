package com.xiaoyue.uiinspector.interaction
import com.xiaoyue.uiinspector.inspector.NodeSnapshot

/** Candidate navigation follows visual area at the same point, not accessibility parentage. */
object SelectionAlternatives {
    fun smaller(current: NodeSnapshot,candidates: List<NodeSnapshot>)=candidates.filter { it.visibleToUser && it.bounds.area>0 && it.bounds.area<current.bounds.area }.maxByOrNull { it.bounds.area }
    fun larger(current: NodeSnapshot,candidates: List<NodeSnapshot>)=candidates.filter { it.visibleToUser && it.bounds.area>current.bounds.area }.minByOrNull { it.bounds.area }
}
