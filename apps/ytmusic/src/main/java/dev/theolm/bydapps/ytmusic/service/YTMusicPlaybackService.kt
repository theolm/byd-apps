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
import dev.theolm.bydapps.ytmusic.MainActivity
import dev.theolm.bydapps.ytmusic.R
import dev.theolm.bydapps.ytmusic.receiver.YTMusicMediaButtonReceiver

class YTMusicPlaybackService : Service() {

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
                YTMusicMediaButtonReceiver.ACTION_MEDIA_BUTTON -> {
                    val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(YTMusicMediaButtonReceiver.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(YTMusicMediaButtonReceiver.EXTRA_KEY_EVENT) as? KeyEvent
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
                    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'MediaTrackNext', code: 'MediaTrackNext', bubbles: true }));
                    setTimeout(function() {
                        const nextBtn = document.querySelector('[data-test-id="next-button"]') || 
                                       document.querySelector('.next-button') ||
                                       document.querySelector('[aria-label*="Next"]') ||
                                       document.querySelector('tp-yt-paper-icon-button[aria-label*="next"]');
                        if (nextBtn) nextBtn.click();
                    }, 100);
                })();
            """
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> """
                (function() {
                    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'MediaTrackPrevious', code: 'MediaTrackPrevious', bubbles: true }));
                    setTimeout(function() {
                        const prevBtn = document.querySelector('[data-test-id="previous-button"]') ||
                                       document.querySelector('.previous-button') ||
                                       document.querySelector('[aria-label*="Previous"]') ||
                                       document.querySelector('tp-yt-paper-icon-button[aria-label*="previous"]');
                        if (prevBtn) prevBtn.click();
                    }, 100);
                })();
            """
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> """
                (function() {
                    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'MediaPlayPause', code: 'MediaPlayPause', bubbles: true }));
                    setTimeout(function() {
                        const playPauseBtn = document.querySelector('[data-test-id="play-pause-button"]') ||
                                            document.querySelector('.play-pause-button') ||
                                            document.querySelector('[aria-label*="Play"], [aria-label*="Pause"]') ||
                                            document.querySelector('tp-yt-paper-icon-button[aria-label*="play"], tp-yt-paper-icon-button[aria-label*="pause"]');
                        if (playPauseBtn) playPauseBtn.click();
                    }, 100);
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
        private const val CHANNEL_ID = "ytmusic_playback_channel"
        private const val NOTIFICATION_ID = 1
    }
}
