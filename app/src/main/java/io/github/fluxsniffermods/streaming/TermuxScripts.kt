package io.github.fluxsniffermods.streaming

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.github.fluxsniffermods.core.FluxPrefs

/**
 * Termux:Widget's scripts (~/.shortcuts, and ~/.shortcuts/tasks for background tasks) in the
 * quick launcher, found by asking Termux itself: Flux Keyboard can't read Termux's files. The
 * list is kept and asked for again when the quick launcher opens (at most once a minute); it
 * needs Termux's allow-external-apps and the permission to run commands in Termux.
 */
object TermuxScripts {
    // Termux's run-command service (a script in ~/.shortcuts, as Termux:Widget runs it)
    const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val TERMUX_RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
    private const val TERMUX_HOME = "/data/data/com.termux/files/home"

    /** Runs [path] in Termux: in the background, or in a terminal session. */
    private fun termuxCommand(path: String, background: Boolean, arguments: Array<String>? = null): Intent =
        Intent("com.termux.RUN_COMMAND").setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
            .putExtra("com.termux.RUN_COMMAND_PATH", path)
            .putExtra("com.termux.RUN_COMMAND_BACKGROUND", background)
            .apply { arguments?.let { putExtra("com.termux.RUN_COMMAND_ARGUMENTS", it) } }

    private const val KEY_ENABLED = "quick_launcher_termux_scripts"
    private const val KEY_CACHE = "quick_launcher_termux_scripts_found"
    private const val REFRESH_EVERY_MS = 60_000L
    @Volatile private var lastRefresh = 0L

    fun enabled(context: Context): Boolean = FluxPrefs.get(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        FluxPrefs.get(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun available(context: Context): Boolean =
        runCatching { context.packageManager.getApplicationInfo(TERMUX_PACKAGE, 0) }.isSuccess &&
            ContextCompat.checkSelfPermission(context, TERMUX_RUN_COMMAND_PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** The scripts Termux last listed, relative to ~/.shortcuts ("backup.sh", "tasks/sync.sh"). */
    fun found(context: Context): List<String> =
        FluxPrefs.get(context).getString(KEY_CACHE, null)
            ?.lines()?.filter { it.isNotBlank() }.orEmpty()

    /** Keeps what Termux listed (its find's output). */
    fun store(context: Context, listing: String) {
        val scripts = parse(listing)
        FluxPrefs.get(context).edit().putString(KEY_CACHE, scripts.joinToString("\n")).apply()
    }

    fun parse(listing: String): List<String> =
        listing.lines().map { it.trim().removePrefix("./") }
            .filter { it.isNotEmpty() && !it.startsWith("icons/") && it.split('/').none { part -> part.startsWith(".") } }
            .distinct().sorted()

    /** The script's path in Termux and whether it runs in the background (Termux:Widget's tasks). */
    fun command(script: String): Intent =
        termuxCommand("${TERMUX_HOME}/.shortcuts/$script", background = script.startsWith("tasks/"))

    /** "Termux · background" or "Termux · terminal": how the script runs, as Termux:Widget runs it. */
    fun kind(context: Context, background: Boolean): String =
        context.getString(if (background) R.string.termux_runs_background else R.string.termux_runs_terminal)

    /** Whether a Termux command (as an intent URI) runs in the background; null when it isn't one. */
    fun runsInBackground(intentUri: String): Boolean? {
        val intent = runCatching { Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME) }.getOrNull() ?: return null
        if (intent.action != "com.termux.RUN_COMMAND") return null
        return intent.getBooleanExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
    }

    fun label(script: String): String = script.substringAfterLast('/').substringBeforeLast('.').ifBlank { script }

    /** Asks Termux for the scripts; the answer comes to [Receiver], or to [reply] when given. */
    fun listCommand(reply: PendingIntent): Intent =
        termuxCommand(
            "/data/data/com.termux/files/usr/bin/sh", background = true,
            arguments = arrayOf("-c", "cd ~/.shortcuts 2>/dev/null && find . -type f")
        ).putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", reply)

    /**
     * Asks Termux for the scripts again, when on (in the quick launcher, or [forStreaming] for
     * Moonlight's list), allowed and not asked in the last minute.
     */
    fun refresh(context: Context, forStreaming: Boolean = false) {
        if (!(enabled(context) || forStreaming) || !available(context)) return
        val now = System.currentTimeMillis()
        if (now - lastRefresh < REFRESH_EVERY_MS) return
        lastRefresh = now
        val app = context.applicationContext
        val reply = PendingIntent.getBroadcast(
            app, 1, Intent(app, Receiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        runCatching { app.startForegroundService(listCommand(reply)) }
    }

    /** Termux's answer: the scripts found (a failed listing leaves the kept list as it was). */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val result = intent.getBundleExtra("result") ?: return
            if (!result.getString("errmsg").isNullOrBlank()) return
            store(context, result.getString("stdout").orEmpty())
        }
    }
}
