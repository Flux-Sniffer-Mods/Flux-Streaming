package io.github.fluxsniffermods.streaming

import android.content.Context

import android.app.Activity
import android.widget.Toast

/**
 * A game opened while streaming, with a Moonlight client set up: streamed to it (turned on, Moonlight opened
 * there straight into it) or on the phone, asked each time unless set to one.
 */
object TvPrompt {
    /** Whether opening a game now would go to the Moonlight client or be asked about. */
    fun applies(context: Context): Boolean =
        StreamHostService.enabled(context) && TvRemote.ready(context) &&
            StreamHost.session == null && TvRemote.games(context) != TvRemote.Games.PHONE

    /** For Flux Gaming, as a game opens: "phone" (nothing to ask), "tv" (to the TV, as set) or "ask". */
    fun mode(context: Context): String = when {
        !applies(context) -> "phone"
        TvRemote.games(context) == TvRemote.Games.TV -> "tv"
        else -> "ask"
    }

    /** Flux Gaming's game [id] (named [name]) played on the TV. */
    fun playGame(context: Context, id: String, name: String) {
        val appId = StreamHost.games().firstOrNull { it.second.id == id }?.first ?: return
        play(context, appId, name)
    }

    /** Moonlight's entry [appId] (named [name]) on the Moonlight client, as a game is. */
    fun play(context: Context, appId: Int, name: String) {
        val app = context.applicationContext
        val tvName = TvRemote.tv(app)?.name.orEmpty()
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        fun say(text: String) = main.post { Toast.makeText(app, text, Toast.LENGTH_LONG).show() }
        say(app.getString(R.string.tv_starting, name, tvName))
        Thread {
            val result = runCatching { TvRemote.play(app, appId, name) { step -> say(step) } }.getOrDefault(TvRemote.Result.FAILED)
            when (result) {
                TvRemote.Result.STARTED -> Unit
                TvRemote.Result.NO_TV -> say(app.getString(R.string.tv_no_answer, tvName))
                TvRemote.Result.REFUSED -> say(app.getString(R.string.tv_refused, tvName))
                else -> say(app.getString(R.string.tv_failed, tvName))
            }
        }.start()
    }
}
