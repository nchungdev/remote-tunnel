package signaling

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestMessageHub_ExchangeSignal(t *testing.T) {
	hub := NewMessageHub()
	room := "test-room"

	hostCh, err := hub.Register(room, "host")
	if err != nil {
		t.Fatalf("failed to register host: %v", err)
	}
	defer hub.Unregister(room, "host")

	_, err = hub.Register(room, "client")
	if err != nil {
		t.Fatalf("failed to register client: %v", err)
	}
	defer hub.Unregister(room, "client")

	// Client sends offer to host
	hub.Broadcast(room, SignalMessage{
		Type:      "offer",
		Sender:    "client",
		Recipient: "host",
		Payload:   json.RawMessage(`{"sdp":"v=0..."}`),
	})

	select {
	case msg := <-hostCh:
		if msg.Type != "offer" || msg.Sender != "client" {
			t.Fatalf("unexpected message: %+v", msg)
		}
	case <-time.After(500 * time.Millisecond):
		t.Fatal("timeout waiting for offer on host channel")
	}
}

func TestHTTPHandler_HandleSignal(t *testing.T) {
	hub := NewMessageHub()
	handler := NewHTTPHandler(hub, nil, "secret-token")

	hostCh, _ := hub.Register("room1", "host")
	defer hub.Unregister("room1", "host")

	body := []byte(`{"type":"candidate","payload":{"candidate":"cand1"}}`)
	req := httptest.NewRequest(http.MethodPost, "/api/webrtc/signal?room=room1&peer=client&token=secret-token", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	handler.HandleSignal(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rec.Code)
	}

	select {
	case msg := <-hostCh:
		if msg.Type != "candidate" || msg.Sender != "client" {
			t.Fatalf("unexpected signal: %+v", msg)
		}
	case <-time.After(500 * time.Millisecond):
		t.Fatal("timeout waiting for candidate on host")
	}
}
