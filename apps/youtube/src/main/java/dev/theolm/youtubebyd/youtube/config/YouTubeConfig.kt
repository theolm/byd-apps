package dev.theolm.youtubebyd.youtube.config

import dev.theolm.core.WebViewConfiguration

/**
 * YouTube-specific WebView configuration
 */
object YouTubeConfig : WebViewConfiguration {
    override val startUrl = "https://www.youtube.com"
    override val enableDownloads = true
    override val enableFullscreen = true
    override val customJavaScript = null
}