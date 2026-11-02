package io.github.fluxsniffermods.streaming

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.fluxsniffermods.core.FluxCore

class StreamingApp : Application() {
    private companion object {
        /** Streaming's settings as Flux Titan Hub kept them: pairings, the TV, what Moonlight lists. */
        val MIGRATED = listOf("stream_", "streaming_", "tv_", "airplay_", "moonlight_", "quick_launcher_termux_scripts")
        /** The phone's identity to Moonlight and the devices paired with it, so none pair again. */
        val MIGRATED_FILES = listOf("streaming/host.key", "streaming/host.crt", "streaming/clients.pem")
    }

    override fun onCreate() {
        super.onCreate()
        FluxCore.init(
            this,
            BuildConfig.VERSION_NAME,
            about = FluxCore.About(
                credits = R.string.streaming_credits,
                crashTags = "Stream(Wake|Host|Session|Input|Audio|Control)|FluxExtraScreen|AirPlay|Miracast"
            )
        )
        val app = this
        Thread {
            // Once: the pairings and choices Flux Titan Hub kept, taken over from Flux Keyboard
            runCatching { io.github.fluxsniffermods.core.link.FluxMigration.importOnce(app, MIGRATED, MIGRATED_FILES) }
            // Moonlight's Wake listened for, streaming on again if it was, and a stream that
            // ended with the app (a crash) put right
            StreamWake.start(app)
            runCatching { StreamHost.recover(app) }
        }.start()
        // Recovered again once the shell answers (it may start after the app)
        io.github.fluxsniffermods.core.shell.BuiltInShell.addStartListener { runCatching { StreamHost.recover(app) } }
    }
}

/** At boot: Moonlight's Wake listened for, and streaming on again if it was on. */
class StreamBoot : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Starting the app is all it takes (StreamingApp starts the rest)
    }
}

/** Streaming to Moonlight on or off from Quick Settings; who it's streaming to, or what it still needs. */
class StreamingTileService : android.service.quicksettings.TileService() {
    override fun onStartListening() = update()

    override fun onClick() {
        runCatching { StreamHostService.setEnabled(applicationContext, !StreamHostService.enabled(this)) }
        update()
    }

    private fun update() {
        val tile = qsTile ?: return
        val on = StreamHostService.enabled(this)
        tile.state = if (on) android.service.quicksettings.Tile.STATE_ACTIVE else android.service.quicksettings.Tile.STATE_INACTIVE
        val device = StreamHost.streamingDevice.takeIf { StreamHost.session != null }
        tile.subtitle = when {
            device != null -> getString(R.string.stream_now_to, device)
            on -> StreamingNeeds.firstMissing(this)
            else -> null
        }
        tile.updateTile()
    }

    companion object {
        fun refresh(context: Context) {
            runCatching { requestListeningState(context, android.content.ComponentName(context, StreamingTileService::class.java)) }
        }
    }
}

/** The tile long-pressed: streaming's settings. */
class StreamingTileSettingsActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        StreamingSettingsActivity.open(this)
        finish()
    }
}
