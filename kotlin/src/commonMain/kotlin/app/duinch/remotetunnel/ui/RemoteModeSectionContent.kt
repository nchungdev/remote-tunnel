package app.duinch.remotetunnel.ui

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

@Composable
internal fun RemoteModeContent(
    initialTarget: String,
    telemetry: RemoteTelemetryData,
    recentTunnels: List<String> = emptyList(),
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onScanQr: () -> Unit,
    onOpenPairing: (() -> Unit)? = null
) {
    val cleanInitial = remember(initialTarget) {
        val trimmed = initialTarget.trim()
        if (trimmed == "default") "" else trimmed
    }
    var remoteText by remember(cleanInitial) { mutableStateOf(cleanInitial) }
    var isEditingUrl by remember { mutableStateOf(false) }
    var editUrlInput by remember(remoteText) { mutableStateOf(remoteText) }
    var detectedLanHost by remember { mutableStateOf<String?>(null) }
    var dismissedHandover by remember { mutableStateOf(false) }

    val isRemoteConnected = telemetry.state == RemoteConnectionState.Connected &&
        (telemetry.channel == RemoteTransportChannel.QuickTunnel || telemetry.channel == RemoteTransportChannel.WebRtcDirect)
    val isRemoteConnecting = telemetry.state == RemoteConnectionState.Connecting &&
        (telemetry.channel == RemoteTransportChannel.QuickTunnel || telemetry.channel == RemoteTransportChannel.WebRtcDirect)
    val showInlineSetup = isEditingUrl || remoteText.isBlank()
    val effectiveTarget = if (showInlineSetup) editUrlInput.trim() else remoteText.trim()

    val dotColor = when {
        isRemoteConnected -> Color(0xFF10B981)
        isRemoteConnecting -> Color(0xFFF59E0B)
        else -> Color(0xFF94A3B8)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                text = "Quét mã QR trên máy tính hoặc dán link máy chủ từ xa:",
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
                    placeholder = { Text("Dán URL Cloudflare hoặc mã phòng", fontSize = 12.5.sp) },
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
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(dotColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val displayStatus = if (remoteText.contains("trycloudflare.com")) {
                            val hostShort = remoteText.removePrefix("https://").removePrefix("http://").substringBefore(".trycloudflare.com").take(18)
                            "$hostShort..."
                        } else {
                            remoteText.removePrefix("https://").removePrefix("http://")
                        }
                        Text(
                            text = displayStatus,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val subtitle = when {
                            isRemoteConnected -> "Đã kết nối thông suốt với máy tính"
                            isRemoteConnecting -> "Đang thiết lập kênh truyền bảo mật..."
                            else -> "Đã sẵn sàng đường truyền P2P / Cloudflare"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        RecentTunnelsSection(
            recentTunnels = recentTunnels,
            selectedUrl = effectiveTarget,
            onSelectTunnel = {
                remoteText = it
                editUrlInput = it
                isEditingUrl = false
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isRemoteConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val disconnectMod = if (onOpenPairing != null) Modifier.weight(1.3f) else Modifier.fillMaxWidth()
                Button(
                    onClick = onDisconnect,
                    modifier = disconnectMod.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (onOpenPairing != null) "Ngắt Kết Nối" else "Ngắt Kết Nối Từ Xa", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }

                if (onOpenPairing != null) {
                    OutlinedButton(
                        onClick = onOpenPairing,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ghép đôi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onScanQr,
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quét QR", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }

                if (!showInlineSetup) {
                    OutlinedButton(
                        onClick = {
                            editUrlInput = remoteText
                            isEditingUrl = true
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đổi URL", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }

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
                    modifier = Modifier.weight(if (showInlineSetup) 1.3f else 1.15f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isRemoteConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kết Nối", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
