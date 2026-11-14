package dev.theolm.bydapps.youtube.config

import android.webkit.WebView
import dev.theolm.core.MediaButtonHandler

/**
 * Maps steering-wheel media controls to the YouTube web player.
 *
 * Both the desktop and mobile pages expose the player as `#movie_player` with
 * `nextVideo()`, `previousVideo()`, `playVideo()` and `pauseVideo()`. The old
 * `.ytp-*` buttons only exist on the desktop page, which is why clicking them
 * silently did nothing on the mobile layout.
 *
 * Keep this JavaScript ES5-friendly: the head unit's WebView can be as old as
 * Chrome 74, so optional chaining and similar syntax must be avoided.
 */
class YouTubeMediaHandler : MediaButtonHandler {

    override fun onMediaNext(webView: WebView) {
        webView.evaluateJavascript(
            """
            (function() {
                var player = document.getElementById('movie_player');
                if (player && typeof player.nextVideo === 'function') {
                    player.nextVideo();
                    return;
                }
                var button = document.querySelector('.ytp-next-button') ||
                             document.querySelector('button[aria-label*="Next"]') ||
                             document.querySelector('a.ytp-next-button');
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
                var player = document.getElementById('movie_player');
                if (player && typeof player.previousVideo === 'function') {
                    player.previousVideo();
                    return;
                }
                var button = document.querySelector('.ytp-prev-button') ||
                             document.querySelector('button[aria-label*="Previous"]');
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
                var video = document.querySelector('video');
                if (video) {
                    if (video.paused) video.play(); else video.pause();
                    return;
                }
                var player = document.getElementById('movie_player');
                if (player && typeof player.getPlayerState === 'function') {
                    if (player.getPlayerState() === 1) player.pauseVideo();
                    else player.playVideo();
                    return;
                }
                var button = document.querySelector('.ytp-play-button');
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
                var video = document.querySelector('video');
                if (video) {
                    video.play();
                    return;
                }
                var player = document.getElementById('movie_player');
                if (player && typeof player.playVideo === 'function') {
                    player.playVideo();
                    return;
                }
                var button = document.querySelector('.ytp-play-button');
                if (button && (button.getAttribute('aria-label') || '').toLowerCase().indexOf('play') >= 0) {
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
                var video = document.querySelector('video');
                if (video) {
                    video.pause();
                    return;
                }
                var player = document.getElementById('movie_player');
                if (player && typeof player.pauseVideo === 'function') {
                    player.pauseVideo();
                    return;
                }
                var button = document.querySelector('.ytp-play-button');
                if (button && (button.getAttribute('aria-label') || '').toLowerCase().indexOf('pause') >= 0) {
                    button.click();
                }
            })();
            """.trimIndent(),
            null
        )
    }
}
