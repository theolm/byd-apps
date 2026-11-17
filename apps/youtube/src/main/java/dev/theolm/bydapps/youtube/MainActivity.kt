package dev.theolm.bydapps.youtube

import dev.theolm.bydapps.core.GeckoWebAppActivity

class MainActivity : GeckoWebAppActivity() {
    override val startUrl = "https://m.youtube.com"
    override val partnerPackages = listOf("dev.theolm.bydapps.ytmusic")
}
