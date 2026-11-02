package app.duinch.remotetunnel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectionTelemetryBar(
    telemetry: RemoteTelemetryData,
    onTriggerConnect: () -> Unit,
    modifier: Modifier = Modifier,
    hideWhenDisconnected: Boolean = true,
    onDetailsClick: (() -> Unit)? = null
) {
    if (hideWhenDisconnected && telemetry.state == RemoteConnectionState.Disconnected) {
        return
    }
    val statusColor = when (telemetry.state) {
        RemoteConnectionState.Connected -> Color(0xFF10B981)
        RemoteConnectionState.Connecting -> Color(0xFFF59E0B)
        RemoteConnectionState.Disconnected -> Color(0xFFEF4444)
    }

    val statusText = when (telemetry.state) {
        RemoteConnectionState.Connected -> "ĐÃ KẾT NỐI"
        RemoteConnectionState.Connecting -> "ĐANG NỐI..."
        RemoteConnectionState.Disconnected -> "CHƯA KẾT NỐI"
    }

    val hrs = telemetry.uptimeSec / 3600
    val mins = (telemetry.uptimeSec % 3600) / 60
    val secs = telemetry.uptimeSec % 60
    val uptimeText = if (telemetry.state == RemoteConnectionState.Connected) {
        "%02d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "--:--:--"
    }

    val pingText = if (telemetry.pingMs != null && telemetry.state == RemoteConnectionState.Connected) {
        "${telemetry.pingMs}ms"
    } else {
        "-- ms"
    }

    val clickModifier = when {
        telemetry.state == RemoteConnectionState.Disconnected -> Modifier.clickable { onTriggerConnect() }
        onDetailsClick != null -> Modifier.clickable { onDetailsClick() }
        else -> Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Status dot & Channel badge & Action link
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = statusColor
                    )
                    if (telemetry.host.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${telemetry.host}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (telemetry.state == RemoteConnectionState.Disconnected || onDetailsClick != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (telemetry.state == RemoteConnectionState.Disconnected) "Ghép nối" else "Chi tiết",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bento Grid: 3 metrics (Channel, Ping, Uptime)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoMetricItem(
                    icon = Icons.Default.SignalCellularAlt,
                    label = "KÊNH",
                    value = telemetry.channel.label,
                    modifier = Modifier.weight(1.3f)
                )
                BentoMetricItem(
                    icon = Icons.Default.Speed,
                    label = "PING",
                    value = pingText,
                    modifier = Modifier.weight(1f)
                )
                BentoMetricItem(
                    icon = Icons.Default.Timer,
                    label = "UPTIME",
                    value = uptimeText,
                    modifier = Modifier.weight(1.1f)
                )
            }
        }
    }
}

@Composable
private fun BentoMetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
