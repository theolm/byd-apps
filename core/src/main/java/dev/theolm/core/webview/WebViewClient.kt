package dev.theolm.core.webview

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import dev.theolm.core.constants.WebViewConstants

/**
 * Callback to get the current theme state
 */
fun interface ThemeProvider {
    fun isDarkTheme(): Boolean
}

/**
 * Custom WebViewClient for handling URL loading and errors
 */
class WebViewClient(
    private val context: Context,
    private val themeProvider: ThemeProvider
) : WebViewClient() {
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        injectTheme(view)
    }
    
    private fun injectTheme(view: WebView?) {
        val jsCode = if (themeProvider.isDarkTheme()) {
            WebViewConstants.DARK_MODE_JS
        } else {
            WebViewConstants.LIGHT_MODE_JS
        }
        view?.evaluateJavascript(jsCode, null)
    }
    
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        return !url.startsWith(WebViewConstants.HTTPS_SCHEME)
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