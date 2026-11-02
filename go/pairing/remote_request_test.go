package pairing

import (
	"testing"
	"time"
)

func TestRemoteTunnelRequest(t *testing.T) {
	hub := NewHub(1 * time.Second)

	req := hub.SubmitRemoteTunnelRequest("test-device", "Test Phone")
	if req.Status != "pending" {
		t.Fatalf("expected pending status, got %s", req.Status)
	}

	pending := hub.GetPendingRemoteTunnelRequests()
	if len(pending) != 1 || pending[0].DeviceID != "test-device" {
		t.Fatalf("expected 1 pending remote request")
	}

	ok := hub.ConfirmRemoteTunnelRequest("test-device", true, "https://test.trycloudflare.com", "123-456")
	if !ok {
		t.Fatalf("expected confirmation to succeed")
	}

	status, url, room := hub.CheckRemoteTunnelStatus("test-device")
	if status != "approved" || url != "https://test.trycloudflare.com" || room != "123-456" {
		t.Fatalf("unexpected remote status: %s, %s, %s", status, url, room)
	}

	if len(hub.GetPendingRemoteTunnelRequests()) != 0 {
		t.Fatalf("expected 0 pending requests after approval")
	}
}
