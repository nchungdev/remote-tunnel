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
    onOpenPairing: (() -> Unit)? = null
) {
    var currentHost by remember(initialAddress) {
        val clean = initialAddress.trim()
        val effective = if (clean.contains("trycloudflare.com") || clean.contains("://")) "" else clean
        mutableStateOf(effective)
    }
    var isEditingHost by remember { mutableStateOf(false) }
    var editHostInput by remember(currentHost) { mutableStateOf(currentHost) }

    val isLanConnected = telemetry.state == RemoteConnectionState.Connected && telemetry.channel == RemoteTransportChannel.LanWifi
    val isLanConnecting = telemetry.state == RemoteConnectionState.Connecting && telemetry.channel == RemoteTransportChannel.LanWifi
    val effectiveTarget = if (isEditingHost) editHostInput.trim() else currentHost.trim()

    LaunchedEffect(Unit) {
        if (currentHost.isBlank()) {
            onMdnsScan?.invoke()
        }
    }

    val dotColor = when {
        isLanConnected -> Color(0xFF10B981)
        isLanConnecting -> Color(0xFFF59E0B)
        else -> Color(0xFF94A3B8)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isEditingHost) {
            Text(
                text = "Nhập địa chỉ IP máy chủ hoặc bấm Quét lại để tự tìm:",
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
                        Text(
                            text = currentHost.ifBlank { "Chưa thiết lập IP máy chủ" },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = if (currentHost.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                isLanConnected -> "Đã kết nối qua mạng Wi-Fi LAN"
                                currentHost.isNotBlank() -> "Sẵn sàng kết nối qua mạng Wi-Fi LAN"
                                else -> "Bấm biểu tượng sửa bên phải để nhập IP hoặc bấm Quét lại"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                    IconButton(
                        onClick = {
                            editHostInput = currentHost
                            isEditingHost = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Chỉnh sửa IP",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            val detectedPort = currentHost.substringAfterLast(":", "").takeIf { it.isNotEmpty() && it.all { c -> c.isDigit() } } ?: "8080"
            ConnectionSpecsCard(
                items = listOf(
                    "Địa chỉ máy chủ" to (currentHost.ifBlank { "Chưa cấu hình" }),
                    "Trạng thái Server" to when {
                        isLanConnected -> "Đã kết nối (Port $detectedPort)"
                        isLanConnecting -> "Đang kết nối (Port $detectedPort)"
                        else -> "Sẵn sàng (Port $detectedPort)"
                    },
                    "Độ trễ mạng" to (telemetry.pingMs?.takeIf { isLanConnected }?.let { "$it ms" } ?: "--")
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLanConnected) {
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
                    Text(if (onOpenPairing != null) "Ngắt Kết Nối" else "Ngắt Kết Nối Wi-Fi", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
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
