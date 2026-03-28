package dev.theolm.youtubebyd

import android.webkit.WebChromeClient

/**
 * Callbacks for fullscreen video handling
 */
internal interface FullscreenCallbacks {
    fun onEnterFullscreen(view: android.view.View, callback: WebChromeClient.CustomViewCallback)
    fun onExitFullscreen()
}

/**
 * Custom WebChromeClient for handling fullscreen video playback
 */
internal class YouTubeWebChromeClient(
    private val fullscreenCallbacks: FullscreenCallbacks
) : WebChromeClient() {
    
    override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        if (view != null && callback != null) {
            fullscreenCallbacks.onEnterFullscreen(view, callback)
        }
    }
    
    override fun onHideCustomView() {
        super.onHideCustomView()
        fullscreenCallbacks.onExitFullscreen()
    }
}