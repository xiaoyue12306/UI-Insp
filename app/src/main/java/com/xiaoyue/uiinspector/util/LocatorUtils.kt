package com.xiaoyue.uiinspector.util
import com.xiaoyue.uiinspector.inspector.NodeSnapshot
object LocatorUtils {
    fun pythonString(value: String): String = buildString {
        append('"')
        value.forEach { c -> when (c) {
            '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> if (c.code < 32 || c.code == 127) append("\\u%04x".format(c.code)) else append(c)
        } }
        append('"')
    }
    fun appium(node: NodeSnapshot): String {
        val id = node.resourceId?.takeIf { it.isNotBlank() }
        val description = node.contentDescription?.takeIf { it.isNotBlank() }
        val strategy = if (id != null) "ID" else if (description != null) "ACCESSIBILITY_ID" else null
        return if (strategy != null) "from appium.webdriver.common.appiumby import AppiumBy\n\ndriver.find_element(\n    AppiumBy.$strategy,\n    ${pythonString(id ?: description!!)}\n)"
        else "No stable ID found.\n\nPossible text:\n${if (node.password) "[password redacted]" else node.text.orEmpty()}"
    }
}
