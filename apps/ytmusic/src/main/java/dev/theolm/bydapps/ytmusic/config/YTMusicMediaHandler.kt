package dev.theolm.bydapps.ytmusic.config

import android.webkit.WebView
import dev.theolm.core.MediaButtonHandler

/**
 * Maps steering-wheel media controls to the YouTube Music player bar.
 *
 * The player bar controls are not stable DOM ids for next/previous, but they
 * do have accessible labels ("Next", "Previous", "Play", "Pause"), which are
 * more reliable than the old data-test-id selectors.
 */
class YTMusicMediaHandler : MediaButtonHandler {

    override fun onMediaNext(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function() {
                var bar = document.querySelector('ytmusic-player-bar');
                if (!bar) return;
                var button = bar.querySelector('#next-button') ||
                             bar.querySelector('[aria-label="Next"]') ||
                             bar.querySelector('[aria-label*="ext"]');
                if (button) button.click();
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onMediaPrevious(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function() {
                var bar = document.querySelector('ytmusic-player-bar');
                if (!bar) return;
                var button = bar.querySelector('#previous-button') ||
                             bar.querySelector('[aria-label="Previous"]') ||
                             bar.querySelector('[aria-label*="revious"]');
                if (button) button.click();
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onMediaPlayPause(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function() {
                var bar = document.querySelector('ytmusic-player-bar');
                if (!bar) return;
                var button = bar.querySelector('[aria-label="Play"]') ||
                             bar.querySelector('[aria-label="Pause"]') ||
                             bar.querySelector('#play-pause-button button') ||
                             bar.querySelector('#play-pause-button');
                if (button) button.click();
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onMediaPlay(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function() {
                var bar = document.querySelector('ytmusic-player-bar');
                if (!bar) return;
                var button = bar.querySelector('[aria-label="Play"]') ||
                             bar.querySelector('#play-pause-button button') ||
                             bar.querySelector('#play-pause-button');
                if (button && ((button.getAttribute('aria-label') || button.getAttribute('title') || '').toLowerCase().indexOf('play') >= 0)) {
                    button.click();
                }
            })();
            """.trimIndent(),
            null
        )
    }

    override fun onMediaPause(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function() {
                var bar = document.querySelector('ytmusic-player-bar');
                if (!bar) return;
                var button = bar.querySelector('[aria-label="Pause"]') ||
                             bar.querySelector('#play-pause-button button') ||
                             bar.querySelector('#play-pause-button');
                if (button && ((button.getAttribute('aria-label') || button.getAttribute('title') || '').toLowerCase().indexOf('pause') >= 0)) {
                    button.click();
                }
            })();
            """.trimIndent(),
            null
        )
    }
}
