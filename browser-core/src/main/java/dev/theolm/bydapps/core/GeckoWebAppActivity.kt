package dev.theolm.bydapps.core

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import dev.theolm.bydapps.core.session.SessionBridge
import dev.theolm.bydapps.core.session.SessionSync
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.Image
import org.mozilla.geckoview.MediaSession
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Shared GeckoView activity for the YouTube and YT Music packages.
 *
 * The rendering engine is embedded in each APK, so the head unit's system
 * WebView version no longer matters. The page is rendered at 1x, with dark
 * color scheme and a native MediaSession that forwards transport controls to
 * the page's media session.
 */
abstract class GeckoWebAppActivity : ComponentActivity() {

    protected abstract val startUrl: String

    /**
     * Packages that may provide a signed-in session when this app is signed
     * out. Empty when the app has no sibling.
     */
    protected open val partnerPackages: List<String> = emptyList()

    /**
     * Page scale relative to the device density. Values above 1 render the
     * site larger; YouTube keeps 1.0 and YT Music overrides this with 1.2.
     */
    protected open val contentScale: Float = 1f

    /**
     * Display mode reported to the page through the `display-mode` media
     * query. The default is browser mode; a site that ships a web app manifest
     * can opt into [GeckoSessionSettings.DISPLAY_MODE_MINIMAL_UI] so it hides
     * its own install prompts, even though the APK owns the window chrome.
     */
    protected open val displayMode: Int = GeckoSessionSettings.DISPLAY_MODE_BROWSER

    /** User agent mode; desktop gives sites their full web layout. */
    protected open val userAgentMode: Int = GeckoSessionSettings.USER_AGENT_MODE_MOBILE

    private lateinit var mediaSessionCompat: MediaSessionCompat
    private var session: GeckoSession? = null
    private var sessionSync: SessionSync? = null
    private var geckoMediaSession: MediaSession? = null
    private var lastMetadata: MediaSession.Metadata? = null
    private var lastPosition = 0.0
    private var isPlaying = false
    private var keepPlayingInBackground = false
    private var canGoBack = false
    private var currentUrl: String? = null
    private var startPageStarted = false
    private var playbackServiceStarted = false
    private var notificationPermissionRequested = false
    private var artwork: Bitmap? = null
    private var artworkUri: String? = null
    private var artworkRequest = 0
    private var fullScreen = false
    private var splashView: View? = null
    private var splashDismissed = false
    private var startupStartedAt = 0L
    private var errorDialog: AlertDialog? = null
    private var contentCrashed = false

    /** Covers the GeckoView until the first paint so launch feels like an app. */
    private val splashTimeout = Runnable { hideSplash() }

