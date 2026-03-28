package dev.theolm.youtubebyd

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/**
 * Callback for theme injection when page finishes loading
 */
internal interface ThemeInjector {
    fun injectTheme(webView: WebView?)
}

/**
 * Custom WebViewClient for handling URL loading and errors
 */
internal class YouTubeWebViewClient(
    private val context: Context,
    private val themeInjector: ThemeInjector
) : WebViewClient() {
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        themeInjector.injectTheme(view)
    }
    
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        // Only allow HTTPS URLs, block HTTP and all other schemes
        return !url.startsWith(YouTubeConstants.HTTPS_SCHEME)
    }
    
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        val errorMessage = error?.description?.toString() ?: "Unknown error"
        Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
    }
}