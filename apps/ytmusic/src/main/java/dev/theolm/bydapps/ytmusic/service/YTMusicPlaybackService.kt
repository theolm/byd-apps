package dev.theolm.bydapps.ytmusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import dev.theolm.bydapps.ytmusic.MainActivity
import dev.theolm.bydapps.ytmusic.R
import dev.theolm.bydapps.ytmusic.config.YTMusicConfig
import dev.theolm.bydapps.ytmusic.receiver.YTMusicMediaButtonReceiver
import dev.theolm.core.media.WebViewMediaSession

class YTMusicPlaybackService : Service() {

    private var webViewHolder: WebViewHolder? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var mediaSession: WebViewMediaSession

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        webViewHolder = WebViewHolder.getInstance()

        val mediaHandler = requireNotNull(YTMusicConfig.mediaButtonHandler) {
            "YTMusicConfig.mediaButtonHandler is required"
        }
        mediaSession = WebViewMediaSession(
            context = this,
            tag = "YTMusicBYD",
            sessionActivity = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ),
            webView = { webViewHolder?.webView },
            handler = mediaHandler
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            YTMusicMediaButtonReceiver.ACTION_MEDIA_BUTTON -> {
                val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        YTMusicMediaButtonReceiver.EXTRA_KEY_EVENT,
                        KeyEvent::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(YTMusicMediaButtonReceiver.EXTRA_KEY_EVENT) as? KeyEvent
                }
                keyEvent?.let { handleMediaButton(it.keyCode) }
            }
            ACTION_NEXT -> handleMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT)
            ACTION_PREVIOUS -> handleMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            ACTION_PLAY_PAUSE -> handleMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }

    private fun handleMediaButton(keyCode: Int) {
        val webView = webViewHolder?.webView ?: return
        val mediaHandler = YTMusicConfig.mediaButtonHandler ?: return

        mainHandler.post {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> mediaHandler.onMediaNext(webView)
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> mediaHandler.onMediaPrevious(webView)
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> mediaHandler.onMediaPlayPause(webView)
                KeyEvent.KEYCODE_MEDIA_PLAY -> mediaHandler.onMediaPlay(webView)
                KeyEvent.KEYCODE_MEDIA_PAUSE -> mediaHandler.onMediaPause(webView)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "YT Music Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps YT Music playing in background"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YT Music")
            .setContentText("Playing in background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                mediaAction(ACTION_PREVIOUS, 1)
            )
            .addAction(
                android.R.drawable.ic_media_play,
                "Play/Pause",
                mediaAction(ACTION_PLAY_PAUSE, 2)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                mediaAction(ACTION_NEXT, 3)
            )
            .build()
    }

    private fun mediaAction(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, YTMusicPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    class WebViewHolder private constructor() {
        var webView: android.webkit.WebView? = null

        companion object {
            @Volatile
            private var instance: WebViewHolder? = null

            fun getInstance(): WebViewHolder {
                return instance ?: synchronized(this) {
                    instance ?: WebViewHolder().also { instance = it }
                }
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "ytmusic_playback_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_NEXT = "dev.theolm.bydapps.ytmusic.ACTION_NEXT"
        private const val ACTION_PREVIOUS = "dev.theolm.bydapps.ytmusic.ACTION_PREVIOUS"
        private const val ACTION_PLAY_PAUSE = "dev.theolm.bydapps.ytmusic.ACTION_PLAY_PAUSE"
    }
}
