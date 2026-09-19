package com.solappan.agent.testing

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Test APK only. No network or contacts: Send updates a local label. */
class FakeConversationActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 80, 32, 32) }
        layout.addView(TextView(this).apply { text = "Synthetic Recipient"; textSize = 24f })
        val result = TextView(this).apply { text = "No synthetic message sent" }
        layout.addView(result)
        val draft = EditText(this).apply { contentDescription = "Draft"; setText("Synthetic hello"); setSingleLine(true) }
        layout.addView(draft)
        layout.addView(Button(this).apply {
            text = "Send"
            setOnClickListener { result.text = "Synthetic sent: ${draft.text}"; draft.setText("") }
        })
        layout.isFocusableInTouchMode = true
        layout.requestFocus()
        setContentView(layout)
    }
}
