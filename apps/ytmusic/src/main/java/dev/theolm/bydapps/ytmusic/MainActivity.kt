package dev.theolm.bydapps.ytmusic

import dev.theolm.bydapps.core.GeckoWebAppActivity
import org.mozilla.geckoview.GeckoSessionSettings

class MainActivity : GeckoWebAppActivity() {
    override val startUrl = "https://music.youtube.com"

    // The manifest asks for minimal-ui, so report that instead of browser mode.
    // The app already draws no browser chrome; this only changes display-mode
    // queries and lets the site opt into its installed-app layout.
    override val displayMode = GeckoSessionSettings.DISPLAY_MODE_MINIMAL_UI

    // The mobile layout turns the player into a fullscreen page. The desktop
    // layout keeps the queue and transport controls visible on the wide car
    // screen.
    override val userAgentMode = GeckoSessionSettings.USER_AGENT_MODE_DESKTOP

    override val contentScale = 1.2f
    override val partnerPackages = listOf("dev.theolm.bydapps.youtube")
}
