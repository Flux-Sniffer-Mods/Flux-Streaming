package io.github.fluxsniffermods.streaming

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import io.github.fluxsniffermods.core.FluxApp
import io.github.fluxsniffermods.core.display.ExtraScreen
import io.github.fluxsniffermods.core.link.FluxLink
import io.github.fluxsniffermods.core.link.LinkMethods

/**
 * What streaming asks of Flux Gaming (when it's installed): the games with their art and what
 * starts each, and gaming mode, told where the game is while it streams.
 */
internal object GamingLink {
    /** A game in Flux Gaming's list: its own [id], [name], apps, what starts it, its art's version and streaming size. */
    data class Game(val id: String, val name: String, val packages: Set<String>, val launch: Intent?, val own: String?, val artVersion: Long, val streamHeight: Int?)

    class Games(val games: List<Game>, val xboxLaunch: Intent?, val xboxArtVersion: Long, val gamesApp: Intent?)

    @Volatile private var cached: Pair<Long, Games>? = null

    fun installed(context: Context) = FluxApp.GAMING.installed(context)

    /** The games, as the Games app lists them; asked at most every few seconds (Moonlight asks often). Blocking. */
    fun games(context: Context): Games {
        val now = android.os.SystemClock.elapsedRealtime()
        cached?.let { (at, games) -> if (now - at < CACHE_MS) return games }
        val answer = FluxLink.call(context, FluxApp.GAMING, LinkMethods.GAMES) ?: return Games(emptyList(), null, 0, null)
        fun intent(uri: String?) = uri?.let { runCatching { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) }.getOrNull() }
        val ids = answer.getStringArray("ids").orEmpty()
        val names = answer.getStringArray("names").orEmpty()
        val packages = answer.getStringArray("packages").orEmpty()
        val launches = answer.getStringArray("launches").orEmpty()
        val own = answer.getStringArray("own").orEmpty()
        val art = answer.getLongArray("art") ?: LongArray(0)
        val heights = answer.getIntArray("streamHeights") ?: IntArray(0)
        val games = ids.indices.map { i ->
            Game(ids[i], names.getOrElse(i) { "" }, packages.getOrElse(i) { "" }.split(',').filter { it.isNotEmpty() }.toSet(),
                intent(launches.getOrNull(i)), own.getOrNull(i)?.takeIf { it.isNotEmpty() }, art.getOrElse(i) { 0L },
                heights.getOrElse(i) { 0 }.takeIf { it > 0 })
        }
        return Games(games, intent(answer.getString("xbox_launch")), answer.getLong("xbox_art"), intent(answer.getString("games_app")))
            .also { cached = now to it }
    }

    /** A game's art from Flux Gaming ([kind] "cover" or "portrait"; [id] the Xbox tile's for its tile). Blocking. */
    fun art(context: Context, id: String, kind: String): Bitmap? = runCatching {
        context.contentResolver.openInputStream(FluxLink.uri(FluxApp.GAMING, LinkMethods.gameArtPath(id, kind)))
            ?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

    /** The Xbox Cloud Gaming tile's id, for its art. */
    const val XBOX_TILE_ID = "xbox-cloud-tile"

    /** The stream mirrors the phone at [size], or (null) no longer: gaming mode turns the screen to suit. */
    fun streaming(context: Context, size: Pair<Int, Int>?) {
        FluxLink.call(context, FluxApp.GAMING, LinkMethods.GAME_STREAMING, null, size?.let { (w, h) -> Bundle().apply { putInt("width", w); putInt("height", h) } })
    }

    /** The game on [screen], a screen of its own (null: back on the phone). */
    fun extraScreen(context: Context, screen: ExtraScreen.Screen?) {
        FluxLink.call(context, FluxApp.GAMING, LinkMethods.GAME_EXTRA_SCREEN, null, screen?.let {
            Bundle().apply { putInt("id", it.id); putInt("width", it.width); putInt("height", it.height); putBoolean("desktop", it.desktop) }
        })
    }

    fun front(context: Context, packageName: String) {
        FluxLink.call(context, FluxApp.GAMING, LinkMethods.GAME_FRONT, packageName)
    }

    fun recover(context: Context) {
        FluxLink.call(context, FluxApp.GAMING, LinkMethods.GAME_RECOVER)
    }

    /** Waits for gaming mode's screen changes to finish (a few seconds at most). Blocking. */
    fun settle(context: Context) {
        if (installed(context)) FluxLink.call(context, FluxApp.GAMING, LinkMethods.GAME_SETTLE)
    }

    /** Gaming mode's "place keys on the screen", for the streaming notification (null without Flux Gaming). */
    fun placeKeys(context: Context): PendingIntent? = runCatching {
        @Suppress("DEPRECATION")
        FluxLink.call(context, FluxApp.GAMING, LinkMethods.GAME_PLACE_KEYS)?.getParcelable<PendingIntent>("intent")
    }.getOrNull()

    /** The game gaming mode is playing, and whether it takes keys rather than a controller; read at most once a second. */
    class State(val playing: String?, val sendsKeys: Boolean)

    @Volatile private var state: Pair<Long, State>? = null

    fun state(context: Context): State {
        val now = android.os.SystemClock.elapsedRealtime()
        state?.let { (at, s) -> if (now - at < STATE_MS) return s }
        val answer = FluxLink.call(context, FluxApp.GAMING, LinkMethods.GAME_STATE)
        return State(answer?.getString("playing"), answer?.getBoolean("sendsKeys") == true).also { state = now to it }
    }

    private const val CACHE_MS = 5_000L
    private const val STATE_MS = 1_000L
}
