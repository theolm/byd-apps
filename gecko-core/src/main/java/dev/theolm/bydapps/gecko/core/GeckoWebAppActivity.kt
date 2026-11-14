package dev.theolm.bydapps.gecko.core

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.MediaSession

/**
 * Shared GeckoView activity for the YouTube and YT Music packages.
 *
 * The rendering engine is embedded in each APK, so the head unit's system
 * WebView version no longer matters. The page is rendered at 1x, with dark
 * color scheme and a native MediaSession that forwards transport controls to
 * the page's media session.
 */
abstract class GeckoWebAppActivity : ComponentActivity() {

    protected abstract val startUrl: String

    private lateinit var runtime: GeckoRuntime
    private lateinit var mediaSessionCompat: MediaSessionCompat
    private var session: GeckoSession? = null
    private var geckoMediaSession: MediaSession? = null
    private var lastMetadata: MediaSession.Metadata? = null
    private var lastPosition = 0.0
    private var lastDuration = 0.0
    private var canGoBack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setupMediaSession()

        runtime = GeckoRuntime.create(
            applicationContext,
            GeckoRuntimeSettings.Builder()
                .aboutConfigEnabled(true)
                .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_DARK)
                .build()
        )

        val settings = GeckoSessionSettings.Builder()
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .allowJavascript(true)
            .build()

        val geckoSession = GeckoSession(settings)
        geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                this@GeckoWebAppActivity.canGoBack = canGoBack
            }
        }
        geckoSession.mediaSessionDelegate = object : MediaSession.Delegate {
            override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
                geckoMediaSession = mediaSession
                updateNativeState(mediaSession.isActive)
            }

            override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
                if (geckoMediaSession === mediaSession) geckoMediaSession = null
                updateNativeState(false)
            }

            override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
                updateNativeState(true)
            }

            override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
                updateNativeState(false)
            }

            override fun onStop(session: GeckoSession, mediaSession: MediaSession) {
                updateNativeState(false)
            }

            override fun onMetadata(
                session: GeckoSession,
                mediaSession: MediaSession,
                metadata: MediaSession.Metadata
            ) {
                lastMetadata = metadata
                updateNativeState(mediaSession.isActive)
            }

            override fun onPositionState(
                session: GeckoSession,
                mediaSession: MediaSession,
                positionState: MediaSession.PositionState
            ) {
                lastPosition = positionState.position
                lastDuration = positionState.duration
                updateNativeState(mediaSession.isActive)
            }

            override fun onFeatures(
                session: GeckoSession,
                mediaSession: MediaSession,
                features: Long
            ) {
                updateNativeState(mediaSession.isActive)
            }
        }
        geckoSession.open(runtime)
        session = geckoSession

        val geckoView = GeckoView(this)
        geckoView.setSession(geckoSession)
        setContentView(geckoView)
        geckoSession.loadUri(startUrl)
    }

    private fun setupMediaSession() {
        mediaSessionCompat = MediaSessionCompat(this, packageName).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setSessionActivity(
                PendingIntent.getActivity(
                    this@GeckoWebAppActivity,
                    0,
                    Intent(this@GeckoWebAppActivity, this@GeckoWebAppActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    geckoMediaSession?.play()
                }

                override fun onPause() {
                    geckoMediaSession?.pause()
                }

                override fun onSkipToNext() {
                    geckoMediaSession?.nextTrack()
                }

                override fun onSkipToPrevious() {
                    geckoMediaSession?.previousTrack()
                }

                override fun onStop() {
                    geckoMediaSession?.stop()
                }

                override fun onSeekTo(pos: Long) {
                    geckoMediaSession?.seekTo(pos / 1000.0, true)
                }
            })
            isActive = true
        }
        updateNativeState(false)
    }

    private fun updateNativeState(playing: Boolean) {
        val metadata = lastMetadata
        val metadataBuilder = MediaMetadataCompat.Builder()
        metadata?.title?.let {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, it)
        }
        metadata?.artist?.let {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it)
        }
        metadata?.album?.let {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it)
        }
        mediaSessionCompat.setMetadata(metadataBuilder.build())

        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP

        mediaSessionCompat.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                    if (playing) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    (lastPosition * 1000).toLong(),
                    if (playing) 1f else 0f
                )
                .build()
        )
    }

    override fun onDestroy() {
        session?.close()
        mediaSessionCompat.release()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (canGoBack) {
            session?.goBack()
        } else {
            finish()
        }
    }
}
