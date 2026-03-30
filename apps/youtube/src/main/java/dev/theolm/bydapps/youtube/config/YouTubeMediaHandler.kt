package dev.theolm.bydapps.youtube.config

import android.webkit.WebView
import dev.theolm.core.MediaButtonHandler

/**
 * Media button handler for YouTube-specific navigation
 * Maps steering wheel/media buttons to YouTube keyboard shortcuts and player controls
 */
class YouTubeMediaHandler : MediaButtonHandler {
    
    override fun onMediaNext(webView: WebView) {
        // Try keyboard shortcut first (Shift+N), then button click as fallback
        webView.evaluateJavascript(
            """
            (function() {
                // Try Shift+N keyboard shortcut
                document.dispatchEvent(new KeyboardEvent('keydown', {
                    key: 'N',
                    code: 'KeyN',
                    shiftKey: true,
                    bubbles: true
                }));
                
                // Fallback: click next button after short delay
                setTimeout(function() {
                    const nextBtn = document.querySelector('.ytp-next-button') || 
                                   document.querySelector('button[title*="Next"]') ||
                                   document.querySelector('a.ytp-next-button');
                    if (nextBtn) {
                        nextBtn.click();
                    }
                }, 100);
            })();
            """,
            null
        )
    }
    
    override fun onMediaPrevious(webView: WebView) {
        // Try keyboard shortcut first (Shift+P), then button click as fallback
        // Note: Previous only works in playlists with Shift+P
        webView.evaluateJavascript(
            """
            (function() {
                // Try Shift+P keyboard shortcut
                document.dispatchEvent(new KeyboardEvent('keydown', {
                    key: 'P',
                    code: 'KeyP',
                    shiftKey: true,
                    bubbles: true
                }));
                
                // Fallback: click previous button after short delay
                setTimeout(function() {
                    const prevBtn = document.querySelector('.ytp-prev-button') ||
                                   document.querySelector('button[title*="Previous"]');
                    if (prevBtn) {
                        prevBtn.click();
                    }
                }, 100);
            })();
            """,
            null
        )
    }
    
    override fun onMediaPlayPause(webView: WebView) {
        // YouTube uses 'k' key for play/pause toggle
        webView.evaluateJavascript(
            """
            (function() {
                // Try 'k' keyboard shortcut
                document.dispatchEvent(new KeyboardEvent('keydown', {
                    key: 'k',
                    code: 'KeyK',
                    bubbles: true
                }));
                
                // Fallback: click play/pause button
                setTimeout(function() {
                    const playPauseBtn = document.querySelector('.ytp-play-button') ||
                                        document.querySelector('button[title*="Play"], button[title*="Pause"]');
                    if (playPauseBtn) {
                        playPauseBtn.click();
                    }
                }, 100);
            })();
            """,
            null
        )
    }
}
