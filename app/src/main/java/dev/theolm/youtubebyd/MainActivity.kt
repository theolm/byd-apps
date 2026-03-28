package dev.theolm.youtubebyd

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.theolm.youtubebyd.ui.theme.YouTubeBYDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Enable fullscreen immersive mode
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
        
        setContent {
            YouTubeBYDTheme {
                YouTubeWebView()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "StaticFieldLeak")
@Composable
fun YouTubeWebView() {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    
    // Handle back button
    BackHandler {
        when {
            isFullscreen && customViewCallback != null -> {
                // Exit fullscreen first
                customViewCallback?.onCustomViewHidden()
            }
            webView?.canGoBack() == true -> {
                // Go back in WebView history
                webView?.goBack()
            }
            else -> {
                // Exit app
                (webView?.context as? ComponentActivity)?.finish()
            }
        }
    }
    
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                val webViewInstance = WebView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.apply {
                        loadsImagesAutomatically = true
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                    }
                    
                    // Enable cookies
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            super.onShowCustomView(view, callback)
                            if (view != null && callback != null) {
                                customView = view
                                customViewCallback = callback
                                isFullscreen = true
                                
                                // Hide WebView and show custom view
                                this@apply.visibility = View.GONE
                                
                                // Add custom view to container
                                (this@apply.parent as? ViewGroup)?.addView(
                                    view,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        }
                        
                        override fun onHideCustomView() {
                            super.onHideCustomView()
                            
                            // Remove custom view from container
                            customView?.let { view ->
                                (view.parent as? ViewGroup)?.removeView(view)
                            }
                            
                            // Show WebView again
                            this@apply.visibility = View.VISIBLE
                            
                            customView = null
                            customViewCallback = null
                            isFullscreen = false
                        }
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            // Only allow HTTPS URLs, block HTTP and all other schemes
                            return !url.startsWith("https://")
                        }
                        
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            Toast.makeText(context, "Error: $errorCode - $description", Toast.LENGTH_LONG).show()
                        }
                    }
                    
                    setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                        val request = DownloadManager.Request(Uri.parse(url)).apply {
                            setMimeType(mimeType)
                            addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
                            addRequestHeader("User-Agent", userAgent)
                            setDescription("Downloading...")
                            setTitle(Uri.parse(url).lastPathSegment)
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                Uri.parse(url).lastPathSegment
                            )
                        }
                        
                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        dm.enqueue(request)
                        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
                    }
                    
                    loadUrl("https://www.youtube.com")
                }
                
                webView = webViewInstance
                addView(webViewInstance)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}