package io.github.fluxsniffermods.streaming

import android.content.Context
import android.content.Intent
import io.github.fluxsniffermods.core.ui.AppSettingsActivity
import io.github.fluxsniffermods.core.ui.Need
import io.github.fluxsniffermods.core.ui.Needs
import io.github.fluxsniffermods.core.ui.SearchEntry
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** What streaming can't do without: the shell, notifications, and a Moonlight device paired. */
internal object StreamingNeeds {
    fun all(context: Context): List<Need> = listOf(
        Needs.shell(R.string.shell_access_for_streaming) { AppSettingsActivity.openShell(it) },
        Needs.notifications(R.string.readiness_notifications_streaming),
        Need(
            R.string.readiness_paired,
            { ctx -> ctx.getString(R.string.readiness_paired_description) +
                if (!StreamHostService.enabled(ctx)) " " + ctx.getString(R.string.readiness_paired_start) else "" },
            { ctx -> runCatching { StreamHost.pairedCount(ctx) == 0 }.getOrDefault(false) },
            { ctx ->
                // Pairing starts from Moonlight: the phone answering it first
                if (!StreamHostService.enabled(ctx)) runCatching { StreamHostService.setEnabled(ctx, true) }
                android.widget.Toast.makeText(ctx, R.string.readiness_paired_description, android.widget.Toast.LENGTH_LONG).show()
            }
        )
    )

    /** The first thing still needed, as the Quick Settings tile's line (null when ready). */
    fun firstMissing(context: Context): String? =
        all(context).firstOrNull { runCatching { it.missing(context) }.getOrDefault(false) }?.let { context.getString(it.title) }
}

/** Streaming's own settings, found by the search on its page. */
internal object StreamingSearch {
    fun entries(context: Context): List<SearchEntry> {
        fun entry(title: Int, summary: Int?) = SearchEntry(title, summary) { StreamingSettingsActivity.open(context) }
        return listOf(
            entry(R.string.stream_switch, R.string.stream_switch_description),
            entry(R.string.stream_always, R.string.stream_always_description),
            entry(R.string.stream_all_apps, R.string.stream_all_apps_description),
            entry(R.string.stream_termux, R.string.stream_termux_description),
            entry(R.string.stream_phone_screen_switch, R.string.stream_phone_screen_description),
            entry(R.string.stream_pass_through, R.string.stream_pass_through_description),
            entry(R.string.stream_unpair, R.string.stream_unpair_description),
            entry(R.string.tv_off_after, R.string.tv_off_after_description),
            entry(R.string.tv_volume_keys, R.string.tv_volume_keys_description),
            entry(R.string.tv_title, null),
        )
    }
}

/** Streaming's settings page (from the Streaming app's ⚙ and the Quick Settings tile). */
class StreamingSettingsActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            io.github.fluxsniffermods.core.ui.FluxTheme {
                io.github.fluxsniffermods.streaming.settings.StreamingHomeScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun open(context: Context) {
            runCatching {
                context.startActivity(Intent(context, StreamingSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
