package dev.theolm.youtubebyd.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import dev.theolm.youtubebyd.ui.theme.YouTubeBYDTheme
import dev.theolm.youtubebyd.webview.YouTubeWebView

/**
 * Main entry point for the YouTube BYD application
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isSystemDark = isSystemInDarkTheme()

            YouTubeBYDTheme(darkTheme = isSystemDark) {
                YouTubeWebView(
                    activity = this,
                    isSystemDark = isSystemDark
                )
            }
        }
    }
}