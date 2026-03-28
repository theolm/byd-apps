package dev.theolm.youtubebyd.webview

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/**
 * Callback to get the current theme state
 */
internal fun interface ThemeProvider {
    fun isDarkTheme(): Boolean
}

/**
 * Custom WebViewClient for handling URL loading and errors
 */
internal class YouTubeWebViewClient(
    private val context: Context,
    private val themeProvider: ThemeProvider
) : WebViewClient() {
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        injectTheme(view)
    }
    
    private fun injectTheme(view: WebView?) {
        val jsCode = if (themeProvider.isDarkTheme()) {
            YouTubeConstants.DARK_MODE_JS
        } else {
            YouTubeConstants.LIGHT_MODE_JS
        }
        view?.evaluateJavascript(jsCode, null)
    }
    
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
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