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
}

/**
 * Theme mode options for the app
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