    private val loadErrorRunnable = Runnable { showLoadError() }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val keepAliveRunnable = Runnable {
        if (!keepPlayingInBackground) return@Runnable
        val activeSession = session ?: return@Runnable
        if (!activeSession.isOpen) return@Runnable
        activeSession.setActive(true)
        if (!isPlaying) {
            geckoMediaSession?.play()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        startupStartedAt = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Startup: onCreate")
        WindowCompat.setDecorFitsSystemWindows(window, true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    fullScreen -> session?.exitFullScreen()
                    canGoBack -> session?.goBack()
                    else -> finish()
                }
            }
        })

        val runtime = getOrCreateRuntime(applicationContext, contentScale)
        runtime.warmUp()
        Log.d(TAG, "Startup: runtime ready +${startupElapsed()} ms (warmUp)")

        setupMediaSession()
        Log.d(TAG, "Startup: media session ready +${startupElapsed()} ms")

        val settings = GeckoSessionSettings.Builder()
            .userAgentMode(userAgentMode)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .displayMode(displayMode)
            .allowJavascript(true)
            .suspendMediaWhenInactive(false)
            .build()

        val geckoSession = GeckoSession(settings)
        geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                this@GeckoWebAppActivity.canGoBack = canGoBack
            }
        }
        geckoSession.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onFirstContentfulPaint(session: GeckoSession) {
                Log.d(TAG, "Startup: first contentful paint +${startupElapsed()} ms")
                hideSplash()
                window.decorView.removeCallbacks(loadErrorRunnable)
                dismissErrorDialog()
                contentCrashed = false
            }

            override fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {
                Log.d(
                    TAG,
                    "Web app manifest: name=${manifest.optString("name")}, " +
                        "short_name=${manifest.optString("short_name")}, " +
                        "display=${manifest.optString("display")}, " +
                        "start_url=${manifest.optString("start_url")}, " +
                        "theme_color=${manifest.optString("theme_color")}, " +
                        "background_color=${manifest.optString("background_color")}"
                )
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                Log.d(TAG, "Fullscreen=$fullScreen")
                this@GeckoWebAppActivity.fullScreen = fullScreen
                // YT Music can open its player in fullscreen. This wrapper has
                // no fullscreen chrome, so that hides the queue and transport
                // bar; exit after the transition and keep the normal page.
                if (fullScreen) {
                    window.decorView.postDelayed(
                        { if (session.isOpen) session.exitFullScreen() },
                        FULLSCREEN_EXIT_DELAY_MS
                    )
                }
            }

            override fun onCrash(session: GeckoSession) {
                Log.e(TAG, "Content process crashed")
                handleContentProcessLost()
            }

            override fun onKill(session: GeckoSession) {
                Log.e(TAG, "Content process killed")
                handleContentProcessLost()
            }
        }
        geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                Log.d(TAG, "Startup: page start $url +${startupElapsed()} ms")
                currentUrl = url
                if (isStartPage(url)) {
                    startPageStarted = true
                }
                window.decorView.removeCallbacks(loadErrorRunnable)
                dismissErrorDialog()
                contentCrashed = false
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                Log.d(TAG, "Startup: page stop success=$success +${startupElapsed()} ms")
                if (!success) {
                    hideSplash()
                    // A redirect or a quick retry can stop a load without a
                    // real failure; only show the dialog if no new page starts.
                    window.decorView.removeCallbacks(loadErrorRunnable)
                    window.decorView.postDelayed(loadErrorRunnable, LOAD_ERROR_DELAY_MS)
                }
            }
        }
        geckoSession.mediaSessionDelegate = object : MediaSession.Delegate {
            override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
                geckoMediaSession = mediaSession
                mediaSessionCompat.isActive = true
                updateNativeState()
                startPlaybackService()
            }

            override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
                if (geckoMediaSession === mediaSession) {
                    geckoMediaSession = null
                    lastMetadata = null
                    lastPosition = 0.0
                    artwork = null
                    artworkUri = null
                    artworkRequest++
                }
                isPlaying = false
                mediaSessionCompat.isActive = false
                updateNativeState()
                stopPlaybackService()
            }

            override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
                mediaSessionCompat.isActive = true
                isPlaying = true
                updateNativeState()
                startPlaybackService()
            }

            override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
                isPlaying = false
                updateNativeState()
            }

            override fun onStop(session: GeckoSession, mediaSession: MediaSession) {
                isPlaying = false
                updateNativeState()
                stopPlaybackService()
            }

            override fun onMetadata(
                session: GeckoSession,
                mediaSession: MediaSession,
                metadata: MediaSession.Metadata
            ) {
                lastMetadata = metadata
                artwork = null
                artworkUri = null
                loadArtwork(metadata.artwork)
                updateNativeState()
            }

            override fun onPositionState(
                session: GeckoSession,
                mediaSession: MediaSession,
                positionState: MediaSession.PositionState
            ) {
                lastPosition = positionState.position
                updateNativeState()
            }

            override fun onFeatures(
                session: GeckoSession,
                mediaSession: MediaSession,
                features: Long
            ) {
                updateNativeState()
            }
        }
        geckoSession.open(runtime)
        session = geckoSession

        val geckoView = GeckoView(this).apply {
            setBackgroundColor(Color.BLACK)
            setSession(geckoSession)
        }
        setContentView(createContentView(geckoView))
        Log.d(TAG, "Startup: view ready +${startupElapsed()} ms")
        if (!splashDismissed) {
            window.decorView.postDelayed(splashTimeout, SPLASH_TIMEOUT_MS)
        }

        // Start the first load immediately; session restore runs in parallel
        // and only asks for a reload if it lands after the page has started.
        Log.d(TAG, "Startup: loadUri +${startupElapsed()} ms")
        geckoSession.loadUri(startUrl)

        val bridge = SessionBridge.get(runtime)
        bridge.start()
        val sync = SessionSync(applicationContext, partnerPackages, bridge)
        sessionSync = sync
        sync.prepare(
            onLateImport = {
                // Reload only while the start page is still showing. A late
                // import must not interrupt a song, video or deeper page.
                if (!isDestroyed && geckoSession.isOpen && !isPlaying &&
                    startPageStarted && isStartPage(currentUrl)
                ) {
                    Log.d(TAG, "Late session import; reloading start page")
                    geckoSession.reload()
                }
            },
        )
        Log.d(TAG, "Startup: onCreate complete +${startupElapsed()} ms")
    }

    private fun startupElapsed(): Long = SystemClock.elapsedRealtime() - startupStartedAt

    private fun isStartPage(url: String?): Boolean {
        if (url == null) return false
        val current = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val start = Uri.parse(startUrl)
        val path = current.path ?: ""
        return current.host == start.host && (path.isEmpty() || path == "/")
    }

    /**
     * Wraps the GeckoView in a black container and shows the app icon until
     * the first paint. An installed PWA gets a similar splash from its web app
     * manifest; this keeps the same feel while Gecko boots.
     */
    private fun createContentView(geckoView: GeckoView): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        root.addView(
            geckoView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        if (splashDismissed) return root

        val iconSize = (SPLASH_ICON_DP * resources.displayMetrics.density).toInt()
        val icon = ImageView(this).apply {
            setImageDrawable(packageManager.getApplicationIcon(applicationInfo))
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val label = TextView(this).apply {
            text = packageManager.getApplicationLabel(applicationInfo)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
        }
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            // Consume taps while the page is loading; the splash goes away on
            // the first paint or after the timeout.
            isClickable = true
            addView(icon, LinearLayout.LayoutParams(iconSize, iconSize))
            addView(
                label,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (16 * resources.displayMetrics.density).toInt()
                }
            )
        }
        root.addView(
            splash,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        splashView = splash
        return root
    }

    private fun hideSplash() {
        if (splashDismissed) return
        splashDismissed = true
        window.decorView.removeCallbacks(splashTimeout)
        val splash = splashView ?: return
        splashView = null
        splash.animate()
            .alpha(0f)
            .setDuration(SPLASH_FADE_MS)
            .withEndAction {
                (splash.parent as? FrameLayout)?.removeView(splash)
            }
            .start()
    }

    private fun handleContentProcessLost() {
        contentCrashed = true
        hideSplash()
        window.decorView.removeCallbacks(loadErrorRunnable)
        showLoadError()
    }

    private fun showLoadError() {
        if (isFinishing || isDestroyed || errorDialog != null) return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return

        val crashed = contentCrashed
        errorDialog = AlertDialog.Builder(this)
            .setTitle(R.string.session_error_title)
            .setMessage(
                if (crashed) R.string.session_crashed_message
                else R.string.session_error_message
            )
            .setPositiveButton(R.string.session_error_retry) { _, _ ->
                errorDialog = null
                val active = session
                if (crashed || active == null || !active.isOpen) {
                    recreate()
                } else {
                    active.reload()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun dismissErrorDialog() {
        errorDialog?.dismiss()
        errorDialog = null
    }

    private fun setupMediaSession() {
        mediaSessionCompat = MediaSessionCompat(this, packageName).apply {
            setSessionActivity(
                PendingIntent.getActivity(
                    this@GeckoWebAppActivity,
                    0,
                    Intent(this@GeckoWebAppActivity, this@GeckoWebAppActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    geckoMediaSession?.play()
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    if (mediaId == MediaPlaybackService.NOW_PLAYING_ID) {
                        geckoMediaSession?.play()
                    }
                }

                override fun onPause() {
                    // This is a user/steering-wheel pause; do not let the
                    // background keep-alive start playback again.
                    keepPlayingInBackground = false
                    geckoMediaSession?.pause()
                }

                override fun onSkipToNext() {
                    geckoMediaSession?.nextTrack()
                }

                override fun onSkipToPrevious() {
                    geckoMediaSession?.previousTrack()
                }

                override fun onStop() {
                    keepPlayingInBackground = false
                    geckoMediaSession?.stop()
                }

                override fun onSeekTo(pos: Long) {
                    geckoMediaSession?.seekTo(pos / 1000.0, true)
                }
            })
            // Inactive until the page actually has a media session. Keeping
            // this true in every app would steal steering-wheel keys from the
            // app that is really playing.
            isActive = false
        }
        MediaSessionRegistry.token = mediaSessionCompat.sessionToken
        updateNativeState()
    }

    private fun loadArtwork(image: Image?) {
        val request = ++artworkRequest
        if (image == null) {
            artwork = null
            return
        }
        image.getBitmap(ARTWORK_SIZE).accept({ bitmap ->
            if (request != artworkRequest || bitmap == null) return@accept
            runOnUiThread {
                if (request != artworkRequest) return@runOnUiThread
                publishArtwork(bitmap, request)
            }
        }, { throwable ->
            Log.w(TAG, "Artwork load failed", throwable)
        })
    }

    private fun publishArtwork(bitmap: Bitmap, request: Int) {
        val file = File(cacheDir, MediaArtworkProvider.ARTWORK_FILE)
        val written = try {
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (error: IOException) {
            Log.w(TAG, "Artwork save failed", error)
            false
        }

        artwork = bitmap
        artworkUri = if (written) {
            "content://${packageName}${MediaArtworkProvider.AUTHORITY_SUFFIX}/current?v=$request"
        } else {
            null
        }
        updateNativeState()
        Log.d(TAG, "Artwork loaded ${bitmap.width}x${bitmap.height}, uri=$artworkUri")
    }

    private fun updateNativeState() {
        val metadata = lastMetadata
        val metadataBuilder = MediaMetadataCompat.Builder()
        metadata?.title?.let {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, it)
        }
        metadata?.artist?.let {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it)
        }
        metadata?.album?.let {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it)
        }
        artwork?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
            artworkUri?.let { uri ->
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, uri)
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, uri)
            }
        }
        mediaSessionCompat.setMetadata(metadataBuilder.build())

        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP

        mediaSessionCompat.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    (lastPosition * 1000).toLong(),
                    if (isPlaying) 1f else 0f
                )
                .build()
        )
    }

    override fun onPause() {
        super.onPause()
        keepPlayingInBackground = isPlaying
        if (keepPlayingInBackground) {
            keepSessionActive()
        }
    }

    override fun onStop() {
        super.onStop()
        // Export the signed-in cookies so the sibling app can log in without
        // asking Google again. Safe to call on every stop: a signed-out app
        // never overwrites the stored bundle.
        sessionSync?.save()
        if (keepPlayingInBackground) {
            // GeckoView deactivates the session when its surface is hidden,
            // and the page treats that as a visibility change. Keep it active
            // while media is playing so camera/app switches do not pause or
            // reload the site.
            keepSessionActive()
        }
    }

    override fun onResume() {
        super.onResume()
        keepPlayingInBackground = false
        window.decorView.removeCallbacks(keepAliveRunnable)
        session?.setActive(true)
        if (contentCrashed) showLoadError()
    }

    /**
     * The surface release can deactivate the session after onPause/onStop, so
     * re-activate it once the window has settled and resume playback if the
     * site paused on visibility change.
     */
    private fun keepSessionActive() {
        session?.setActive(true)
        window.decorView.removeCallbacks(keepAliveRunnable)
        window.decorView.postDelayed(keepAliveRunnable, 750L)
    }

    private fun startPlaybackService() {
        if (playbackServiceStarted) return
        val intent = Intent(this, MediaPlaybackService::class.java)
            .putExtra(
                MediaPlaybackService.EXTRA_SESSION_TOKEN,
                mediaSessionCompat.sessionToken
            )
        try {
            ContextCompat.startForegroundService(this, intent)
            playbackServiceStarted = true
        } catch (_: IllegalStateException) {
            // Android 12+ can reject a foreground-service start from the
            // background. Playback still has the Gecko keep-alive fallback.
            playbackServiceStarted = false
        }
        ensureNotificationPermission()
    }

    private fun stopPlaybackService() {
        if (!playbackServiceStarted) return
        stopService(Intent(this, MediaPlaybackService::class.java))
        playbackServiceStarted = false
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (notificationPermissionRequested) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        notificationPermissionRequested = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onDestroy() {
        window.decorView.removeCallbacks(keepAliveRunnable)
        window.decorView.removeCallbacks(splashTimeout)
        window.decorView.removeCallbacks(loadErrorRunnable)
        splashView = null
        dismissErrorDialog()
        stopPlaybackService()
        session?.close()
        if (MediaSessionRegistry.token == mediaSessionCompat.sessionToken) {
            MediaSessionRegistry.token = null
        }
        mediaSessionCompat.release()
        super.onDestroy()
    }

    private companion object {
        private const val TAG = "GeckoWebAppActivity"
        private const val ARTWORK_SIZE = 256
        private const val SPLASH_ICON_DP = 128
        private const val SPLASH_FADE_MS = 200L
        private const val SPLASH_TIMEOUT_MS = 8_000L
        private const val FULLSCREEN_EXIT_DELAY_MS = 1_000L
        private const val LOAD_ERROR_DELAY_MS = 800L

        @Volatile
        private var sharedRuntime: GeckoRuntime? = null

        /**
         * The runtime must be created once per process: create() rejects a
         * second instance, and the density override has to be set before
         * initialization. Reusing it also survives Activity recreation.
         */
        fun getOrCreateRuntime(context: Context, contentScale: Float): GeckoRuntime {
            sharedRuntime?.let { return it }
            synchronized(this) {
                sharedRuntime?.let { return it }
                val density = context.resources.displayMetrics.density * contentScale
                return GeckoRuntime.create(
                    context,
                    GeckoRuntimeSettings.Builder()
                        .displayDensityOverride(density)
                        // ETP standard was tested here: enabling the content
                        // blocking database added several seconds to the first
                        // paint on the DiLink-sized emulator, so it stays off.
                        .build()
                ).also {
                    // Apply after init so Gecko propagates the dark scheme to
                    // already-loaded content; the builder alone left YouTube light.
                    it.settings.setPreferredColorScheme(
                        GeckoRuntimeSettings.COLOR_SCHEME_DARK
                    )
                    sharedRuntime = it
                }
            }
        }
    }
}
