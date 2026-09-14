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
@Composable fun HomeScreen() {
    val context = LocalContext.current
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
        Text("Select one item. See its size, nearby spacing and rendered color directly on screen.")
        Text("Accessibility: ${if (connected) "Enabled / Connected" else if (enabled) "Enabled / Waiting for service" else "Disabled"}")
        Text("Inspector: ${if (running) "Running" else "Stopped"}")
        Text("Android API ${Build.VERSION.SDK_INT} · Screenshot API available")
        Text("Density ${metrics.density} · Display ${displayBounds.width()} × ${displayBounds.height()} px")
        Text("App window ${config.screenWidthDp} × ${config.screenHeightDp} dp")
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Enable Accessibility") }
        Button(enabled = connected && !running, onClick = { AccessibilityServiceState.service?.startInspector() }) { Text("Start Inspector") }
        OutlinedButton(enabled = running, onClick = { AccessibilityServiceState.service?.stopInspector() }) { Text("Stop Inspector") }
        Text("Start Inspector → open the target app → Select → tap an item. Blue rulers show size; orange rulers show reliable neighbor gaps. Both dp and px are always visible.")
        Text("Use A ↔ B for two items, or Picker for one pixel. Details contains full colors, position and collapsed Advanced accessibility information. Freeze keeps the current measurements and captured item image; Copy exports the summary.")
        Text("Measurements use bounds exposed by the target app. Screenshots are analyzed locally. Secure windows cannot be sampled.")
    } }
}
