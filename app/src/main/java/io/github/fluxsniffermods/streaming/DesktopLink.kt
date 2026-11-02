package io.github.fluxsniffermods.streaming

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.github.fluxsniffermods.core.FluxApp
import io.github.fluxsniffermods.core.link.FluxLink
import io.github.fluxsniffermods.core.link.LinkMethods

/** What streaming asks of Flux Desktop (when it's installed): its Linux, and opening it on the TV. */
internal object DesktopLink {
    class Info(val installed: Boolean, val label: String, val running: Boolean, val starting: Boolean)

    /** Blocking; null without Flux Desktop. */
    fun info(context: Context): Info? = FluxLink.call(context, FluxApp.DESKTOP, LinkMethods.DESKTOP_INFO)?.let {
        Info(it.getBoolean("installed"), it.getString("label").orEmpty(), it.getBoolean("running"), it.getBoolean("starting"))
    }

    /** The desktop on [screen] (a TV's), or the phone's: started first if it isn't running. */
    fun open(context: Context, screen: Int?) {
        runCatching {
            context.startActivity(
                Intent().setComponent(ComponentName(FluxApp.DESKTOP.packageName, LinkMethods.DESKTOP_OPEN_ACTIVITY))
                    .putExtra(LinkMethods.EXTRA_SCREEN, screen ?: -1)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
