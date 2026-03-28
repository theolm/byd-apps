package dev.theolm.youtubebyd

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.theolm.youtubebyd.ui.theme.YouTubeBYDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
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
    val isSystemDark = isSystemInDarkTheme()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    
    // Inject theme JavaScript whenever system theme changes
    LaunchedEffect(isSystemDark) {
        webView?.let { wv ->
            val jsCode = if (isSystemDark) {
                """
                    (function() {
                        document.documentElement.setAttribute('dark', '');
                        document.documentElement.style.colorScheme = 'dark';
                    })();
                """.trimIndent()
            } else {
                """
                    (function() {
                        document.documentElement.removeAttribute('dark');
                        document.documentElement.style.colorScheme = 'light';
                    })();
                """.trimIndent()
            }
            wv.evaluateJavascript(jsCode, null)
        }
    }
    
    BackHandler {
        when {
            isFullscreen && customViewCallback != null -> {
                customViewCallback?.onCustomViewHidden()
            }
            webView?.canGoBack() == true -> {
                webView?.goBack()
            }
            else -> {
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
                    
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            super.onShowCustomView(view, callback)
                            if (view != null && callback != null) {
                                customView = view
                                customViewCallback = callback
                                isFullscreen = true
                                this@apply.visibility = View.GONE
                                
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
                            
                            customView?.let { view ->
                                (view.parent as? ViewGroup)?.removeView(view)
                            }
                            
                            this@apply.visibility = View.VISIBLE
                            
                            customView = null
                            customViewCallback = null
                            isFullscreen = false
                        }
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Inject theme after page loads
                            val jsCode = if (isSystemDark) {
                                """
                                    (function() {
                                        document.documentElement.setAttribute('dark', '');
                                        document.documentElement.style.colorScheme = 'dark';
                                    })();
                                """.trimIndent()
                            } else {
                                """
                                    (function() {
                                        document.documentElement.removeAttribute('dark');
                                        document.documentElement.style.colorScheme = 'light';
                                    })();
                                """.trimIndent()
                            }
                            view?.evaluateJavascript(jsCode, null)
                        }
                        
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            return !url.startsWith("https://")
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
                    
                    setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                        val request = DownloadManager.Request(url.toUri()).apply {
                            setMimeType(mimeType)
                            addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
                            addRequestHeader("User-Agent", userAgent)
                            setDescription("Downloading...")
                            setTitle(url.toUri().lastPathSegment)
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                url.toUri().lastPathSegment
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