package io.github.fluxsniffermods.streaming

import android.content.Context
import android.media.AudioManager
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper

/**
 * The phone's volume keys as the TV's while a stream plays there, as a cast app's are: a media
 * session whose volume is the TV's. In a game, the keys stay the phone's (gaming mode has SYM with
 * them for the TV).
 */
internal object TvVolume {
    private val main = Handler(Looper.getMainLooper())
    private var session: MediaSession? = null

    /** Started or stopped as streaming to the TV starts and ends (or its setting changes). */
    fun update(context: Context) {
        val app = context.applicationContext
        val wanted = runCatching { TvRemote.volumeHere(app) }.getOrDefault(false)
        main.post { if (wanted) start(app) else stop() }
    }

    private fun start(app: Context) {
        if (session != null) return
        val provider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
            override fun onAdjustVolume(direction: Int) {
                if (direction == 0) return
                Thread {
                    if (GamingLink.state(app).playing != null) {
                        app.getSystemService(AudioManager::class.java)?.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            if (direction > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    } else TvRemote.volume(app, direction > 0)
                }.start()
            }
        }
        session = runCatching {
            MediaSession(app, "FluxTvVolume").apply {
                setPlaybackToRemote(provider)
                setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1f).build())
                isActive = true
            }
        }.getOrNull()
    }

    private fun stop() {
        session?.let { runCatching { it.isActive = false; it.release() } }
        session = null
    }
}
