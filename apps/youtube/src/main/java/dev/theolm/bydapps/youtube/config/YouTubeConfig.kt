package dev.theolm.bydapps.youtube.config

import dev.theolm.core.ThemeMode
import dev.theolm.core.WebViewConfiguration

/**
 * YouTube-specific WebView configuration
 */
object YouTubeConfig : WebViewConfiguration {
    override val startUrl = "https://www.youtube.com"
    override val enableDownloads = true
    override val enableFullscreen = true
    override val themeMode = ThemeMode.DARK
    
    /**
     * Enable SPA navigation tracking for YouTube's single-page app architecture
     */
    override val enableSpaNavigationTracking = true
    
    /**
     * Delay JavaScript injection to allow YouTube's DOM to initialize
     */
    override val javascriptInjectionDelayMs = 500L
    
    /**
     * Media button handler for steering wheel controls
     */
    override val mediaButtonHandler = YouTubeMediaHandler()
    
    /**
     * JavaScript to force YouTube dark theme based on configuration
     * Sets localStorage PREF value and HTML dark attribute
     */
    override val customJavaScript = """
        (function() {
            'use strict';
            
            // Set localStorage preference first (critical for YouTube)
            try {
                const currentPref = localStorage.getItem('PREF') || '';
                const params = new URLSearchParams(currentPref);
                params.set('f6', '400'); // 400 = dark theme
                localStorage.setItem('PREF', params.toString());
            } catch (e) {
                console.error('Theme: Failed to set localStorage', e);
            }
            
            // Function to apply dark theme
            const applyDarkTheme = function() {
                const html = document.documentElement;
                if (!html) return;
                
                if (!html.hasAttribute('dark')) {
                    html.setAttribute('dark', '');
                }
            };
            
            // Apply immediately
            applyDarkTheme();
            
            // Apply when DOM is ready
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', applyDarkTheme);
            }
            
            // Apply on full page load
            window.addEventListener('load', applyDarkTheme);
            
            // Watch for dynamic changes
            const observer = new MutationObserver(function(mutations) {
                for (const mutation of mutations) {
                    if (mutation.type === 'attributes' && 
                        mutation.attributeName === 'dark' &&
                        !mutation.target.hasAttribute('dark')) {
                        applyDarkTheme();
                    }
                }
            });
            
            if (document.documentElement) {
                observer.observe(document.documentElement, {
                    attributes: true,
                    attributeFilter: ['dark']
                });
            }
            
            // Periodic reapplication (handles race conditions)
            let attempts = 0;
            const intervalId = setInterval(function() {
                applyDarkTheme();
                attempts++;
                if (attempts >= 10) clearInterval(intervalId);
            }, 500);
        })();
    """.trimIndent()
}
