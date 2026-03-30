package dev.theolm.core

import android.webkit.WebView

/**
 * Interface for handling media button events (steering wheel controls, headset buttons, etc.)
 * Implement this to define app-specific behavior for next/previous/play-pause actions.
 */
interface MediaButtonHandler {
    /**
     * Called when the next track/media button is pressed
     * @param webView The WebView instance to control
     */
    fun onMediaNext(webView: WebView)
    
    /**
     * Called when the previous track/media button is pressed
     * @param webView The WebView instance to control
     */
    fun onMediaPrevious(webView: WebView)
    
    /**
     * Called when the play/pause media button is pressed
     * @param webView The WebView instance to control
     */
    fun onMediaPlayPause(webView: WebView)
}
