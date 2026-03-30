package dev.theolm.bydapps.youtube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.theolm.core.webview.BaseWebView
import dev.theolm.bydapps.youtube.config.YouTubeConfig
import dev.theolm.bydapps.youtube.ui.theme.YouTubeTheme

/**
 * Main entry point for the YouTube application
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Opt-out of enforced edge-to-edge on SDK 35+
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            YouTubeTheme {
                BaseWebView(
                    activity = this,
                    configuration = YouTubeConfig,
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

    /**
     * Handles system UI visibility when fullscreen state changes
     */
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
