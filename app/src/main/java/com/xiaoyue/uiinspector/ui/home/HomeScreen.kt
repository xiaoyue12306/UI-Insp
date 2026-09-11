package com.xiaoyue.uiinspector.ui.home
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xiaoyue.uiinspector.accessibility.AccessibilityServiceState
@Composable fun HomeScreen() {
    val context = LocalContext.current
    val connected by AccessibilityServiceState.connected.collectAsState()
    val running by AccessibilityServiceState.running.collectAsState()
    Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Android UI Inspector", style = MaterialTheme.typography.headlineMedium)
        Text("Inspect accessibility elements and rendered colors on this device.")
        Text("Accessibility: ${if (connected) "Enabled / Connected" else "Disabled / Disconnected"}")
        Text("Inspector: ${if (running) "Running" else "Stopped"}")
        Text("Android API ${Build.VERSION.SDK_INT} · Screenshot API available")
        val metrics = context.resources.displayMetrics
        Text("Density ${metrics.density} · Resolution ${metrics.widthPixels} × ${metrics.heightPixels}")
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Enable Accessibility") }
        Button(enabled = connected && !running, onClick = { AccessibilityServiceState.service?.startInspector() }) { Text("Start Inspector") }
        OutlinedButton(enabled = running, onClick = { AccessibilityServiceState.service?.stopInspector() }) { Text("Stop Inspector") }
        Text("Accessibility reads semantics exposed by the target app. Screenshots are analyzed locally on selection. Secure windows cannot be sampled.")
    } }
}
