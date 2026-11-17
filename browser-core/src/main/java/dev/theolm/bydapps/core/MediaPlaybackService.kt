package dev.theolm.bydapps.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat

/**
 * Keeps the app process alive while GeckoView has an active media session and
 * exposes the current session to car launchers through MediaBrowserService.
 *
 * The service does not own the player: GeckoView and the Activity keep the
 * playback. Its job is to hold a foreground-service priority, publish the
 * mandatory notification and provide a standard MediaBrowserService endpoint.
 */
class MediaPlaybackService : MediaBrowserServiceCompat() {

    private var sessionTokenSet = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setTokenIfNeeded(MediaSessionRegistry.token)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = intent?.let {
            IntentCompat.getParcelableExtra(
                it,
                EXTRA_SESSION_TOKEN,
                MediaSessionCompat.Token::class.java
            )
        } ?: MediaSessionRegistry.token

        if (token == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        setTokenIfNeeded(token)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(token),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        return START_NOT_STICKY
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        setTokenIfNeeded(MediaSessionRegistry.token)
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun setTokenIfNeeded(token: MediaSessionCompat.Token?) {
        if (token == null || sessionTokenSet) return
        setSessionToken(token)
        sessionTokenSet = true
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.media_playback_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.media_playback_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(token: MediaSessionCompat.Token): Notification {
        val appName = applicationInfo.loadLabel(packageManager)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_media_notification)
            .setContentTitle(appName)
            .setContentText(getString(R.string.media_playback_notification))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setStyle(MediaNotificationCompat.MediaStyle().setMediaSession(token))

        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            builder.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        return builder.build()
    }

    companion object {
        const val EXTRA_SESSION_TOKEN = "dev.theolm.bydapps.core.SESSION_TOKEN"
        private const val ROOT_ID = "root"
        private const val CHANNEL_ID = "media_playback"
        private const val NOTIFICATION_ID = 1001
    }
}

/**
 * Process-wide bridge between the Activity-owned MediaSessionCompat and the
 * MediaBrowserService endpoint. Each app runs in its own process.
 */
internal object MediaSessionRegistry {
    @Volatile
    var token: MediaSessionCompat.Token? = null
}
