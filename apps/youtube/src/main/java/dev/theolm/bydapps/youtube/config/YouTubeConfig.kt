package dev.theolm.bydapps.youtube.config

import dev.theolm.core.ThemeMode
import dev.theolm.core.WebViewConfiguration

/**
 * YouTube-specific WebView configuration
 */
object YouTubeConfig : WebViewConfiguration {
    /**
     * The mobile site has touch-sized controls. The desktop site is laid out
     * for a 1280x604 CSS viewport on the head unit and its controls are too
     * small at driving distance.
     */
    override val startUrl = "https://m.youtube.com"
    override val enableDownloads = true
    override val enableFullscreen = true
    override val themeMode = ThemeMode.DARK

    /**
     * The mobile site only reads its theme preference at startup, so the
     * `PREF` cookie must be present before the first load. Also remove the
     * persisted "desktop site" choice: a car that once opened YouTube's
     * desktop link would otherwise keep the small layout across launches.
     */
    override val cookies = listOf(
        "https://www.youtube.com" to "PREF=f6=400; Domain=.youtube.com; Path=/",
        "https://m.youtube.com" to "PREF=f6=400; Domain=.youtube.com; Path=/",
        "https://www.youtube.com" to "persist_app=; Domain=.youtube.com; Path=/; Max-Age=0",
        "https://m.youtube.com" to "persist_app=; Domain=.youtube.com; Path=/; Max-Age=0"
    )
    
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

            // The mobile page advertises a native YouTube app that does not
            // exist on the head unit. Hide the header button and dismiss any
            // "Open in app" prompt.
            function dismissOpenAppPrompt() {
                var buttons = document.querySelectorAll('button, a, [role="button"]');
                for (var i = 0; i < buttons.length; i++) {
                    var text = (buttons[i].innerText || '').trim().toLowerCase();
                    if (text === 'not now' || text === 'agora não' ||
                        text === 'later' || text === 'mais tarde') {
                        buttons[i].click();
                        return;
                    }
                }
            }

            function hideOpenAppButton() {
                var elements = document.querySelectorAll('button, a, [role="button"]');
                for (var j = 0; j < elements.length; j++) {
                    var text = (elements[j].innerText || '').trim().toLowerCase();
                    if (text === 'open app' || text === 'abrir app' ||
                        text === 'open in app' || text === 'abrir no app') {
                        elements[j].style.display = 'none';
                    }
                }
            }

            function cleanUpWebAppPrompts() {
                dismissOpenAppPrompt();
                hideOpenAppButton();
            }

            cleanUpWebAppPrompts();
            if (document.body) {
                var promptScheduled = false;
                new MutationObserver(function() {
                    if (promptScheduled) return;
                    promptScheduled = true;
                    setTimeout(function() {
                        promptScheduled = false;
                        cleanUpWebAppPrompts();
                    }, 250);
                }).observe(document.body, { childList: true, subtree: true });
            }
            setTimeout(cleanUpWebAppPrompts, 1000);
            setTimeout(cleanUpWebAppPrompts, 3000);
        })();
    """.trimIndent()
}
