package dev.theolm.youtubebyd.webview

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.theolm.youtubebyd.webview.WebViewConfig.applyYouTubeSettings

/**
 * A WebView composable that loads YouTube with fullscreen support and theme synchronization
 *
 * @param activity The ComponentActivity for system UI control
 * @param isSystemDark Whether the system is currently in dark mode
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YouTubeWebView(
    activity: ComponentActivity,
    isSystemDark: Boolean
) {
    val context = LocalContext.current

    // State for managing WebView and fullscreen
    var webView: WebView? by remember { mutableStateOf(null) }
    var customView: View? by remember { mutableStateOf(null) }
    var customViewCallback: WebChromeClient.CustomViewCallback? by remember { mutableStateOf(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Use rememberUpdatedState to always have latest values
    val currentWebView by rememberUpdatedState(webView)
    val currentCustomViewCallback by rememberUpdatedState(customViewCallback)
    val currentIsFullscreen by rememberUpdatedState(isFullscreen)
    val currentIsSystemDark by rememberUpdatedState(isSystemDark)

    // Window insets controller for system UI
    val windowInsetsController = remember(activity) {
        WindowInsetsControllerCompat(activity.window, activity.window.decorView)
    }

    // Derived state for back button handling
    val canGoBack by remember(currentWebView) {
        derivedStateOf { currentWebView?.canGoBack() == true }
    }

    // Callbacks for fullscreen handling
    @Suppress("UNUSED_VALUE")
    val fullscreenCallbacks = remember {
        object : FullscreenCallbacks {
            override fun onEnterFullscreen(
                view: View,
                callback: WebChromeClient.CustomViewCallback
            ) {
                webView?.visibility = View.GONE
                customView = view
                customViewCallback = callback
                isFullscreen = true

                // Hide system bars for immersive fullscreen
                windowInsetsController.apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }

                (webView?.parent as? ViewGroup)?.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }

            override fun onExitFullscreen() {
                customView?.let { view ->
                    (view.parent as? ViewGroup)?.removeView(view)
                }

                webView?.visibility = View.VISIBLE

                // Show system bars when exiting fullscreen
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

                customView = null
                customViewCallback = null
                isFullscreen = false
            }
        }
    }
    
    // Theme provider that always returns current value
    val themeProvider = remember {
        ThemeProvider { currentIsSystemDark }
    }
    
    // Handle back button with priority: exit fullscreen > go back > exit app
    BackHandler {
        when {
            currentIsFullscreen && currentCustomViewCallback != null -> {
                currentCustomViewCallback?.onCustomViewHidden()
            }
            canGoBack -> {
                currentWebView?.goBack()
            }
            else -> {
                (context as? ComponentActivity)?.finish()
            }
        }
    }
    
    // Cleanup WebView when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }
    
    AndroidView(
        factory = { factoryContext ->
            FrameLayout(factoryContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                val webViewInstance = WebView(factoryContext).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    applyYouTubeSettings()
                    
                    webChromeClient = YouTubeWebChromeClient(fullscreenCallbacks)
                    webViewClient = YouTubeWebViewClient(factoryContext, themeProvider)
                    setDownloadListener(YouTubeDownloadHandler(factoryContext))
                    
                    loadUrl(YouTubeConstants.YOUTUBE_URL)
                }
                
                webView = webViewInstance
                addView(webViewInstance)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}