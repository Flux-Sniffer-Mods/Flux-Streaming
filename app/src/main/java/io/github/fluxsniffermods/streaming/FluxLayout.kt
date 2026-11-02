package io.github.fluxsniffermods.streaming

import android.view.KeyEvent

/**
 * Flux Gaming's layout (its FluxLayout): the key each controller control is sent as, so a
 * controller in Moonlight plays a game given that layout as the keyboard would.
 */
internal object FluxLayout {
    enum class Control {
        UP, DOWN, LEFT, RIGHT, A, B, X, Y, L1, R1, L2, R2, SELECT, START, HOME, L3, R3,
        LS_UP, LS_DOWN, LS_LEFT, LS_RIGHT, RS_UP, RS_DOWN, RS_LEFT, RS_RIGHT,
        /** The triggers pressed all the way without their click (a GameCube controller's). */
        L2_ANALOG, R2_ANALOG
    }

    /**
     * The key each control is sent as (and so written into emulators as): the keys you press for
     * it where they're its own, keys of their own for the rest.
     */
    val KEYS: List<Pair<Int, Control>> = listOf(
        KeyEvent.KEYCODE_W to Control.LS_UP, KeyEvent.KEYCODE_Z to Control.LS_DOWN,
        KeyEvent.KEYCODE_A to Control.LS_LEFT, KeyEvent.KEYCODE_D to Control.LS_RIGHT,
        KeyEvent.KEYCODE_S to Control.L3,
        KeyEvent.KEYCODE_I to Control.Y, KeyEvent.KEYCODE_J to Control.X,
        KeyEvent.KEYCODE_M to Control.A, KeyEvent.KEYCODE_L to Control.B,
        KeyEvent.KEYCODE_K to Control.R3,
        KeyEvent.KEYCODE_Q to Control.L1, KeyEvent.KEYCODE_E to Control.R1,
        KeyEvent.KEYCODE_U to Control.L2, KeyEvent.KEYCODE_O to Control.R2,
        KeyEvent.KEYCODE_SPACE to Control.START, KeyEvent.KEYCODE_ENTER to Control.SELECT,
        KeyEvent.KEYCODE_DEL to Control.HOME,
        KeyEvent.KEYCODE_DPAD_UP to Control.UP, KeyEvent.KEYCODE_DPAD_DOWN to Control.DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT to Control.LEFT, KeyEvent.KEYCODE_DPAD_RIGHT to Control.RIGHT,
        KeyEvent.KEYCODE_T to Control.RS_UP, KeyEvent.KEYCODE_V to Control.RS_DOWN,
        KeyEvent.KEYCODE_F to Control.RS_LEFT, KeyEvent.KEYCODE_G to Control.RS_RIGHT,
        KeyEvent.KEYCODE_C to Control.L2_ANALOG, KeyEvent.KEYCODE_N to Control.R2_ANALOG
    )

    /** The key a control is sent as. */
    fun keyOf(control: Control): Int = KEYS.first { it.second == control }.first
}
