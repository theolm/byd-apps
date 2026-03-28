package dev.theolm.youtubebyd

/**
 * Constants used throughout the YouTube WebView application
 */
internal object YouTubeConstants {
    
    /**
     * YouTube mobile website URL
     */
    const val YOUTUBE_URL = "https://www.youtube.com"
    
    /**
     * Chrome mobile user agent for compatibility
     */
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    
    /**
     * HTTPS scheme prefix for URL validation
     */
    const val HTTPS_SCHEME = "https://"
    
    /**
     * JavaScript code to enable dark mode on YouTube
     */
    val DARK_MODE_JS = """
        (function() {
            document.documentElement.setAttribute('dark', '');
            document.documentElement.style.colorScheme = 'dark';
        })();
    """.trimIndent()
    
    /**
     * JavaScript code to enable light mode on YouTube
     */
    val LIGHT_MODE_JS = """
        (function() {
            document.documentElement.removeAttribute('dark');
            document.documentElement.style.colorScheme = 'light';
        })();
    """.trimIndent()
}