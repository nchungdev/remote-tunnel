package app.duinch.remotetunnel.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Connection state as the card shows it; Running and Standby are both connected. */
enum class ServerCardTone { Neutral, Running, Standby, Connecting, Error }

internal fun RemoteConnectionState.defaultTone(): ServerCardTone = when (this) {
    RemoteConnectionState.Connected -> ServerCardTone.Running
    RemoteConnectionState.Connecting -> ServerCardTone.Connecting
    else -> ServerCardTone.Neutral
}

/** What every connection card's header needs from its owner, whichever mode it renders. */
internal val ServerCardBodyPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)

internal data class ServerCardChrome(
    val title: String,
    val displayName: String? = null,
    val onRename: (() -> Unit)? = null,
    val statusNote: String?,
    val tone: ServerCardTone,
    val expanded: Boolean,
    val onToggleExpanded: (() -> Unit)?,
    val onClose: (() -> Unit)?,
    val onDisconnect: () -> Unit,
    val belowHeader: (@Composable () -> Unit)?,
    val detailItems: List<Pair<String, String>> = emptyList(),
    val detailsFooter: (@Composable () -> Unit)? = null,
    val showMenu: Boolean = false
)

/** The server identifies the card: its address leads, with channel and state on one line below. */
@Composable
internal fun ServerCardHeader(
    chrome: ServerCardChrome,
    address: String,
    channelLabel: String,
    state: RemoteConnectionState,
    hint: String? = null,
    onShowDetails: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val stateText = when {
        chrome.statusNote == "Không có sẵn" -> "Không có sẵn"
        state == RemoteConnectionState.Connecting -> "Đang kết nối..."
        state != RemoteConnectionState.Connected -> "Chưa kết nối"
        chrome.tone == ServerCardTone.Standby -> "Dự phòng"
        else -> "Đang dùng"
    }
    val statusLine = hint ?: listOfNotNull(
        channelLabel,
        stateText,
        chrome.statusNote.takeIf { it != "Không có sẵn" && state == RemoteConnectionState.Connected }
    ).joinToString(" • ")

    ServerCardHeaderRow(
        title = chrome.displayName ?: address.ifBlank { chrome.title },
        statusLine = statusLine,
        tone = chrome.tone,
        expanded = chrome.expanded,
        onToggleExpanded = chrome.onToggleExpanded,
        beforeToggle = {
            if (chrome.expanded || state == RemoteConnectionState.Connected) {
                onShowDetails?.let { HeaderAction(Icons.Default.Info, "Chi tiết kết nối", it) }
            }
        },
        afterToggle = {
            if (chrome.showMenu) {
                ServerCardMenu(
                    onRename = chrome.onRename,
                    onEdit = onEdit,
                    onDisconnect = chrome.onDisconnect.takeIf {
                        state == RemoteConnectionState.Connected || state == RemoteConnectionState.Connecting
                    },
                    onDelete = chrome.onClose
                )
            }
        }
    )
}

/** The band every connection shows, whatever carries it: state dot, name, status line, then actions. */
@Composable
fun ServerCardHeaderRow(
    title: String,
    statusLine: String,
    tone: ServerCardTone,
    expanded: Boolean,
    onToggleExpanded: (() -> Unit)?,
    beforeToggle: @Composable () -> Unit = {},
    afterToggle: @Composable () -> Unit = {}
) {
    val palette = serverCardPalette(tone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onToggleExpanded != null) Modifier.clickable(onClick = onToggleExpanded) else Modifier)
            .background(palette.card)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(9.dp).background(palette.accent, CircleShape))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = palette.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusLine,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = palette.content.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        beforeToggle()
        if (onToggleExpanded != null) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
                tint = palette.content,
                modifier = Modifier.size(22.dp).rotate(if (expanded) 180f else 0f)
            )
        }
        afterToggle()
    }
}

@Composable
private fun HeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = LocalServerCardPalette.current?.content ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ServerCardMenu(
    onRename: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDisconnect: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    if (onRename == null && onEdit == null && onDisconnect == null && onDelete == null) return
    var open by remember { mutableStateOf(false) }
    Box {
        HeaderAction(Icons.Default.MoreVert, "Tùy chọn kết nối") { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            onRename?.let { MenuAction(Icons.Default.TextFields, "Đổi tên") { open = false; it() } }
            onEdit?.let { MenuAction(Icons.Default.Edit, "Sửa địa chỉ") { open = false; it() } }
            onDisconnect?.let { MenuAction(Icons.Default.PowerSettingsNew, "Ngắt kết nối") { open = false; it() } }
            onDelete?.let {
                MenuAction(Icons.Default.Delete, "Xoá kết nối", MaterialTheme.colorScheme.error) { open = false; it() }
            }
        }
    }
}

@Composable
private fun MenuAction(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, color = tint) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) },
        onClick = onClick
    )
}
