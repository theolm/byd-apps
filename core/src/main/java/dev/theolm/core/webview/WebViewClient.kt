package dev.theolm.core.webview

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import dev.theolm.core.WebViewConfiguration

/**
 * Custom WebViewClient for handling URL loading and errors
 */
class WebViewClient(
    private val context: Context,
    private val configuration: WebViewConfiguration? = null
) : WebViewClient() {
    
    private var lastInjectedUrl: String? = null
    
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        return !url.startsWith("https://")
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        injectJavaScript(view, url)
    }
    
    /**
     * Called when the WebView navigates to a new URL within a Single Page Application.
     * Re-injects JavaScript if SPA tracking is enabled and URL has changed.
     */
    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        
        if (configuration?.enableSpaNavigationTracking == true && url != lastInjectedUrl) {
            injectJavaScript(view, url)
        }
    }
    
    /**
     * Injects custom JavaScript with optional delay based on configuration.
     */
    private fun injectJavaScript(view: WebView?, url: String?) {
        configuration?.customJavaScript?.let { script ->
            val delay = configuration.javascriptInjectionDelayMs
            
            if (delay > 0) {
                view?.postDelayed({
                    view.evaluateJavascript(script, null)
                    lastInjectedUrl = url
                }, delay)
            } else {
                view?.evaluateJavascript(script, null)
                lastInjectedUrl = url
            }
        }
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
