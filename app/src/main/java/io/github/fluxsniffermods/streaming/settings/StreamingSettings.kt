package io.github.fluxsniffermods.streaming.settings

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhoneAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Info
import io.github.fluxsniffermods.core.ui.*
import io.github.fluxsniffermods.streaming.R
import io.github.fluxsniffermods.streaming.StreamingNeeds
import io.github.fluxsniffermods.streaming.StreamingSearch

/**
 * Streaming's settings, the same from its Quick Settings tile and the Streaming app's ⚙: what it
 * still needs, a search of its own settings, then its settings by group, and Flux Streaming itself.
 */
@Composable
internal fun StreamingHomeScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    FluxScreenScaffold(stringResource(R.string.app_name), onBack, modifier) {
        ReadinessCard(remember { StreamingNeeds.all(context) })
        SettingsSearchArea(
            entries = remember { StreamingSearch.entries(context) },
            fieldModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            hint = stringResource(R.string.streaming_search)
        ) {
            StreamingSettings()
            // Flux Streaming itself: updates, shell access, the crash log, the other Flux apps
            SettingsCategoryRow(
                icon = androidx.compose.material.icons.Icons.Filled.Info,
                title = stringResource(io.github.fluxsniffermods.core.R.string.flux_app_page, stringResource(R.string.app_name)),
                description = stringResource(io.github.fluxsniffermods.core.R.string.flux_app_page_description),
                onClick = { AppSettingsActivity.open(context) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Streaming to Moonlight's settings, grouped: the connection (on, always available, pairings),
 * what Moonlight lists, the picture and sound, and the TV. On streaming's one settings page,
 * opened from its tile and the Streaming app's ⚙.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun StreamingSettings(onStreamingChanged: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val host = io.github.fluxsniffermods.streaming.StreamHostService
    var streaming by remember { mutableStateOf(host.enabled(context)) }
    var paired by remember { mutableIntStateOf(io.github.fluxsniffermods.streaming.StreamHost.pairedCount(context)) }
    // Read again on coming back: the Streaming app or a Quick Settings tile may switch it
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                streaming = host.enabled(context)
                paired = io.github.fluxsniffermods.streaming.StreamHost.pairedCount(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // The connection: the phone answering Moonlight, and the devices paired with it
    SettingsSectionDivider(stringResource(R.string.stream_connection_section))
    FluxSwitchRow(
        linkId = "streaming.host",
        title = stringResource(R.string.stream_switch),
        description = stringResource(R.string.stream_switch_description),
        checked = streaming
    ) { on ->
        streaming = on
        host.setEnabled(context, on)
        onStreamingChanged(on)
    }
    // Listed in Moonlight with streaming off, for clients whose Wake can't reach the phone
    var always by remember { mutableStateOf(host.alwaysAvailable(context)) }
    FluxSwitchRow(
        linkId = "streaming.always",
        title = stringResource(R.string.stream_always),
        description = stringResource(R.string.stream_always_description),
        checked = always
    ) { on ->
        always = on
        host.setAlwaysAvailable(context, on)
    }
    if (paired > 0) FluxActionRow(
        linkId = "streaming.unpair",
        icon = "\u2715",
        title = stringResource(R.string.stream_unpair),
        description = stringResource(R.string.stream_unpair_description, paired)
    ) {
        io.github.fluxsniffermods.streaming.StreamHost.unpairAll(context)
        paired = 0
    }

    // What Moonlight lists besides the games: the phone's screen, apps, Termux's scripts
    SettingsSectionDivider(stringResource(R.string.stream_lists_section))
    var phoneScreen by remember { mutableStateOf(host.phoneScreen(context)) }
    FluxSwitchRow(
        linkId = "streaming.phone_screen",
        title = stringResource(R.string.stream_phone_screen_switch),
        description = stringResource(R.string.stream_phone_screen_description),
        checked = phoneScreen
    ) { on ->
        phoneScreen = on
        host.setPhoneScreen(context, on)
    }
    // Every app, or the ones long-pressed in the Streaming app
    var allApps by remember { mutableStateOf(io.github.fluxsniffermods.streaming.StreamApps.allApps(context)) }
    FluxSwitchRow(
        linkId = "streaming.all_apps",
        title = stringResource(R.string.stream_all_apps),
        description = stringResource(R.string.stream_all_apps_description),
        checked = allApps
    ) { on ->
        allApps = on
        io.github.fluxsniffermods.streaming.StreamApps.setAllApps(context, on)
    }
    // Termux:Widget's scripts too, each opening Termux on the TV as it runs
    var termux by remember { mutableStateOf(io.github.fluxsniffermods.streaming.StreamApps.termux(context)) }
    FluxSwitchRow(
        linkId = "streaming.termux",
        title = stringResource(R.string.stream_termux),
        description = stringResource(R.string.stream_termux_description),
        checked = termux
    ) { on ->
        termux = on
        io.github.fluxsniffermods.streaming.StreamApps.setTermux(context, on)
        if (on) io.github.fluxsniffermods.streaming.TermuxScripts.refresh(context, forStreaming = true)
    }

    // The picture and sound: the stream's size, where the game plays, the phone's sound
    SettingsSectionDivider(stringResource(R.string.stream_picture_section))
    // The game's size for the Moonlight client: the phone's screen takes its shape while streaming
    val resolutions = host.RESOLUTIONS
    val resolutionNames = listOf(stringResource(R.string.stream_resolution_client), "720p", "1080p", "1440p")
    var resolution by remember { mutableStateOf(host.resolution(context)) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ChoiceRow(stringResource(R.string.stream_resolution), resolutionNames[resolutions.indexOf(resolution).coerceAtLeast(0)], resolutionNames) { index ->
            resolution = resolutions[index]
            host.setResolution(context, resolution)
        }
        Text(stringResource(R.string.stream_resolution_description), style = MaterialTheme.typography.bodySmall)
    }
    // Where the game plays while it streams: the phone's screen mirrored, or one of its own
    val modes = host.SCREEN_MODES
    val modeNames = listOf(
        stringResource(R.string.stream_screen_mirror),
        stringResource(R.string.stream_screen_separate),
        stringResource(R.string.stream_screen_desktop)
    )
    var mode by remember { mutableStateOf(host.screenMode(context)) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ChoiceRow(stringResource(R.string.stream_screen), modeNames[modes.indexOf(mode).coerceAtLeast(0)], modeNames) { index ->
            mode = modes[index]
            host.setScreenMode(context, mode)
        }
        Text(stringResource(R.string.stream_screen_description), style = MaterialTheme.typography.bodySmall)
    }
    // Keyboards, mice and controllers plugged into the phone, for the game's own screen
    if (mode != host.SCREEN_MIRROR) {
        var passThrough by remember { mutableStateOf(host.passThrough(context)) }
        FluxSwitchRow(
            linkId = "streaming.pass_through",
            title = stringResource(R.string.stream_pass_through),
            description = stringResource(R.string.stream_pass_through_description),
            checked = passThrough
        ) { on ->
            passThrough = on
            host.setPassThrough(context, on)
        }
    }
    // The phone quiet while the TV plays: the game's sound, or every sound of the phone's
    val quietModes = io.github.fluxsniffermods.streaming.StreamQuiet.Mode.entries
    val quietNames = listOf(
        stringResource(R.string.stream_quiet_off),
        stringResource(R.string.stream_quiet_game),
        stringResource(R.string.stream_quiet_all)
    )
    var quiet by remember { mutableStateOf(io.github.fluxsniffermods.streaming.StreamQuiet.mode(context)) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ChoiceRow(stringResource(R.string.stream_quiet), quietNames[quietModes.indexOf(quiet)], quietNames) { index ->
            quiet = quietModes[index]
            io.github.fluxsniffermods.streaming.StreamQuiet.setMode(context, quiet)
        }
        Text(stringResource(R.string.stream_quiet_description), style = MaterialTheme.typography.bodySmall)
    }

    // A TV the phone turns on and opens Moonlight on, straight into a game picked on the phone
    SettingsSectionDivider(stringResource(R.string.stream_tv_section))
    TvRows()
}

/**
 * The TV games picked on the phone start on: found on the Wi-Fi (LG's webOS TVs), set up once
 * (the TV asks), then woken and opened at Moonlight straight into the game.
 */
@Composable
private fun TvRows() {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var tv by remember { mutableStateOf(io.github.fluxsniffermods.streaming.TvRemote.tv(context)) }
    var ready by remember { mutableStateOf(io.github.fluxsniffermods.streaming.TvRemote.ready(context)) }
    var finding by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<List<io.github.fluxsniffermods.streaming.TvRemote.Tv>?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    fun find() {
        finding = true; found = null; status = context.getString(R.string.tv_finding)
        scope.launch {
            found = withContext(Dispatchers.IO) { io.github.fluxsniffermods.streaming.TvRemote.discover(context) }
            status = if (found.isNullOrEmpty()) context.getString(R.string.tv_none_found) else null
        }
    }

    fun setUp(picked: io.github.fluxsniffermods.streaming.TvRemote.Tv) {
        found = null
        status = context.getString(R.string.tv_connecting, picked.name)
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                io.github.fluxsniffermods.streaming.TvRemote.pair(context, picked) {
                    scope.launch { status = context.getString(R.string.tv_accept, picked.name) }
                }
            }
            tv = io.github.fluxsniffermods.streaming.TvRemote.tv(context)
            ready = io.github.fluxsniffermods.streaming.TvRemote.ready(context)
            status = context.getString(if (ok) R.string.tv_set_up else R.string.tv_setup_failed, picked.name)
            finding = false
        }
    }

    FluxActionRow(
        linkId = "streaming.tv",
        icon = "\u25AD",
        title = stringResource(R.string.tv_title),
        description = status ?: if (ready && tv != null) stringResource(R.string.tv_description_set, tv!!.name) else stringResource(R.string.tv_description_none)
    ) { if (ready) menu = true else find() }
    if (ready) {
        // A game opened while streaming: asked each time, or always on the TV or the phone
        var games by remember { mutableStateOf(io.github.fluxsniffermods.streaming.TvRemote.games(context)) }
        val choices = io.github.fluxsniffermods.streaming.TvRemote.Games.entries
        val labels = listOf(stringResource(R.string.tv_games_ask), stringResource(R.string.tv_play_on_tv), stringResource(R.string.tv_play_on_phone))
        ChoiceRow(stringResource(R.string.tv_games_on_tv), labels[choices.indexOf(games)], labels) { index ->
            games = choices[index]
            io.github.fluxsniffermods.streaming.TvRemote.setGames(context, choices[index])
        }
        Text(stringResource(R.string.tv_games_on_tv_description), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        // The TV turned off when a stream to it ends
        var offAfter by remember { mutableStateOf(io.github.fluxsniffermods.streaming.TvRemote.offAfter(context)) }
        FluxSwitchRow(
            linkId = "streaming.tv_off",
            title = stringResource(R.string.tv_off_after),
            description = stringResource(R.string.tv_off_after_description),
            checked = offAfter
        ) { on ->
            offAfter = on
            io.github.fluxsniffermods.streaming.TvRemote.setOffAfter(context, on)
        }
        // The phone's volume keys turning the TV up and down while streaming to it
        var volumeKeys by remember { mutableStateOf(io.github.fluxsniffermods.streaming.TvRemote.volumeKeys(context)) }
        FluxSwitchRow(
            linkId = "streaming.tv_volume",
            title = stringResource(R.string.tv_volume_keys),
            description = stringResource(R.string.tv_volume_keys_description),
            checked = volumeKeys
        ) { on ->
            volumeKeys = on
            io.github.fluxsniffermods.streaming.TvRemote.setVolumeKeys(context, on)
        }
    }
    if (menu) AlertDialog(
        onDismissRequest = { menu = false },
        title = { Text(tv?.name.orEmpty()) },
        text = { Text(stringResource(R.string.tv_menu_description)) },
        confirmButton = { TextButton(onClick = { menu = false; find() }) { Text(stringResource(R.string.tv_find_again)) } },
        dismissButton = {
            TextButton(onClick = {
                menu = false
                io.github.fluxsniffermods.streaming.TvRemote.forget(context)
                tv = null; ready = false; status = null
            }) { Text(stringResource(R.string.tv_forget)) }
        }
    )
    found?.takeIf { it.isNotEmpty() }?.let { list ->
        AlertDialog(
            onDismissRequest = { found = null; finding = false; status = null },
            title = { Text(stringResource(R.string.tv_pick)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    list.forEach { each ->
                        Text(
                            "${each.name}  (${each.host})",
                            modifier = Modifier.fillMaxWidth().clickable { setUp(each) }.padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { found = null; finding = false; status = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** A setting with a few choices: its title, the one chosen, and the others in a menu. */
@Composable
private fun ChoiceRow(title: String, value: String, options: List<String>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.primary)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onPick(index) })
            }
        }
    }
}
