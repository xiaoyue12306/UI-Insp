package com.xiaoyue.uiinspector.interaction

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.CheckBox
import android.widget.Toast
import com.xiaoyue.uiinspector.analysis.*
import com.xiaoyue.uiinspector.color.rgbHex
import com.xiaoyue.uiinspector.inspector.*
import com.xiaoyue.uiinspector.measurement.*
import com.xiaoyue.uiinspector.overlay.*
import com.xiaoyue.uiinspector.screenshot.*
import com.xiaoyue.uiinspector.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/** Interaction only; the existing tree, neighbor, screenshot and color engines remain unchanged. */
class InspectorInteractionController(private val service: AccessibilityService) {
    val state=MutableStateFlow<InspectorUiState>(InspectorUiState.Stopped)
    private val host=OverlayController(service)
    private val prefs=InspectorPreferences(service)
    private val panel=InspectorPanelOverlay(service,host) { back() }
    private val screenshots=ScreenshotProvider(service)
    private val analyzer=SelectedItemAnalyzer(ItemColorCapture(screenshots,host::hideAll,host::showAll)::capture)
    private val generation=SelectionGeneration()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate+CoroutineExceptionHandler { _,e ->
        Log.e("UIInspector","Inspection failed",e); hideResult(); toast("Could not inspect that item. Tap the bubble to try again.")
    })
    private var selectionJob: Job?=null
    private var colorJob: Job?=null
    private var invalidationJob: Job?=null
    private var previewJob: Job?=null
    private var bubble: FloatingBubbleController?=null
    private var capture: TouchCaptureOverlay?=null
    private var measurement: MeasurementOverlay?=null
    private var hint: View?=null
    private var lens: PixelMagnifierOverlay?=null
    private var lensBitmap: Bitmap?=null
    private var tree: NodeTree?=null
    private var candidates: List<NodeSnapshot> = emptyList()
    private fun result(s: InspectorUiState=state.value): InspectorUiState.ShowingResult?=when(s) {
        is InspectorUiState.ShowingResult -> s
        is InspectorUiState.Menu -> result(s.previous)
        is InspectorUiState.Settings -> result(s.previous)
        else -> null
    }
    private fun replaceResult(s: InspectorUiState,a: SelectedItemAnalysis): InspectorUiState=when(s) {
        is InspectorUiState.ShowingResult -> s.copy(analysis=a)
        is InspectorUiState.Menu -> s.copy(previous=replaceResult(s.previous,a))
        is InspectorUiState.Settings -> s.copy(previous=replaceResult(s.previous,a))
        else -> s
    }
    private fun toast(message: String)=Toast.makeText(service,message,Toast.LENGTH_SHORT).show()
    fun start() {
        stop(); bubble=FloatingBubbleController(service,host,prefs,::select,::menu)
        if(bubble?.show()!=true) return
        state.value=if(prefs.welcomeSeen) InspectorUiState.Idle else InspectorUiState.Welcome
        render()
    }
    fun select() {
        if(state.value is InspectorUiState.Selecting) { hideResult(); return }
        beginSelection(SelectionPurpose.ITEM)
    }
    private fun pair()=beginSelection(SelectionPurpose.PAIR_A)
    private fun picker()=beginSelection(SelectionPurpose.PIXEL)
    private fun menu() {
        if(state.value is InspectorUiState.Selecting) { hideResult(); return }
        state.value=InspectorUiState.Menu(if(state.value is InspectorUiState.Menu) (state.value as InspectorUiState.Menu).previous else state.value)
        render()
    }
    private fun settings() {
        val previous=(state.value as? InspectorUiState.Menu)?.previous ?: state.value
        state.value=InspectorUiState.Settings(previous); render()
    }
    fun back(): Boolean {
        if(state.value==InspectorUiState.Stopped || state.value==InspectorUiState.Idle) return false
        if(state.value is InspectorUiState.Selecting) { hideResult(); return true }
        if(state.value==InspectorUiState.Welcome) prefs.welcomeSeen=true
        if((state.value as? InspectorUiState.ShowingResult)?.detailsExpanded==true) prefs.save(prefs.read().copy(expandedResults=false))
        val next=state.value.back()
        if(next==InspectorUiState.Idle) hideResult() else { state.value=next; render() }
        return true
    }
    private fun clearCapture() {
        capture?.remove(); capture=null; host.remove(hint); hint=null
        previewJob?.cancel(); previewJob=null; host.remove(lens); lens=null
        lensBitmap?.recycle(); lensBitmap=null
    }
    private fun beginSelection(purpose: SelectionPurpose,anchor: SelectedItemAnalysis?=null) {
        val token=generation.next(); selectionJob?.cancel(); colorJob?.cancel(); invalidationJob?.cancel()
        clearCapture(); panel.remove(); host.clear(); measurement=null
        state.value=InspectorUiState.Selecting(purpose,anchor)
        capture=TouchCaptureOverlay(service,host,::pick,{ back() }) { _,x,y -> lens?.point(x,y) }
        if(capture?.show()!=true) { stop(); return }
        bubble?.selecting(true); bubble?.show()
        val text=when(purpose) { SelectionPurpose.PAIR_A -> "Select first item"; SelectionPurpose.PAIR_B -> "A ✓  Select second item"; SelectionPurpose.PIXEL -> "Tap a color · hold to magnify"; else -> "Tap an item to inspect" }
        val row=android.widget.LinearLayout(service).apply {
            setPadding(host.dp(10),host.dp(4),host.dp(10),host.dp(4)); background=service.rounded(0xf0f0f5fa.toInt())
            line(text,14f); action("×") { back() }
        }
        hint=row
        host.add(row,host.params(host.dp(250),-2).apply { x=(host.manager.maximumWindowMetrics.bounds.width()-width)/2; y=host.dp(40) })
        if(purpose==SelectionPurpose.PIXEL) previewJob=scope.launch {
            val b=host.manager.maximumWindowMetrics.bounds; val bounds=Bounds(b.left,b.top,b.right,b.bottom)
            val shot=screenshots.capture(null,bounds,bounds,host::hideAll,host::showAll)
            if(shot is ScreenshotResult.Success) {
                if(!generation.accepts(token) || state.value !is InspectorUiState.Selecting) { shot.bitmap.recycle(); return@launch }
                lensBitmap=shot.bitmap; lens=PixelMagnifierOverlay(service,host,shot.bitmap).also { it.show() }
            }
        }
        selectionJob=scope.launch { delay(30_000); if(state.value is InspectorUiState.Selecting) hideResult() }
    }
    private fun pick(x: Int,y: Int) {
        val selecting=state.value as? InspectorUiState.Selecting ?: return
        selectionJob?.cancel(); clearCapture(); bubble?.selecting(false)
        val token=generation.next()
        selectionJob=scope.launch {
            delay(100)
            val found=withContext(Dispatchers.Default) { NodeTreeBuilder(service).at(x,y) }
            if(!generation.accepts(token)) return@launch
            if(selecting.purpose==SelectionPurpose.PIXEL) { samplePixel(x,y,found,token); return@launch }
            tree=found; candidates=NodeFinder.candidates(found?.nodes.orEmpty(),x,y)
            val node=candidates.firstOrNull()
            if(node==null || found==null) { hideResult(); toast("No item found here. Tap the bubble and try a nearby area."); return@launch }
            if(selecting.purpose==SelectionPurpose.PAIR_A) {
                val a=withContext(Dispatchers.Default) { analyzer.measure(node,found) }
                beginSelection(SelectionPurpose.PAIR_B,a); return@launch
            }
            val anchor=selecting.anchor
            if(anchor!=null && (anchor.node.windowId!=node.windowId || anchor.node.density!=node.density || found.nodes.none { it.bounds==anchor.boundsPx && it.resourceId==anchor.node.resourceId && it.className==anchor.node.className })) {
                hideResult(); toast("The first item moved. Choose both items again."); return@launch
            }
            choose(node,anchor?.boundsPx)
        }
    }
    private fun choose(node: NodeSnapshot,anchor: Bounds?=null,neighbor: Boolean=false) {
        val snapshot=tree ?: return
        colorJob?.cancel(); invalidationJob?.cancel(); val token=generation.next()
        val details=anchor==null && ((state.value as? InspectorUiState.ShowingResult)?.detailsExpanded ?: prefs.read().expandedResults)
        if(neighbor) candidates=NodeFinder.candidates(snapshot.nodes,node.bounds.left+node.bounds.width/2,node.bounds.top+node.bounds.height/2)
        val measured=analyzer.measure(node,snapshot).let { if(anchor==null) it else it.copy(pair=SpacingCalculator.between(anchor,node.bounds,node.density)) }
        state.value=InspectorUiState.ShowingResult(measured,details); render()
        colorJob=scope.launch {
            val completed=analyzer.color(measured,snapshot)
            if(generation.accepts(token) && result()?.analysis?.node==node) {
                state.value=replaceResult(state.value,completed)
                if(state.value is InspectorUiState.ShowingResult) render()
            }
        }
    }
    private fun toggleDetails() {
        val s=state.value as? InspectorUiState.ShowingResult ?: return
        state.value=s.copy(detailsExpanded=!s.detailsExpanded)
        prefs.save(prefs.read().copy(expandedResults=!s.detailsExpanded)); render()
    }
    private fun render() {
        host.remove(measurement); measurement=null
        when(val s=state.value) {
            is InspectorUiState.ShowingResult -> {
                val a=s.analysis; val node=a.node
                fun action(target: NodeSnapshot?,neighbor: Boolean=false): (() -> Unit)?=if(a.stale) null else target?.let { { choose(it,neighbor=neighbor) } }
                val actions=mutableMapOf<String,(() -> Unit)?>(
                    "details" to ::toggleDetails,"close" to ::hideResult,
                    "smaller" to action(SelectionAlternatives.smaller(node,candidates)),"larger" to action(SelectionAlternatives.larger(node,candidates)),
                    "pair" to ::pair,"picker" to ::picker,"settings" to ::settings,"done" to ::hideResult
                )
                a.neighbors.forEach { (dir,n) -> actions["neighbor:${dir.name}"]=action(n.node,true) }
                panel.show(a,s.detailsExpanded,prefs.read(),actions)
                if(!a.stale) measurement=MeasurementOverlay(service,host,a,panel.screenBounds,prefs.read(),bubble?.bounds()).also { it.show() }
            }
            InspectorUiState.Welcome -> panel.simple("Ready to inspect",bubble?.bounds()) {
                line("Tap the floating button,\nthen tap any UI item.",16f); line("◎ → Button",24f)
                action("Got it") { prefs.welcomeSeen=true; hideResult() }
            }
            is InspectorUiState.Menu -> panel.simple("",bubble?.bounds()) {
                action("Measure between two items",::pair); action("Color picker",::picker); action("Settings",::settings); action("Stop inspector",::stop)
            }
            is InspectorUiState.Settings -> panel.simple("Settings",bubble?.bounds()) {
                val p=prefs.read()
                actionRow(listOf("Primary: dp" to { prefs.save(prefs.read().copy(primaryUnit=PrimaryUnit.DP)); render() },"Primary: px" to { prefs.save(prefs.read().copy(primaryUnit=PrimaryUnit.PX)); render() }))
                line("Primary unit: ${p.primaryUnit.name.lowercase()}",12f,true)
                line("Both dp and px remain visible.",12f,true)
                fun toggle(label: String,checked: Boolean,change: (Boolean)->Unit) { addView(CheckBox(service).apply { text=label; isChecked=checked; setOnCheckedChangeListener { _,value -> change(value) } }) }
                toggle("Show all spacing",p.showAllSpacing) { prefs.save(prefs.read().copy(showAllSpacing=it)) }
                toggle("Open results in Details",p.expandedResults) { prefs.save(prefs.read().copy(expandedResults=it)) }
                action("Done") { back() }
            }
            is InspectorUiState.PickedColor -> panel.simple("Color picker",bubble?.bounds()) {
                swatch(s.color,s.message)
                action("Done",::hideResult)
            }
            InspectorUiState.Idle,InspectorUiState.Stopped -> panel.remove()
            is InspectorUiState.Selecting -> Unit
        }
        if(state.value !is InspectorUiState.Selecting && state.value != InspectorUiState.Stopped) {
            bubble?.remove(); bubble?.show()
        }
    }
    private suspend fun samplePixel(x: Int,y: Int,target: NodeTree?,token: Long) {
        val screen=host.manager.maximumWindowMetrics.bounds
        val shot=screenshots.capture(target?.windowId,target?.windowBounds ?: Bounds(screen.left,screen.top,screen.right,screen.bottom),Bounds(x,y,x+1,y+1),host::hideAll,host::showAll)
        var color: Int?=null
        val message=when(shot) {
            is ScreenshotResult.Unavailable -> ItemColorAnalysis(null,shot.reason).colorMessage()
            is ScreenshotResult.Success -> try { color=withContext(Dispatchers.Default) { shot.bitmap.getColor(0,0).convert(ColorSpace.get(ColorSpace.Named.SRGB)).toArgb() }; "${rgbHex(color)}\n${rgbDescription(color)}" } finally { shot.bitmap.recycle() }
        }
        if(generation.accepts(token)) { state.value=InspectorUiState.PickedColor(color,message); render() }
    }
    fun onEvent(event: AccessibilityEvent) {
        val a=result()?.analysis ?: return
        if(a.stale || event.packageName?.toString()==service.packageName) return
        val changed=event.windowId==a.node.windowId && (event.eventType==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.eventType==AccessibilityEvent.TYPE_VIEW_SCROLLED || (event.eventType==AccessibilityEvent.TYPE_WINDOWS_CHANGED && event.windowChanges and (AccessibilityEvent.WINDOWS_CHANGE_BOUNDS or AccessibilityEvent.WINDOWS_CHANGE_REMOVED)!=0))
        val switched=event.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.packageName!=null && event.packageName.toString()!=a.node.packageName
        if(changed || switched) {
            invalidationJob?.cancel(); invalidationJob=scope.launch {
                delay(250); val current=result()?.analysis ?: return@launch
                generation.next(); colorJob?.cancel()
                state.value=replaceResult(state.value,current.copy(stale=true,notice="Saved result from the previous screen.",renderedColor=current.renderedColor ?: ItemColorAnalysis(null,"Screen changed before capture completed")))
                if(state.value is InspectorUiState.ShowingResult) render()
            }
        }
    }
    fun configurationChanged() {
        val a=result()
        clearCapture(); host.clear(); panel.remove(); measurement=null
        bubble?.show()
        if(a!=null) { generation.next(); colorJob?.cancel(); state.value=a.copy(analysis=a.analysis.copy(stale=true,notice="Saved result from the previous display orientation.",renderedColor=a.analysis.renderedColor ?: ItemColorAnalysis(null,"Display changed before capture completed"))); render() }
        else hideResult()
    }
    private fun hideResult() {
        generation.next(); selectionJob?.cancel(); colorJob?.cancel(); invalidationJob?.cancel(); clearCapture()
        panel.remove(); host.remove(measurement); measurement=null; tree=null; candidates=emptyList()
        bubble?.selecting(false); state.value=InspectorUiState.Idle
    }
    fun stop() {
        hideResult(); host.clear(); bubble=null; state.value=InspectorUiState.Stopped
    }
    fun destroy() { stop(); scope.cancel() }
}
