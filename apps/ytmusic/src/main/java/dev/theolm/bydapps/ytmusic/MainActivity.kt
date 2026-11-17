package dev.theolm.bydapps.ytmusic

import dev.theolm.bydapps.core.GeckoWebAppActivity

class MainActivity : GeckoWebAppActivity() {
    override val startUrl = "https://music.youtube.com"
    override val contentScale = 1.2f
    override val partnerPackages = listOf("dev.theolm.bydapps.youtube")
}
