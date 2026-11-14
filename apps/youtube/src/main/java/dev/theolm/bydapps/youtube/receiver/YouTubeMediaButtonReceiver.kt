package dev.theolm.bydapps.youtube.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import dev.theolm.bydapps.youtube.service.YouTubePlaybackService

class YouTubeMediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_MEDIA_BUTTON != intent.action) return

        val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        } ?: return

        if (keyEvent.action != KeyEvent.ACTION_DOWN) return

        val serviceIntent = Intent(context, YouTubePlaybackService::class.java).apply {
            action = ACTION_MEDIA_BUTTON
            putExtra(EXTRA_KEY_EVENT, keyEvent)
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        const val ACTION_MEDIA_BUTTON = "dev.theolm.bydapps.youtube.ACTION_MEDIA_BUTTON"
        const val EXTRA_KEY_EVENT = "extra_key_event"
    }
}
