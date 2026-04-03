package dev.theolm.bydapps.youtube

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.theolm.core.webview.BaseWebView
import dev.theolm.bydapps.youtube.config.YouTubeConfig
import dev.theolm.bydapps.youtube.service.YouTubePlaybackService
import dev.theolm.bydapps.youtube.ui.theme.YouTubeTheme

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            YouTubeTheme(themeMode = YouTubeConfig.themeMode) {
                BaseWebView(
                    activity = this,
                    configuration = YouTubeConfig,
                    onWebViewCreated = { webView = it },
                    onFullscreenChanged = { isFullscreen ->
                        handleFullscreenChange(isFullscreen)
                    },
                    onBack = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startService(Intent(this, YouTubePlaybackService::class.java))
    }

    override fun onResume() {
        super.onResume()
        webView?.let {
            YouTubePlaybackService.WebViewHolder.getInstance().webView = it
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val handler = YouTubeConfig.mediaButtonHandler
        handler?.let {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    webView?.let { handler.onMediaNext(it) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    webView?.let { handler.onMediaPrevious(it) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    webView?.let { handler.onMediaPlayPause(it) }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleFullscreenChange(isFullscreen: Boolean) {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (isFullscreen) {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
