package ui

import (
	"strings"
	"testing"
)

func TestRenderRemoteChannelCard(t *testing.T) {
	card := RenderRemoteChannelCard(RemoteCardData{
		TunnelURL: "https://test.trycloudflare.com",
		RoomCode:  "123-456",
		LanURL:    "ws://192.168.1.5:8080",
		UptimeSec: 3661,
		MemoryMB:  12.5,
		GoVersion: "1.24",
	})

	if !strings.Contains(card, "https://test.trycloudflare.com") {
		t.Fatalf("expected tunnel URL in card HTML, got: %s", card)
	}
	if !strings.Contains(card, "123-456") {
		t.Fatalf("expected room code in card HTML, got: %s", card)
	}
}

func TestRenderQRModal(t *testing.T) {
	modal := RenderQRModal("/api/qr.png")
	if !strings.Contains(modal, "/api/qr.png") {
		t.Fatalf("expected qr endpoint in modal HTML, got: %s", modal)
	}
}
