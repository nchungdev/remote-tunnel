package app.duinch.remotetunnel.webrtc

object WebRtcConstants {
    val DEFAULT_STUN_SERVERS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302"
    )

    const val DATA_CHANNEL_ADB = "adb-stream"
    const val DATA_CHANNEL_AGENT = "agent-stream"
}
