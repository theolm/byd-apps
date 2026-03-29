package dev.theolm.core.webview

import android.view.View
import android.webkit.WebChromeClient

/**
 * Custom WebChromeClient for handling fullscreen video playback
 */
class WebViewChromeClient(
    private val fullscreenCallbacks: FullscreenCallbacks
) : WebChromeClient() {
    
    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
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