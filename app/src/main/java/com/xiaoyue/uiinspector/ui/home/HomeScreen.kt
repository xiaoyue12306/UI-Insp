package com.xiaoyue.uiinspector.ui.home
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.xiaoyue.uiinspector.accessibility.AccessibilityServiceState
import com.xiaoyue.uiinspector.interaction.*
@Composable fun HomeScreen() {
    val context = LocalContext.current
    val preferences = remember { InspectorPreferences(context) }
    var showSettings by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var presentation by remember { mutableStateOf(preferences.read()) }
    val connected by AccessibilityServiceState.connected.collectAsState()
    val running by AccessibilityServiceState.running.collectAsState()
    var enabled by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, context) {
        fun refresh() {
            enabled = context.getSystemService(AccessibilityManager::class.java)
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                    val info = it.resolveInfo.serviceInfo
                    ComponentName(info.packageName, info.name) == ComponentName(context, com.xiaoyue.uiinspector.accessibility.InspectorAccessibilityService::class.java)
                }
        }
        refresh()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val config = LocalConfiguration.current
    val metrics = LocalResources.current.displayMetrics
    val displayBounds = remember(config) { context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds }
    Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Android Visual UI Inspector", style = MaterialTheme.typography.headlineMedium)
        Text("Measure UI size, spacing and color.")
        Text("Accessibility  ${if (connected) "✓ Enabled" else if (enabled) "Connecting…" else "Not enabled"}")
        Button(enabled = connected || !enabled, onClick = {
            if (!connected) context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            else {
                if (!running) AccessibilityServiceState.service?.startInspector()
                (context as? android.app.Activity)?.finish()
            }
        }) { Text(if (!enabled && !connected) "Enable Accessibility" else if (running) "Return to Inspector" else "Start Inspector") }
        if (running) TextButton(onClick = { AccessibilityServiceState.service?.stopInspector() }) { Text("Stop Inspector") }
        HorizontalDivider()
        Text("How to use", style = MaterialTheme.typography.titleLarge)
        Text("1. Tap the floating button\n2. Tap any UI item\n3. Read size, spacing and color")
        Text("Tap the bubble again to inspect another item. Long press it for more tools.", style = MaterialTheme.typography.bodyMedium)
        Row { TextButton(onClick = { presentation=preferences.read(); showSettings=true }) { Text("Settings") }; TextButton(onClick = { showDiagnostics=!showDiagnostics }) { Text("Diagnostics") } }
        if (showDiagnostics) {
            Text("Inspector: ${if(running) "Running" else "Stopped"}\nService connected: $connected\nAndroid API ${Build.VERSION.SDK_INT}\nDensity ${metrics.density}\nDisplay ${displayBounds.width()} × ${displayBounds.height()} px\nApp window ${config.screenWidthDp} × ${config.screenHeightDp} dp")
            Text("Measurements use accessibility bounds. Screenshots are analyzed locally. Secure windows block color capture.")
        }
    } }
    if (showSettings) AlertDialog(onDismissRequest = { showSettings=false }, title = { Text("Settings") }, text = {
        Column {
            Text("Primary unit")
            Row { PrimaryUnit.entries.forEach { unit -> TextButton(onClick={ presentation=presentation.copy(primaryUnit=unit); preferences.save(presentation) }) { Text(if(presentation.primaryUnit==unit) "✓ ${unit.name.lowercase()}" else unit.name.lowercase()) } } }
            Text("Both dp and px always remain visible.")
            Row { Checkbox(presentation.showAllSpacing,{ presentation=presentation.copy(showAllSpacing=it); preferences.save(presentation) }); Text("Show all spacing") }
            Row { Checkbox(presentation.expandedResults,{ presentation=presentation.copy(expandedResults=it); preferences.save(presentation) }); Text("Open results in Details") }
        }
    }, confirmButton = { TextButton(onClick = { showSettings=false }) { Text("Done") } })
}
