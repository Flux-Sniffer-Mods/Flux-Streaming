package io.github.fluxsniffermods.streaming

import android.os.Bundle
import io.github.fluxsniffermods.core.link.FluxLink
import io.github.fluxsniffermods.core.link.FluxLinkProvider
import io.github.fluxsniffermods.core.link.LinkMethods

/**
 * What the other Flux apps ask of streaming: Flux Gaming, the stream and the TV a game opens on;
 * Flux Desktop, the screen of its own a stream has open.
 */
class StreamingLink : FluxLinkProvider() {
    override fun answer(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        return when (method) {
            LinkMethods.STREAM_SCREEN -> Bundle().apply { io.github.fluxsniffermods.core.display.ExtraScreen.current?.let { putInt("id", it.id) } }
            LinkMethods.STREAM_STATE -> Bundle().apply {
                val session = StreamHost.session
                putBoolean("enabled", StreamHostService.enabled(context))
                putBoolean("session", session != null)
                putBoolean("separate", session?.separate == true)
                putBoolean("desktop", session?.isDesktop == true)
                putString("game", session?.gamePackage)
            }
            LinkMethods.STREAM_ENABLE -> { StreamHostService.setEnabled(context, arg == "1"); FluxLink.ok() }
            LinkMethods.TV_PROMPT -> Bundle().apply {
                putString("mode", TvPrompt.mode(context))
                putString("tv", TvRemote.tv(context)?.name.orEmpty())
            }
            LinkMethods.TV_PLAY -> { if (arg != null) TvPrompt.playGame(context, arg, extras?.getString("name").orEmpty()); FluxLink.ok() }
            LinkMethods.TV_VOLUME_HERE -> FluxLink.ok(TvRemote.volumeHere(context))
            LinkMethods.TV_VOLUME -> { TvRemote.volume(context, arg == "up"); FluxLink.ok() }
            else -> null
        }
    }
}
