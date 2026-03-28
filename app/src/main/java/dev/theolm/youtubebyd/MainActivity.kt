package dev.theolm.youtubebyd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.theolm.youtubebyd.ui.theme.YouTubeBYDTheme

/**
 * Main entry point for the YouTube BYD application
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupImmersiveMode()
        
        setContent {
            val isSystemDark = isSystemInDarkTheme()
            
            YouTubeBYDTheme(darkTheme = isSystemDark) {
                YouTubeWebView(isSystemDark = isSystemDark)
            }
        }
    }
    
    /**
     * Configures the window for edge-to-edge immersive experience
     */
    private fun setupImmersiveMode() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}