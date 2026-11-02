package app.duinch.remotetunnel.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

enum class ConnectMode {
    Lan,
    Remote
}

@Composable
fun DualChannelConnectContent(
    initialLanAddress: String,
    initialRemoteTarget: String,
    telemetry: RemoteTelemetryData,
    onConnect: (lan: String, remote: String) -> Unit,
    onDisconnect: () -> Unit,
    onScanQr: () -> Unit,
    onMdnsScan: (() -> Unit)? = null,
    onOpenPairing: (() -> Unit)? = null,
    isUsbConnected: Boolean = false,
    isWifiConnected: Boolean = true,
    serverRemoteUrl: String = "",
    isServerRemoteActive: Boolean = false,
    recentTunnels: List<String> = emptyList(),
    title: String = "Kết Nối Máy Chủ",
    dedicatedMode: ConnectMode? = null,
    adbStatusContent: (@Composable () -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeMode = remember(dedicatedMode, initialRemoteTarget, initialLanAddress, isWifiConnected, telemetry.channel) {
        when {
            dedicatedMode != null -> dedicatedMode
            telemetry.channel == RemoteTransportChannel.QuickTunnel || telemetry.channel == RemoteTransportChannel.WebRtcDirect -> ConnectMode.Remote
            telemetry.channel == RemoteTransportChannel.LanWifi -> ConnectMode.Lan
            !isWifiConnected -> ConnectMode.Remote
            initialRemoteTarget.isNotBlank() && initialLanAddress.isBlank() -> ConnectMode.Remote
            else -> ConnectMode.Lan
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header with Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val isLan = activeMode == ConnectMode.Lan
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isLan) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (isLan) "Wi-Fi" else "Từ Xa",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = if (isLan) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val (badgeText, badgeBg, badgeFg) = when (telemetry.state) {
                        RemoteConnectionState.Connected -> Triple("ĐÃ KẾT NỐI", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                        RemoteConnectionState.Connecting -> Triple("ĐANG KẾT NỐI...", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                        else -> Triple("CHƯA KẾT NỐI", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = badgeFg
                        )
                    }
                    if (onClose != null) {
                        IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (adbStatusContent != null) {
                Spacer(modifier = Modifier.height(14.dp))
                adbStatusContent()
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content per mode
            val effectiveRemoteTarget = when {
                serverRemoteUrl.isNotBlank() -> serverRemoteUrl
                initialRemoteTarget.contains("trycloudflare.com") || initialRemoteTarget.startsWith("http") -> initialRemoteTarget
                initialRemoteTarget.isNotBlank() -> initialRemoteTarget
                else -> ""
            }
            when (activeMode) {
                ConnectMode.Lan -> LanModeContent(
                    initialAddress = initialLanAddress,
                    telemetry = telemetry,
                    onConnect = { onConnect(it, "") },
                    onDisconnect = onDisconnect,
                    onMdnsScan = onMdnsScan,
                    onOpenPairing = onOpenPairing
                )
                ConnectMode.Remote -> RemoteModeContent(
                    initialTarget = effectiveRemoteTarget,
                    telemetry = telemetry,
                    recentTunnels = recentTunnels,
                    onConnect = { onConnect("", it) },
                    onDisconnect = onDisconnect,
                    onScanQr = onScanQr,
                    onOpenPairing = onOpenPairing
                )
            }
        }
    }
}
