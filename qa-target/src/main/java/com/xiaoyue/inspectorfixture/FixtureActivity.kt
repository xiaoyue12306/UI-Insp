package com.xiaoyue.inspectorfixture
import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
class FixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("secure", false)) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(40), dp(64), dp(40), dp(24)); setBackgroundColor(Color.WHITE) }
        layout.addView(TextView(this).apply { text = "Inspector QA Target · ${if (intent.getBooleanExtra("secure", false)) "SECURE" else "NORMAL"}"; textSize = 24f })
        layout.addView(Button(this).apply {
            id = R.id.color_button; text = "Save"; isAllCaps = false; textSize = 24f; setTextColor(0xff202124.toInt())
            setBackgroundColor(0xffc7c6ca.toInt()); contentDescription = "Save settings"
        }, LinearLayout.LayoutParams(dp(350), dp(96)).apply { topMargin = dp(30) })
        val card = LinearLayout(this).apply { id = R.id.card; orientation = LinearLayout.VERTICAL; setBackgroundColor(0xffe3efff.toInt()); setPadding(dp(16), dp(16), dp(16), dp(16)); isClickable = true }
        card.addView(TextView(this).apply { id = R.id.child_label; text = "Child label"; textSize = 20f })
        layout.addView(card, LinearLayout.LayoutParams(dp(350), dp(90)).apply { topMargin = dp(24) })
        layout.addView(EditText(this).apply { id = R.id.clipboard_input; hint = "Paste copied ID or locator here"; setSingleLine(false) }, LinearLayout.LayoutParams(dp(520), dp(140)))
        setContentView(layout)
    }
}
