package com.solappan.agent.assistant

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class SolQuickSettingsTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "SOL"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        sendBroadcast(
            Intent(SolVoiceInteractionService.ACTION_SHOW_ASSISTANT)
                .setPackage(packageName),
        )
    }
}
