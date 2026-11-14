package dev.theolm.bydapps.ytmusic.config

import dev.theolm.core.ThemeMode
import dev.theolm.core.WebViewConfiguration
import org.json.JSONObject

/**
 * YT Music-specific WebView configuration
 */
object YTMusicConfig : WebViewConfiguration {
    override val startUrl = "https://music.youtube.com"
    override val enableDownloads = true
    override val enableFullscreen = true
    override val themeMode = ThemeMode.DARK

    override val cookies = listOf(
        "https://music.youtube.com" to "PREF=f6=400; Domain=.youtube.com; Path=/"
    )

    /**
     * The desktop player bar is too small at 1280 CSS pixels. A 1.3x scale
     * gives the page a ~984 px viewport, just below its tablet breakpoint, and
     * scales text and controls up to driving distance.
     */
    override val uiScale = 1.3f

    /**
     * Returns the current track's (or first visible card's) action-menu labels
     * as a JSON array. The native Compose bottom sheet renders these labels,
     * so the commands follow the page's language automatically.
     */
    val menuItemsScript = """
        (function() {
            var menus = document.querySelectorAll('ytmusic-menu-renderer');
            var menu = null;
            var i;
            for (i = 0; i < menus.length; i++) {
                var rect = menus[i].getBoundingClientRect();
                if (rect.width > 0 && rect.top > innerHeight * 0.7) {
                    menu = menus[i];
                    break;
                }
            }
            if (!menu) {
                for (i = 0; i < menus.length; i++) {
                    if (menus[i].getBoundingClientRect().width > 0) {
                        menu = menus[i];
                        break;
                    }
                }
            }
            if (!menu || !menu.data || !menu.data.items) return [];
            var labels = [];
            for (i = 0; i < menu.data.items.length; i++) {
                var item = menu.data.items[i];
                var renderer = item.menuNavigationItemRenderer || item.menuServiceItemRenderer;
                var runs = renderer && renderer.text && renderer.text.runs;
                if (runs && runs.length && runs[0].text) {
                    labels.push(runs[0].text);
                }
            }
            return labels;
        })();
    """.trimIndent()

    /**
     * Opens the site's action popup and clicks the item with the given label.
     * The popup itself is kept transparent: the visible UI is the native sheet.
     */
    fun clickMenuItemScript(label: String): String {
        val quoted = JSONObject.quote(label)
        return """
            (function() {
                var menus = document.querySelectorAll('ytmusic-menu-renderer');
                var menu = null;
                var i;
                for (i = 0; i < menus.length; i++) {
                    var rect = menus[i].getBoundingClientRect();
                    if (rect.width > 0 && rect.top > innerHeight * 0.7) {
                        menu = menus[i];
                        break;
                    }
                }
                if (!menu) {
                    for (i = 0; i < menus.length; i++) {
                        if (menus[i].getBoundingClientRect().width > 0) {
                            menu = menus[i];
                            break;
                        }
                    }
                }
                if (!menu) return;
                var button = menu.getMenuButton && menu.getMenuButton();
                if (button) button.click();
                setTimeout(function() {
                    var dropdown = document.querySelector('tp-yt-iron-dropdown');
                    if (dropdown) {
                        dropdown.style.opacity = '0';
                        dropdown.style.pointerEvents = 'none';
                    }
                    var items = document.querySelectorAll(
                        'ytmusic-menu-service-item-renderer, ytmusic-menu-navigation-item-renderer'
                    );
                    for (var j = 0; j < items.length; j++) {
                        if ((items[j].innerText || '').trim() === $quoted) {
                            items[j].click();
                            break;
                        }
                    }
                    setTimeout(function() {
                        if (dropdown) {
                            dropdown.style.opacity = '';
                            dropdown.style.pointerEvents = '';
                        }
                    }, 500);
                }, 150);
            })();
        """.trimIndent()
    }
    
    /**
     * Enable SPA navigation tracking for YT Music's single-page app architecture
     */
    override val enableSpaNavigationTracking = true
    
    /**
     * Delay JavaScript injection to allow YT Music's DOM to initialize
     */
    override val javascriptInjectionDelayMs = 500L
    
    /**
     * Media button handler for steering wheel controls
     */
    override val mediaButtonHandler = YTMusicMediaHandler()
    
    /**
     * JavaScript to force YT Music dark theme
     * Sets localStorage and HTML dark attribute
     */
    override val customJavaScript = """
        (function() {
            'use strict';
            
            // Set localStorage preference for dark theme
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

            // There is no native YouTube Music app on the head unit, so the
            // "Open in the YouTube Music app" prompt is a dead end. Dismiss it
            // and hide the matching guide entry.
            function dismissOpenAppPrompt() {
                var buttons = document.querySelectorAll('button, tp-yt-paper-button');
                for (var i = 0; i < buttons.length; i++) {
                    var text = (buttons[i].innerText || '').trim().toLowerCase();
                    if (text === 'not now' || text === 'agora não' ||
                        text === 'later' || text === 'mais tarde') {
                        buttons[i].click();
                        return;
                    }
                }
            }

            function hideOpenAppEntry() {
                var entries = document.querySelectorAll('ytmusic-guide-entry-renderer');
                for (var j = 0; j < entries.length; j++) {
                    if ((entries[j].innerText || '').toLowerCase().indexOf('open app') >= 0) {
                        entries[j].style.display = 'none';
                    }
                }
            }

            function cleanUpWebAppPrompts() {
                dismissOpenAppPrompt();
                hideOpenAppEntry();
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

            // The tablet layout ships a 129 px guide icon and a 10 px player
            // bar. Normalize the guide and enlarge the player controls; the
            // surrounding viewport is already scaled by uiScale.
            function installUiFixes() {
                if (document.getElementById('byd-ui-fixes')) return;
                var style = document.createElement('style');
                style.id = 'byd-ui-fixes';
                style.textContent = `
                    ytmusic-guide-renderer {
                        width: 240px !important;
                        min-width: 240px !important;
                    }
                    ytmusic-guide-entry-renderer tp-yt-paper-item {
                        height: 48px !important;
                        min-height: 48px !important;
                        padding: 0 16px !important;
                    }
                    ytmusic-guide-entry-renderer yt-icon.guide-icon {
                        width: 24px !important;
                        height: 24px !important;
                        min-width: 24px !important;
                        min-height: 24px !important;
                        flex: 0 0 24px !important;
                        --iron-icon-width: 24px;
                        --iron-icon-height: 24px;
                    }
                    ytmusic-guide-entry-renderer {
                        font-size: 14px !important;
                    }
                    ytmusic-guide-entry-renderer yt-formatted-string,
                    ytmusic-guide-entry-renderer .title {
                        font-size: 14px !important;
                    }
                    ytmusic-player-bar {
                        font-size: 16px !important;
                    }
                    ytmusic-player-bar .title,
                    ytmusic-player-bar .byline,
                    ytmusic-player-bar yt-formatted-string {
                        font-size: 15px !important;
                    }
                    ytmusic-player-bar yt-icon-button,
                    ytmusic-player-bar button {
                        width: 32px !important;
                        height: 32px !important;
                    }
                    ytmusic-queue-header-renderer ytmusic-chip-cloud-chip-renderer {
                        display: none !important;
                    }
                `;
                (document.head || document.documentElement).appendChild(style);
            }

            installUiFixes();
            setTimeout(installUiFixes, 1000);
            setTimeout(installUiFixes, 3000);
        })();
    """.trimIndent()
}
