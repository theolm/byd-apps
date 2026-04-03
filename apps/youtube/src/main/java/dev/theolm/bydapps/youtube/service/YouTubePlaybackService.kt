package dev.theolm.bydapps.youtube.service

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
import dev.theolm.bydapps.youtube.MainActivity
import dev.theolm.bydapps.youtube.R
import dev.theolm.bydapps.youtube.receiver.YouTubeMediaButtonReceiver

class YouTubePlaybackService : Service() {

    private var webViewHolder: WebViewHolder? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        webViewHolder = WebViewHolder.getInstance()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        intent?.action?.let { action ->
            when (action) {
                YouTubeMediaButtonReceiver.ACTION_MEDIA_BUTTON -> {
                    val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(YouTubeMediaButtonReceiver.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(YouTubeMediaButtonReceiver.EXTRA_KEY_EVENT) as? KeyEvent
                    }

                    keyEvent?.let { event ->
                        handleMediaButton(event.keyCode)
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun handleMediaButton(keyCode: Int) {
        val webView = webViewHolder?.webView ?: return

        val jsCode = when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> """
                (function() {
                    const nextBtn = document.querySelector('.ytp-next-button') ||
                                   document.querySelector('[data-test-id="next-button"]') ||
                                   document.querySelector('[aria-label*="Next"]');
                    if (nextBtn) nextBtn.click();
                })();
            """
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> """
                (function() {
                    const prevBtn = document.querySelector('.ytp-prev-button') ||
                                   document.querySelector('[data-test-id="previous-button"]') ||
                                   document.querySelector('[aria-label*="Previous"]');
                    if (prevBtn) prevBtn.click();
                })();
            """
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> """
                (function() {
                    const playPauseBtn = document.querySelector('.ytp-play-button') ||
                                        document.querySelector('[data-test-id="play-pause-button"]') ||
                                        document.querySelector('[aria-label*="Play"], [aria-label*="Pause"]');
                    if (playPauseBtn) playPauseBtn.click();
                })();
            """
            else -> null
        }

        jsCode?.let { code ->
            handler.post {
                webView.evaluateJavascript(code, null)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "YouTube Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps YouTube playing in background"
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
            .setContentTitle("YouTube")
            .setContentText("Playing in background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

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
        private const val CHANNEL_ID = "youtube_playback_channel"
        private const val NOTIFICATION_ID = 1
    }
}
