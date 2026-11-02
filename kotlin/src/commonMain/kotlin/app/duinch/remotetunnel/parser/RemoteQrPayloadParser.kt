package app.duinch.remotetunnel.parser

data class RemoteQrPayload(
    val url: String? = null,
    val lan: String? = null,
    val token: String? = null,
    val roomCode: String? = null,
    val app: String? = null,
    val version: Int = 1
)

object RemoteQrPayloadParser {

    fun parse(rawText: String): RemoteQrPayload? {
        val text = rawText.trim()
        if (text.isEmpty()) return null

        // 1. JSON QR format: {"v":1,"app":"adb-remote","url":"https://...","lan":"...","token":"...","room":"..."}
        if (text.startsWith("{") && text.endsWith("}")) {
            val url = Regex("\"(?:url|tunnel)\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)?.trim()
            val lan = Regex("\"lan\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)?.trim()
            val token = Regex("\"token\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)?.trim()
            val room = Regex("\"(?:room|roomCode)\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)?.trim()
            val app = Regex("\"app\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)?.trim()
            val v = Regex("\"v\"\\s*:\\s*(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 1

            if (url != null || lan != null || room != null || token != null) {
                return RemoteQrPayload(
                    url = url,
                    lan = lan,
                    token = token,
                    roomCode = room,
                    app = app,
                    version = v
                )
            }
        }

        // 2. Try URL format (e.g. https://abc.trycloudflare.com or http://192.168.1.15:8080/remote?token=xxx&room=yyy)
        if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true) ||
            text.startsWith("ws://", ignoreCase = true) || text.startsWith("wss://", ignoreCase = true)) {
            val cleanUrl = text.substringBefore("/remote").substringBefore('?').trimEnd('/')
            val token = Regex("[?&]token=([a-zA-Z0-9_-]+)").find(text)?.groupValues?.get(1)
            val room = Regex("[?&]room=([a-zA-Z0-9_-]+)").find(text)?.groupValues?.get(1)
            return RemoteQrPayload(
                url = cleanUrl,
                token = token,
                roomCode = room
            )
        }

        // 3. Raw 6-digit room code fallback (e.g. 589-214 or 589214)
        val clean = text.filter { it.isLetterOrDigit() || it == '-' }
        if (clean.length in 5..16) {
            return RemoteQrPayload(roomCode = clean)
        }

        return null
    }
}
