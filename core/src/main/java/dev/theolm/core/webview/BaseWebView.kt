package dev.theolm.core.webview

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
import androidx.compose.ui.viewinterop.AndroidView
import dev.theolm.core.WebViewConfiguration
import dev.theolm.core.webview.WebViewConfig.applySettings

/**
 * A reusable WebView composable with fullscreen support
 *
 * @param activity The ComponentActivity for context
 * @param configuration WebView configuration (URL, features, etc.)
 * @param onFullscreenChanged Callback when fullscreen state changes (app handles system UI)
 * @param onBack Callback when back button is pressed and WebView can't go back (app handles exit)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BaseWebView(
    activity: ComponentActivity,
    configuration: WebViewConfiguration,
    onFullscreenChanged: (Boolean) -> Unit = {},
    onBack: () -> Unit = {}
) {

    // State for managing WebView and fullscreen
    var webView: WebView? by remember { mutableStateOf(null) }
    var customView: View? by remember { mutableStateOf(null) }
    var customViewCallback: WebChromeClient.CustomViewCallback? by remember { mutableStateOf(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Use rememberUpdatedState to always have the latest values
    val currentWebView by rememberUpdatedState(webView)
    val currentCustomViewCallback by rememberUpdatedState(customViewCallback)
    val currentIsFullscreen by rememberUpdatedState(isFullscreen)

    // Derived state for back button handling
    val canGoBack by remember(currentWebView) {
        derivedStateOf { currentWebView?.canGoBack() == true }
    }

    // Callbacks for fullscreen handling
    val fullscreenCallbacks = remember {
        object : FullscreenCallbacks {
            @Suppress("UNUSED_VALUE")
            override fun onEnterFullscreen(
                view: View,
                callback: WebChromeClient.CustomViewCallback
            ) {
                webView?.visibility = View.GONE
                customView = view
                customViewCallback = callback
                isFullscreen = true
                onFullscreenChanged(true)

                (webView?.parent as? ViewGroup)?.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }

            @Suppress("UNUSED_VALUE")
            override fun onExitFullscreen() {
                customView?.let { view ->
                    (view.parent as? ViewGroup)?.removeView(view)
                }

                webView?.visibility = View.VISIBLE
                onFullscreenChanged(false)

                customView = null
                customViewCallback = null
                isFullscreen = false
            }
        }
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
                onBack()
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

                    applySettings()

                    webChromeClient = WebViewChromeClient(fullscreenCallbacks)
                    webViewClient = WebViewClient(factoryContext, configuration)
                    
                    if (configuration.enableDownloads) {
                        setDownloadListener(WebViewDownloadHandler(factoryContext))
                    }

                    loadUrl(configuration.startUrl)
                }

                webView = webViewInstance
                addView(webViewInstance)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}