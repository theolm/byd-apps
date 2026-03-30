package dev.theolm.bydapps.ytmusic.config

import android.webkit.WebView
import dev.theolm.core.MediaButtonHandler

/**
 * Media button handler for YT Music-specific navigation
 * Maps steering wheel/media buttons to YT Music player controls
 */
class YTMusicMediaHandler : MediaButtonHandler {
    
    override fun onMediaNext(webView: WebView) {
        // Try keyboard shortcut first, then button click as fallback
        webView.evaluateJavascript(
            """
            (function() {
                // Try media key events first
                document.dispatchEvent(new KeyboardEvent('keydown', {
                    key: 'MediaTrackNext',
                    code: 'MediaTrackNext',
                    bubbles: true
                }));
                
                // Try Shift+N as fallback
                setTimeout(function() {
                    document.dispatchEvent(new KeyboardEvent('keydown', {
                        key: 'N',
                        code: 'KeyN',
                        shiftKey: true,
                        bubbles: true
                    }));
                }, 50);
                
                // Fallback: click next button
                setTimeout(function() {
                    const nextBtn = document.querySelector('[data-test-id="next-button"]') || 
                                   document.querySelector('.next-button') ||
                                   document.querySelector('[aria-label*="Next"]') ||
                                   document.querySelector('tp-yt-paper-icon-button[aria-label*="next"]');
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
        webView.evaluateJavascript(
            """
            (function() {
                // Try media key events first
                document.dispatchEvent(new KeyboardEvent('keydown', {
                    key: 'MediaTrackPrevious',
                    code: 'MediaTrackPrevious',
                    bubbles: true
                }));
                
                // Try Shift+P as fallback
                setTimeout(function() {
                    document.dispatchEvent(new KeyboardEvent('keydown', {
                        key: 'P',
                        code: 'KeyP',
                        shiftKey: true,
                        bubbles: true
                    }));
                }, 50);
                
                // Fallback: click previous button
                setTimeout(function() {
                    const prevBtn = document.querySelector('[data-test-id="previous-button"]') ||
                                   document.querySelector('.previous-button') ||
                                   document.querySelector('[aria-label*="Previous"]') ||
                                   document.querySelector('tp-yt-paper-icon-button[aria-label*="previous"]');
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
        webView.evaluateJavascript(
            """
            (function() {
                // Try media key events first
                document.dispatchEvent(new KeyboardEvent('keydown', {
                    key: 'MediaPlayPause',
                    code: 'MediaPlayPause',
                    bubbles: true
                }));
                
                // Try spacebar as fallback
                setTimeout(function() {
                    document.dispatchEvent(new KeyboardEvent('keydown', {
                        key: ' ',
                        code: 'Space',
                        bubbles: true
                    }));
                }, 50);
                
                // Fallback: click play/pause button
                setTimeout(function() {
                    const playPauseBtn = document.querySelector('[data-test-id="play-pause-button"]') ||
                                        document.querySelector('.play-pause-button') ||
                                        document.querySelector('[aria-label*="Play"], [aria-label*="Pause"]') ||
                                        document.querySelector('tp-yt-paper-icon-button[aria-label*="play"], tp-yt-paper-icon-button[aria-label*="pause"]');
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
