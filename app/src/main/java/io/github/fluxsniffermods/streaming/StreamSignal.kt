package io.github.fluxsniffermods.streaming

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Streaming's mark: a broadcast point with Wi-Fi's arcs, as many lit as the phone's Wi-Fi is
 * strong while streaming is on; dimmed while it's off (or the phone isn't on Wi-Fi).
 */
@Composable
internal fun StreamSignal(on: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var level by remember { mutableStateOf(wifiLevel(context)) }
    LaunchedEffect(on) {
        while (true) {
            level = wifiLevel(context)
            kotlinx.coroutines.delay(3_000)
        }
    }
    val lit = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val dim = lit.copy(alpha = 0.25f)
    androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 26.dp, height = 22.dp)) {
        val centre = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.92f)
        val stroke = size.height * 0.11f
        // The point streaming goes out from
        drawCircle(if (on || level < 0) lit else dim, radius = stroke * 0.9f, center = centre)
        // Three arcs, lit up to the signal's strength (all of them dim while streaming is off)
        for (arc in 1..3) {
            val radius = size.height * (0.28f + 0.26f * (arc - 1))
            val shown = on && level >= arc
            drawArc(
                color = if (shown) lit else dim,
                startAngle = 225f, sweepAngle = 90f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(centre.x - radius, centre.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}

/** The phone's Wi-Fi strength, 0 (none) to 3 arcs; 0 when it isn't on Wi-Fi. */
private fun wifiLevel(context: Context): Int = runCatching {
    val connectivity = context.getSystemService(android.net.ConnectivityManager::class.java)
    val caps = connectivity?.getNetworkCapabilities(connectivity.activeNetwork) ?: return 0
    if (!caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) &&
        !caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)) return 0
    // Ethernet (a USB adapter) is as good as it gets
    if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)) return 3
    val rssi = caps.signalStrength
    when {
        rssi == android.net.NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED -> 2
        rssi >= -60 -> 3
        rssi >= -72 -> 2
        else -> 1
    }
}.getOrDefault(0)
