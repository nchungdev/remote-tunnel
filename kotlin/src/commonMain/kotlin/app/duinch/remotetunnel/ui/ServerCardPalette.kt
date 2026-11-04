package app.duinch.remotetunnel.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * One hue per state, used only by the card's header band and accents. The body stays neutral:
 * a coloured surface under coloured tiles fights itself.
 */
data class ServerCardPalette(
    val card: Color,
    val border: Color,
    val content: Color,
    val accent: Color
)

/** The body rises over the tinted card, so the header colour curves around its top corners. */
val ServerCardBodyShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

val LocalServerCardPalette = compositionLocalOf<ServerCardPalette?> { null }

fun serverCardPalette(tone: ServerCardTone, dark: Boolean): ServerCardPalette = when (tone) {
    ServerCardTone.Running -> if (dark) {
        ServerCardPalette(Color(0xFF0B2A23), Color(0xFF065F46), Color(0xFFA7F3D0), Color(0xFF34D399))
    } else {
        ServerCardPalette(Color(0xFFD1FAE5), Color(0xFF6EE7B7), Color(0xFF065F46), Color(0xFF10B981))
    }
    ServerCardTone.Standby -> if (dark) {
        ServerCardPalette(Color(0xFF0C2A3F), Color(0xFF075985), Color(0xFFBAE6FD), Color(0xFF38BDF8))
    } else {
        ServerCardPalette(Color(0xFFE0F2FE), Color(0xFF7DD3FC), Color(0xFF075985), Color(0xFF0EA5E9))
    }
    ServerCardTone.Connecting -> if (dark) {
        ServerCardPalette(Color(0xFF2E2008), Color(0xFF92400E), Color(0xFFFDE68A), Color(0xFFFBBF24))
    } else {
        ServerCardPalette(Color(0xFFFEF3C7), Color(0xFFFCD34D), Color(0xFF92400E), Color(0xFFF59E0B))
    }
    ServerCardTone.Error -> if (dark) {
        ServerCardPalette(Color(0xFF2F1315), Color(0xFF991B1B), Color(0xFFFECACA), Color(0xFFF87171))
    } else {
        ServerCardPalette(Color(0xFFFEE2E2), Color(0xFFFCA5A5), Color(0xFF991B1B), Color(0xFFEF4444))
    }
    ServerCardTone.Neutral -> if (dark) {
        ServerCardPalette(Color(0xFF1E293B), Color(0xFF334155), Color(0xFFE2E8F0), Color(0xFF94A3B8))
    } else {
        ServerCardPalette(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF334155), Color(0xFF94A3B8))
    }
}

@Composable
fun serverCardPalette(tone: ServerCardTone): ServerCardPalette =
    serverCardPalette(tone, dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f)
