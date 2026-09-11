package com.xiaoyue.uiinspector.inspector

object NodeFinder {
    /** Lexicographic score: visibility, smallest area, deepest node, useful semantics. */
    val ranking = compareByDescending<NodeSnapshot> { it.visibleToUser }
        .thenBy { it.bounds.area }.thenByDescending { it.depth }
        .thenByDescending { (if (!it.resourceId.isNullOrBlank()) 4 else 0) +
            (if (it.clickable) 2 else 0) + (if (!it.text.isNullOrBlank()) 1 else 0) +
            (if (!it.contentDescription.isNullOrBlank()) 1 else 0) }
        .thenBy { it.index }
    fun candidates(nodes: List<NodeSnapshot>, x: Int, y: Int) =
        nodes.filter { it.bounds.contains(x, y) && it.bounds.area > 0 }.sortedWith(ranking)
}
