package dev.theolm.core.media

import android.app.PendingIntent
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.webkit.WebView
import dev.theolm.core.MediaButtonHandler

/**
 * A MediaSession that forwards transport controls to a WebView.
 *
 * DiLink routes the steering-wheel buttons through the active media session,
 * so a foreground app without one never sees the keys. The callbacks call the
 * app's [MediaButtonHandler], which evaluates JavaScript in the current page.
 *
 * The playback state is polled from the page's `<video>` element so the
 * framework knows whether a play/pause key should turn into onPlay or onPause.
 */
class WebViewMediaSession(
    context: Context,
    tag: String,
    sessionActivity: PendingIntent?,
    private val webView: () -> WebView?,
    private val handler: MediaButtonHandler
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastPlaying: Boolean? = null

    val session: MediaSessionCompat = MediaSessionCompat(context, tag).apply {
        setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        sessionActivity?.let { setSessionActivity(it) }
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() = withWebView { handler.onMediaPlay(it) }
            override fun onPause() = withWebView { handler.onMediaPause(it) }
            override fun onSkipToNext() = withWebView { handler.onMediaNext(it) }
            override fun onSkipToPrevious() = withWebView { handler.onMediaPrevious(it) }
            override fun onStop() = withWebView { handler.onMediaPause(it) }
        })
        isActive = true
        setPlaybackState(playbackState(playing = false))
    }

    private val pollPlaybackState = object : Runnable {
        override fun run() {
            val view = webView()
            if (view != null) {
                view.evaluateJavascript(
                    "(function(){var v=document.querySelector('video');" +
                        "return v?(v.paused?'paused':'playing'):'none';})()"
                ) { result ->
                    val playing = result?.trim()?.trim('"') == "playing"
                    if (playing != lastPlaying) {
                        lastPlaying = playing
                        session.setPlaybackState(playbackState(playing))
                    }
                }
            }
            mainHandler.postDelayed(this, 1000L)
        }
    }

    init {
        mainHandler.post(pollPlaybackState)
    }

    val sessionToken: MediaSessionCompat.Token
        get() = session.sessionToken

    private fun playbackState(playing: Boolean): PlaybackStateCompat =
        PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (playing) PlaybackStateCompat.STATE_PLAYING
                else PlaybackStateCompat.STATE_PAUSED,
                0L,
                if (playing) 1f else 0f
            )
            .build()

    private fun withWebView(action: (WebView) -> Unit) {
        webView()?.let(action)
    }

    fun release() {
        mainHandler.removeCallbacks(pollPlaybackState)
        session.isActive = false
        session.release()
    }
}
