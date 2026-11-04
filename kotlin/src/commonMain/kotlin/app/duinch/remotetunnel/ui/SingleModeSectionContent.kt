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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.LaunchedEffect
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
internal fun LanModeContent(
    initialAddress: String,
    telemetry: RemoteTelemetryData,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onMdnsScan: (() -> Unit)? = null,
    onOpenPairing: (() -> Unit)? = null,
    chrome: ServerCardChrome
) {
    var currentHost by rememberSaveable(initialAddress) {
        val clean = initialAddress.trim()
        // Saved addresses are normalized to http://host:port; this field shows host:port.
        val effective = if (clean.contains("trycloudflare.com")) "" else clean.substringAfter("://").trimEnd('/')
        mutableStateOf(effective)
    }
    var isEditingHost by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var editHostInput by rememberSaveable(currentHost) { mutableStateOf(currentHost) }

    val isLanConnected = telemetry.state == RemoteConnectionState.Connected && telemetry.channel == RemoteTransportChannel.LanWifi
    val isLanConnecting = telemetry.state == RemoteConnectionState.Connecting && telemetry.channel == RemoteTransportChannel.LanWifi
    val effectiveTarget = if (isEditingHost) editHostInput.trim() else currentHost.trim()

    LaunchedEffect(Unit) {
        if (currentHost.isBlank()) {
            onMdnsScan?.invoke()
        }
    }

    if (showDetails) {
        ConnectionDetailsDialog(
            items = listOf(
                "Địa chỉ máy chủ" to currentHost,
                "Trạng thái Server" to when {
                    isLanConnected -> "Đã kết nối"
                    isLanConnecting -> "Đang kết nối"
                    else -> "Sẵn sàng"
                },
                "Độ trễ mạng" to (telemetry.pingMs?.takeIf { isLanConnected }?.let { "$it ms" } ?: "--")
            ) + chrome.detailItems,
            onDismiss = { showDetails = false },
            onDisconnect = chrome.onDisconnect.takeIf { isLanConnected || isLanConnecting },
            onConnect = { onConnect(currentHost) }.takeIf { !isLanConnected && !isLanConnecting && currentHost.isNotBlank() },
            onDelete = chrome.onClose,
            footer = chrome.detailsFooter
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        ServerCardHeader(
            chrome = chrome,
            address = if (isEditingHost) "" else currentHost,
            channelLabel = "Wi-Fi",
            state = telemetry.state,
            hint = "Bấm mở rộng để nhập IP, hoặc bấm Quét lại".takeIf { currentHost.isBlank() && !isEditingHost },
            onShowDetails = { showDetails = true }.takeIf { currentHost.isNotBlank() && !isEditingHost },
            onEdit = {
                editHostInput = currentHost
                isEditingHost = true
            }.takeIf { !isEditingHost }
        )
        if (!chrome.expanded && !isEditingHost) return@Column

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, ServerCardBodyShape)
                .padding(ServerCardBodyPadding)
        ) {
            chrome.belowHeader?.invoke()
            if (!isLanConnected || isEditingHost || onOpenPairing != null) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isEditingHost) {
                Text(
                    text = "Nhập IP máy chủ hoặc bấm Quét lại:",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editHostInput,
                        onValueChange = { editHostInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Ví dụ: 192.168.1.10:8080", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (editHostInput.isNotBlank()) {
                                currentHost = editHostInput.trim()
                                isEditingHost = false
                            }
                        }),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (editHostInput.isNotBlank()) {
                                currentHost = editHostInput.trim()
                                isEditingHost = false
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Default.Check, contentDescription = "Lưu", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp)) }
                    IconButton(
                        onClick = { editHostInput = currentHost; isEditingHost = false },
                        modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Default.Close, contentDescription = "Hủy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }


            if (isLanConnected) {
                if (onOpenPairing != null) {
                    OutlinedButton(
                        onClick = onOpenPairing,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ghép đôi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onMdnsScan?.invoke() },
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quét lại", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            if (isLanConnecting) {
                                onDisconnect()
                            } else if (effectiveTarget.isNotBlank()) {
                                currentHost = effectiveTarget
                                isEditingHost = false
                                onConnect(effectiveTarget)
                            }
                        },
                        enabled = effectiveTarget.isNotBlank() || isLanConnecting,
                        modifier = Modifier.weight(1.2f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLanConnecting) {
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
}
