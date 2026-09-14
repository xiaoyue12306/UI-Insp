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
        if (intent.getBooleanExtra("measurement", false)) {
            val frame = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
            fun button(idValue: Int, label: String, x: Int, y: Int, w: Int, h: Int, color: Int = 0xffe3efff.toInt()) {
                frame.addView(Button(this).apply {
                    id = idValue; text = label; isAllCaps = false; setTextColor(0xff202124.toInt()); setBackgroundColor(color)
                }, FrameLayout.LayoutParams(dp(w), dp(h)).apply { leftMargin = dp(x); topMargin = dp(y) })
            }
            frame.addView(TextView(this).apply { text = "Visual measurement QA · Size 328 × 48 dp · Gaps T/B 24 dp, L/R 16 dp"; textSize = 20f },
                FrameLayout.LayoutParams(-2, -2).apply { leftMargin = dp(40); topMargin = dp(40) })
            button(R.id.color_button, "Save", 260, 240, 328, 48, 0xffc7c6ca.toInt())
            button(R.id.top_neighbor, "Top", 260, 168, 328, 48)
            button(R.id.bottom_neighbor, "Bottom", 260, 312, 328, 48)
            button(R.id.left_neighbor, "Left", 140, 240, 104, 48)
            button(R.id.right_neighbor, "Right", 604, 240, 104, 48)
            button(R.id.diagonal, "Diagonal", 610, 302, 104, 48)
            frame.addView(EditText(this).apply { id = R.id.clipboard_input; hint = "Paste measurement summary here"; gravity = Gravity.TOP; textSize = 14f },
                FrameLayout.LayoutParams(dp(700), dp(240)).apply { leftMargin = dp(80); topMargin = dp(480) })
            setContentView(frame)
            return
        }
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
