package app.duinch.remotetunnel.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
    remotePlaceholder: String = DEFAULT_REMOTE_PLACEHOLDER,
    title: String = "Kết Nối Máy Chủ",
    dedicatedMode: ConnectMode? = null,
    adbStatusContent: (@Composable () -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    displayName: String? = null,
    onRename: (() -> Unit)? = null,
    statusNote: String? = null,
    tone: ServerCardTone? = null,
    detailItems: List<Pair<String, String>> = emptyList(),
    detailsFooter: (@Composable () -> Unit)? = null,
    expanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val formStates = rememberSaveableStateHolder()
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

    val cardTone = tone ?: telemetry.state.defaultTone()
    val palette = serverCardPalette(cardTone)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = palette.card,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, palette.border)
    ) {
        CompositionLocalProvider(LocalServerCardPalette provides palette) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val chrome = ServerCardChrome(
                title = title,
                displayName = displayName,
                onRename = onRename,
                statusNote = statusNote,
                tone = cardTone,
                expanded = expanded,
                onToggleExpanded = onToggleExpanded,
                onClose = onClose,
                onDisconnect = onDisconnect,
                belowHeader = adbStatusContent,
                detailItems = detailItems,
                detailsFooter = detailsFooter
            )

            // Content per mode
            val effectiveRemoteTarget = when {
                serverRemoteUrl.isNotBlank() -> serverRemoteUrl
                initialRemoteTarget.contains("trycloudflare.com") || initialRemoteTarget.startsWith("http") -> initialRemoteTarget
                initialRemoteTarget.isNotBlank() -> initialRemoteTarget
                else -> ""
            }
            // Collapsing removes this content; the holder keeps what the user typed.
            formStates.SaveableStateProvider(activeMode.name) {
                when (activeMode) {
                    ConnectMode.Lan -> LanModeContent(
                        initialAddress = initialLanAddress,
                        telemetry = telemetry,
                        onConnect = { onConnect(it, "") },
                        onDisconnect = onDisconnect,
                        onMdnsScan = onMdnsScan,
                        onOpenPairing = onOpenPairing,
                        chrome = chrome
                    )
                    ConnectMode.Remote -> RemoteModeContent(
                        initialTarget = effectiveRemoteTarget,
                        telemetry = telemetry,
                        recentTunnels = recentTunnels,
                        onConnect = { onConnect("", it) },
                        onDisconnect = onDisconnect,
                        onScanQr = onScanQr,
                        onOpenPairing = onOpenPairing,
                        remotePlaceholder = remotePlaceholder,
                        chrome = chrome
                    )
                }
            }
        }
        }
    }
}
