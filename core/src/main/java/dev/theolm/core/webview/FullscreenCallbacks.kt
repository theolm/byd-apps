package dev.theolm.core.webview

import android.view.View
import android.webkit.WebChromeClient

/**
 * Callbacks for fullscreen video handling
 */
interface FullscreenCallbacks {
    fun onEnterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onExitFullscreen()
}