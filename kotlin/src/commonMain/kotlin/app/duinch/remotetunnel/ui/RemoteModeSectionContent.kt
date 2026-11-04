package app.duinch.remotetunnel.ui

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val DEFAULT_REMOTE_PLACEHOLDER = "Dán URL Cloudflare hoặc mã phòng"

@Composable
internal fun RemoteModeContent(
    initialTarget: String,
    telemetry: RemoteTelemetryData,
    recentTunnels: List<String> = emptyList(),
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onScanQr: () -> Unit,
    onOpenPairing: (() -> Unit)? = null,
    remotePlaceholder: String = DEFAULT_REMOTE_PLACEHOLDER,
    onDeleteRecentTunnel: ((String) -> Unit)? = null,
    chrome: ServerCardChrome
) {
    val cleanInitial = remember(initialTarget) {
        val trimmed = initialTarget.trim()
        if (trimmed == "default") "" else trimmed
    }
    var remoteText by rememberSaveable(cleanInitial) { mutableStateOf(cleanInitial) }
    var isEditingUrl by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var editUrlInput by rememberSaveable(remoteText) { mutableStateOf(remoteText) }
    var detectedLanHost by rememberSaveable { mutableStateOf<String?>(null) }
    var dismissedHandover by rememberSaveable { mutableStateOf(false) }

    val isRemoteConnected = telemetry.state == RemoteConnectionState.Connected &&
        (telemetry.channel == RemoteTransportChannel.QuickTunnel || telemetry.channel == RemoteTransportChannel.WebRtcDirect)
    val isRemoteConnecting = telemetry.state == RemoteConnectionState.Connecting &&
        (telemetry.channel == RemoteTransportChannel.QuickTunnel || telemetry.channel == RemoteTransportChannel.WebRtcDirect)
    val showInlineSetup = isEditingUrl || remoteText.isBlank()
    val effectiveTarget = if (showInlineSetup) editUrlInput.trim() else remoteText.trim()

    if (showDetails) {
        ConnectionDetailsDialog(
            items = listOf(
                "Địa chỉ máy chủ" to remoteText,
                "Trạng thái" to when {
                    isRemoteConnected -> "Đã kết nối"
                    isRemoteConnecting -> "Đang kết nối"
                    else -> "Chưa kết nối"
                },
                "Độ trễ mạng" to (telemetry.pingMs?.takeIf { isRemoteConnected }?.let { "$it ms" } ?: "--")
            ) + chrome.detailItems,
            onDismiss = { showDetails = false },
            onDisconnect = chrome.onDisconnect.takeIf { isRemoteConnected || isRemoteConnecting },
            onConnect = { onConnect(remoteText) }.takeIf { !isRemoteConnected && !isRemoteConnecting && remoteText.isNotBlank() },
            onDelete = chrome.onClose,
            footer = chrome.detailsFooter
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        ServerCardHeader(
            chrome = chrome,
            address = if (showInlineSetup) "" else remoteText.substringAfter("://").trimEnd('/'),
            channelLabel = "Từ xa",
            state = telemetry.state,
            onShowDetails = { showDetails = true }.takeIf { !showInlineSetup },
            onEdit = {
                editUrlInput = remoteText
                isEditingUrl = true
            }.takeIf { !showInlineSetup }
        )
        if (!chrome.expanded && !isEditingUrl) return@Column

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, ServerCardBodyShape)
                .padding(ServerCardBodyPadding)
        ) {
            chrome.belowHeader?.invoke()
            if (!isRemoteConnected || isEditingUrl || onOpenPairing != null) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isRemoteConnected && detectedLanHost != null && !dismissedHandover) {
                LanHandoverBanner(
                    lanHost = detectedLanHost!!,
                    onSwitch = {
                        onDisconnect()
                        onConnect(detectedLanHost!!)
                    },
                    onDismiss = { dismissedHandover = true }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (showInlineSetup) {
                Text(
                    text = "Quét QR hoặc dán link máy chủ:",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editUrlInput,
                        onValueChange = { editUrlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text(remotePlaceholder, fontSize = 12.5.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (editUrlInput.isNotBlank()) {
                                remoteText = editUrlInput.trim()
                                isEditingUrl = false
                                onConnect(remoteText)
                            }
                        }),
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (remoteText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { if (editUrlInput.isNotBlank()) { remoteText = editUrlInput.trim(); isEditingUrl = false } },
                            modifier = Modifier.size(36.dp)
                        ) { Icon(Icons.Default.Check, contentDescription = "Lưu", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp)) }
                        IconButton(
                            onClick = { editUrlInput = remoteText; isEditingUrl = false },
                            modifier = Modifier.size(36.dp)
                        ) { Icon(Icons.Default.Close, contentDescription = "Hủy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            RecentTunnelsSection(
                recentTunnels = recentTunnels,
                selectedUrl = effectiveTarget,
                onSelectTunnel = {
                    remoteText = it
                    editUrlInput = it
                    isEditingUrl = false
                },
                onDeleteTunnel = onDeleteRecentTunnel
            )

            if (isRemoteConnected) {
                if (onOpenPairing != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenPairing,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ghép đôi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onScanQr,
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quét QR", fontSize = 13.sp, maxLines = 1)
                    }

                    if (!showInlineSetup) {
                        OutlinedButton(
                            onClick = {
                                editUrlInput = remoteText
                                isEditingUrl = true
                            },
                            modifier = Modifier.weight(1f).height(42.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đổi URL", fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (isRemoteConnecting) {
                            onDisconnect()
                        } else if (effectiveTarget.isNotBlank()) {
                            remoteText = effectiveTarget
                            isEditingUrl = false
                            onConnect(effectiveTarget)
                        }
                    },
                    enabled = effectiveTarget.isNotBlank() || isRemoteConnecting,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isRemoteConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang kết nối...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kết Nối Máy Chủ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
