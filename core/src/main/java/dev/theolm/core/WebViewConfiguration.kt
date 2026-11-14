package dev.theolm.core

/**
 * Configuration interface for BaseWebView
 */
interface WebViewConfiguration {
    val startUrl: String
    val enableDownloads: Boolean
    val enableFullscreen: Boolean
    val customJavaScript: String?
    val themeMode: ThemeMode
    
    /**
     * Enable tracking of URL changes in Single Page Applications (SPAs).
     * When enabled, custom JavaScript will be re-injected when the URL changes
     * without a full page reload.
     */
    val enableSpaNavigationTracking: Boolean
    
    /**
     * Delay in milliseconds before injecting custom JavaScript.
     * Useful for SPAs that need time to initialize their DOM.
     * Default: 0 (no delay)
     */
    val javascriptInjectionDelayMs: Long
    
    /**
     * Handler for media button events (steering wheel controls, headset buttons).
     * If null, media buttons will not be handled.
     */
    val mediaButtonHandler: MediaButtonHandler?

    /**
     * Cookies applied before the first page load, as URL to Set-Cookie header
     * pairs. Use for site preferences that the page only reads at startup,
     * such as YouTube's `PREF` theme cookie.
     */
    val cookies: List<Pair<String, String>> get() = emptyList()

    /**
     * Renders the page in a smaller logical viewport and scales it up to fill
     * the screen. This reflows responsive sites instead of just zooming fixed
     * elements, so text and controls get bigger without breaking the layout.
     * 1.0 leaves the WebView at its native size.
     */
    val uiScale: Float get() = 1f
}

/**
 * Theme mode options for the app
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
