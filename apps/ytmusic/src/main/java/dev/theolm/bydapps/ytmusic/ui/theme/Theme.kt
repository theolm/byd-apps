package dev.theolm.bydapps.ytmusic.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = YTMusicPrimary,
    onPrimary = YTMusicOnPrimary,
    background = YTMusicBackground,
    onBackground = YTMusicOnBackground,
    surface = YTMusicSurface,
    onSurface = YTMusicOnSurface,
    surfaceVariant = YTMusicSurfaceVariant,
    onSurfaceVariant = YTMusicOnSurface
)

/**
 * YT Music theme - Always uses dark color scheme
 * No light mode support
 */
@Composable
fun YTMusicTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar is always dark (light status bar = false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
