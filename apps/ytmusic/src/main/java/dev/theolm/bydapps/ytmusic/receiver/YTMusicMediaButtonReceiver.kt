package dev.theolm.bydapps.ytmusic.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import dev.theolm.bydapps.ytmusic.service.YTMusicPlaybackService

class YTMusicMediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_MEDIA_BUTTON != intent.action) return

        val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        } ?: return

        if (keyEvent.action != KeyEvent.ACTION_DOWN) return

        val serviceIntent = Intent(context, YTMusicPlaybackService::class.java).apply {
            action = ACTION_MEDIA_BUTTON
            putExtra(EXTRA_KEY_EVENT, keyEvent)
        }

        context.startService(serviceIntent)
    }

    companion object {
        const val ACTION_MEDIA_BUTTON = "dev.theolm.bydapps.ytmusic.ACTION_MEDIA_BUTTON"
        const val EXTRA_KEY_EVENT = "extra_key_event"
    }
}
