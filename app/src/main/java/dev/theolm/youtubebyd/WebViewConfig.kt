package dev.theolm.youtubebyd

import android.annotation.SuppressLint
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Configuration for YouTube WebView settings
 */
internal object WebViewConfig {
    
    /**
     * Applies all necessary settings to the WebView for optimal YouTube experience
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun WebView.applyYouTubeSettings() {
        settings.apply {
            loadsImagesAutomatically = true
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
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
            setAcceptThirdPartyCookies(this@applyYouTubeSettings, true)
        }
    }
}