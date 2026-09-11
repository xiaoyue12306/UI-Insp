package com.xiaoyue.uiinspector.util
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
fun copyText(context: Context, label: String, value: String) {
    try {
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    } catch (e: RuntimeException) { Log.w("UIInspector", "Clipboard unavailable", e); Toast.makeText(context, "Clipboard unavailable", Toast.LENGTH_SHORT).show() }
}
