package app.duinch.remotetunnel.ui

enum class RemoteConnectionState {
    Connected,
    Connecting,
    Disconnected
}

enum class RemoteTransportChannel(val label: String) {
    None("Chưa kết nối"),
    UsbCable("Cáp USB"),
    LanWifi("Wi-Fi LAN"),
    QuickTunnel("Cloudflare 4G/5G"),
    WebRtcDirect("P2P WebRTC Direct")
}

data class RemoteTelemetryData(
    val state: RemoteConnectionState = RemoteConnectionState.Disconnected,
    val channel: RemoteTransportChannel = RemoteTransportChannel.None,
    val host: String = "",
    val roomCode: String = "",
    val pingMs: Long? = null,
    val uptimeSec: Long = 0L,
    val bytesTransferred: Long = 0L,
    val transferSpeedBps: Long = 0L
)
