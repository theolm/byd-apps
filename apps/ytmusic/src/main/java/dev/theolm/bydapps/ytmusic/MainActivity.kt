package dev.theolm.bydapps.ytmusic

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.theolm.bydapps.ytmusic.config.YTMusicConfig
import dev.theolm.bydapps.ytmusic.service.YTMusicPlaybackService
import dev.theolm.bydapps.ytmusic.ui.theme.YTMusicTheme
import dev.theolm.core.webview.BaseWebView
import org.json.JSONArray

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            YTMusicTheme {
                var menuItems by remember { mutableStateOf<List<String>>(emptyList()) }
                var showSheet by remember { mutableStateOf(false) }
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                Box(modifier = Modifier.fillMaxSize()) {
                    BaseWebView(
                        activity = this@MainActivity,
                        configuration = YTMusicConfig,
                        onWebViewCreated = {
                            webView = it
                            YTMusicPlaybackService.WebViewHolder.getInstance().webView = it
                        },
                        onFullscreenChanged = { isFullscreen ->
                            handleFullscreenChange(isFullscreen)
                        },
                        onBack = {
                            finish()
                        }
                    )

                    val menuDescription = stringResource(R.string.menu_description)
                    FloatingActionButton(
                        onClick = {
                            webView?.evaluateJavascript(
                                YTMusicConfig.menuItemsScript
                            ) { result ->
                                val labels = parseMenuLabels(result)
                                if (labels.isNotEmpty()) {
                                    menuItems = labels
                                    showSheet = true
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 112.dp)
                            .semantics { contentDescription = menuDescription },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.menu_symbol),
                            fontSize = 28.sp
                        )
                    }
                }

                if (showSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showSheet = false },
                        sheetState = sheetState
                    ) {
                        Text(
                            text = stringResource(R.string.menu_description),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            fontSize = 20.sp
                        )
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(menuItems) { label ->
                                TextButton(
                                    onClick = {
                                        webView?.evaluateJavascript(
                                            YTMusicConfig.clickMenuItemScript(label),
                                            null
                                        )
                                        showSheet = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.fillMaxWidth(),
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.startForegroundService(
            this,
            Intent(this, YTMusicPlaybackService::class.java)
        )
    }

    override fun onResume() {
        super.onResume()
        webView?.let {
            YTMusicPlaybackService.WebViewHolder.getInstance().webView = it
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val handler = YTMusicConfig.mediaButtonHandler
        handler?.let {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    webView?.let { handler.onMediaNext(it) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    webView?.let { handler.onMediaPrevious(it) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    webView?.let { handler.onMediaPlayPause(it) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    webView?.let { handler.onMediaPlay(it) }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    webView?.let { handler.onMediaPause(it) }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun parseMenuLabels(result: String?): List<String> {
        if (result.isNullOrBlank() || result == "null") return emptyList()
        return try {
            val array = JSONArray(result)
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun handleFullscreenChange(isFullscreen: Boolean) {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (isFullscreen) {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
