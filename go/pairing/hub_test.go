package pairing

import (
	"testing"
	"time"
)

func TestPairingHub(t *testing.T) {
	hub := NewHub(1 * time.Second)

	req := hub.SubmitRequest("dev-1", "Samsung S24", "123-456")
	if req.Status != "pending" {
		t.Fatalf("expected pending, got %s", req.Status)
	}

	pending := hub.GetPending()
	if len(pending) != 1 || pending[0].DeviceID != "dev-1" {
		t.Fatalf("expected 1 pending request for dev-1")
	}

	if hub.CheckStatus("dev-1") != "pending" {
		t.Fatalf("expected status pending")
	}

	if !hub.Confirm("dev-1", true) {
		t.Fatalf("expected confirm to succeed")
	}

	if hub.CheckStatus("dev-1") != "approved" {
		t.Fatalf("expected status approved")
	}

	if len(hub.GetPending()) != 0 {
		t.Fatalf("expected 0 pending after approval")
	}
}
