package dev.theolm.core

/**
 * Configuration interface for BaseWebView
 */
interface WebViewConfiguration {
    val startUrl: String
    val enableDownloads: Boolean
    val enableFullscreen: Boolean
    val customJavaScript: String?
}