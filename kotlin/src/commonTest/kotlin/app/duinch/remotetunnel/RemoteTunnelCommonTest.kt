package app.duinch.remotetunnel

import app.duinch.remotetunnel.parser.RemoteQrPayloadParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RemoteTunnelCommonTest {

    @Test
    fun testQrParser_JsonFormat() {
        val json = """{"v":1,"app":"adb-remote","url":"https://abc.trycloudflare.com","token":"test-token","room":"123-456"}"""
        val parsed = RemoteQrPayloadParser.parse(json)
        assertNotNull(parsed)
        assertEquals("https://abc.trycloudflare.com", parsed.url)
        assertEquals("test-token", parsed.token)
        assertEquals("123-456", parsed.roomCode)
        assertEquals("adb-remote", parsed.app)
        assertEquals(1, parsed.version)
    }

    @Test
    fun testQrParser_UrlFormat() {
        val url = "https://alpha.trycloudflare.com/remote?token=tok123&room=999-000"
        val parsed = RemoteQrPayloadParser.parse(url)
        assertNotNull(parsed)
        assertEquals("https://alpha.trycloudflare.com", parsed.url)
        assertEquals("tok123", parsed.token)
        assertEquals("999-000", parsed.roomCode)
    }

    @Test
    fun testQrParser_RawRoomCode() {
        val parsed = RemoteQrPayloadParser.parse("123-456")
        assertNotNull(parsed)
        assertEquals("123-456", parsed.roomCode)
    }
}
