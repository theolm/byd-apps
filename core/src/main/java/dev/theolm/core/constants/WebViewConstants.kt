package dev.theolm.core.constants

/**
 * Constants used throughout the WebView applications
 */
object WebViewConstants {
    
    /**
     * HTTPS scheme prefix for URL validation
     */
    const val HTTPS_SCHEME = "https://"
    
    /**
     * JavaScript code to enable dark mode
     */
    val DARK_MODE_JS = """
        (function() {
            document.documentElement.setAttribute('dark', '');
            document.documentElement.style.colorScheme = 'dark';
        })();
    """.trimIndent()
    
    /**
     * JavaScript code to enable light mode
     */
    val LIGHT_MODE_JS = """
        (function() {
            document.documentElement.removeAttribute('dark');
            document.documentElement.style.colorScheme = 'light';
        })();
    """.trimIndent()
}