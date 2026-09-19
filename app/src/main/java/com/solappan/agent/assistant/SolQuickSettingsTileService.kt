package com.solappan.agent.assistant

import android.content.Intent
import android.content.ComponentName
import android.service.voice.VoiceInteractionService
import android.widget.Toast
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class SolQuickSettingsTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = if (isSolAssistant()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "SOL"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (!isSolAssistant()) {
            Toast.makeText(this, "Select Solappan as your default assistant in Settings.", Toast.LENGTH_LONG).show()
            return
        }
        unlockAndRun {
            sendBroadcast(Intent(SolVoiceInteractionService.ACTION_SHOW_ASSISTANT).setPackage(packageName))
        }
    }

    private fun isSolAssistant() = VoiceInteractionService.isActiveService(
        this, ComponentName(this, SolVoiceInteractionService::class.java),
    )
}
