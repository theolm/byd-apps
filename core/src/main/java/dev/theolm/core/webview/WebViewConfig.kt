package dev.theolm.core.webview

import android.annotation.SuppressLint
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Configuration for WebView settings
 */
object WebViewConfig {
    
    /**
     * Applies standard settings to the WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun WebView.applySettings() {
        settings.apply {
            loadsImagesAutomatically = true
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            userAgentString = userAgentString.replace("wv", "")
        }
        
        // Enable hardware acceleration for smooth video playback
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        // Configure cookies for persistent sign-in
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@applySettings, true)
        }
    }
}