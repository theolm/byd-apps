package dev.theolm.bydapps.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
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
    private var controller: MediaControllerCompat? = null

    @Volatile
    private var nowPlayingMetadata: MediaMetadataCompat? = null

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            nowPlayingMetadata = metadata
            notifyChildrenChanged(ROOT_ID)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            notifyChildrenChanged(ROOT_ID)
        }
    }

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
            startForegroundNotification(null)
            stopSelf()
            return START_NOT_STICKY
        }

        // startForeground must happen within five seconds, before any
        // controller binding or other potentially slow work.
        startForegroundNotification(token)
        setTokenIfNeeded(token)
        bindControllerIfNeeded(token)
        return START_NOT_STICKY
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        val token = MediaSessionRegistry.token
        setTokenIfNeeded(token)
        bindControllerIfNeeded(token)
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId != ROOT_ID) {
            result.sendResult(mutableListOf())
            return
        }

        val metadata = nowPlayingMetadata
        val appName = applicationInfo.loadLabel(packageManager)
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(NOW_PLAYING_ID)
            .setTitle(
                metadata?.getText(MediaMetadataCompat.METADATA_KEY_TITLE) ?: appName
            )
            .setSubtitle(
                metadata?.getText(MediaMetadataCompat.METADATA_KEY_ARTIST)
                    ?: metadata?.getText(MediaMetadataCompat.METADATA_KEY_ALBUM)
            )
            .apply {
                metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
                    ?.let(::setIconBitmap)
                metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
                    ?.let { setIconUri(Uri.parse(it)) }
            }
            .build()

        result.sendResult(
            mutableListOf(
                MediaBrowserCompat.MediaItem(
                    description,
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )
            )
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        controller?.unregisterCallback(controllerCallback)
        controller = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startForegroundNotification(token: MediaSessionCompat.Token?) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(token),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    private fun setTokenIfNeeded(token: MediaSessionCompat.Token?) {
        if (token == null || sessionTokenSet) return
        setSessionToken(token)
        sessionTokenSet = true
    }

    private fun bindControllerIfNeeded(token: MediaSessionCompat.Token?) {
        if (token == null || controller != null) return
        try {
            controller = MediaControllerCompat(this, token).also {
                it.registerCallback(controllerCallback)
                nowPlayingMetadata = it.metadata
            }
            notifyChildrenChanged(ROOT_ID)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to attach media controller", error)
        }
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

    private fun buildNotification(token: MediaSessionCompat.Token?): Notification {
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
        token?.let {
            builder.setStyle(MediaNotificationCompat.MediaStyle().setMediaSession(it))
        }

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
        const val NOW_PLAYING_ID = "now_playing"
        private const val TAG = "MediaPlaybackService"
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
