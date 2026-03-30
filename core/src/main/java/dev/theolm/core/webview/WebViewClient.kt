package dev.theolm.core.webview

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import dev.theolm.core.constants.WebViewConstants

/**
 * Custom WebViewClient for handling URL loading and errors
 */
class WebViewClient(
    private val context: Context
) : WebViewClient() {
    
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