package com.solappan.agent.assistant

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class SolVoiceInteractionSession(private val sessionContext: Context) : VoiceInteractionSession(sessionContext) {
    private lateinit var statusText: TextView

    override fun onCreateContentView(): View {
        val root = FrameLayout(sessionContext).apply {
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }

        val panel = LinearLayout(sessionContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            elevation = dp(12).toFloat()
            background = roundedBackground(Color.rgb(26, 21, 39), 28)
        }
        root.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )

        panel.addView(TextView(sessionContext).apply {
            text = "✦"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(33, 17, 11))
            background = roundedBackground(Color.rgb(255, 155, 84), 22)
        }, LinearLayout.LayoutParams(dp(64), dp(64)))

        panel.addView(TextView(sessionContext).apply {
            text = "SOL  /  ANDROID AGENT"
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(255, 155, 84))
        }.withTopMargin(14))

        statusText = TextView(sessionContext).apply {
            text = "Ready"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(243, 238, 247))
        }
        panel.addView(statusText.withTopMargin(8))

        panel.addView(TextView(sessionContext).apply {
            text = "Voice input arrives in the next milestone"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(183, 174, 195))
        }.withTopMargin(6))

        panel.addView(Button(sessionContext).apply {
            text = "Dismiss"
            isAllCaps = false
            setOnClickListener { finish() }
        }.withTopMargin(14))

        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        if (::statusText.isInitialized) statusText.text = "Ready"
        Log.i(TAG, "Assistant session shown")
    }

    override fun onHide() {
        Log.i(TAG, "Assistant session hidden")
        super.onHide()
    }

    private fun TextView.withTopMargin(margin: Int): TextView = apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(margin) }
    }

    private fun roundedBackground(color: Int, radiusDp: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
    }

    private fun dp(value: Int): Int =
        (value * sessionContext.resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "SolAssistantSession"
    }
}
