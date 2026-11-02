package signaling

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"github.com/nchungdev/remote-tunnel/go/quicktunnel"
)

// HTTPHandler provides REST endpoints for WebRTC signaling and on-demand Quick Tunnel management.
type HTTPHandler struct {
	hub       *MessageHub
	tunnel    *quicktunnel.QuickTunnel
	authToken string
}

func NewHTTPHandler(hub *MessageHub, tunnel *quicktunnel.QuickTunnel, authToken string) *HTTPHandler {
	return &HTTPHandler{
		hub:       hub,
		tunnel:    tunnel,
		authToken: strings.TrimSpace(authToken),
	}
}

func (h *HTTPHandler) verifyAuth(r *http.Request) bool {
	if h.authToken == "" {
		return true
	}
	token := r.URL.Query().Get("token")
	if token == "" {
		authHeader := r.Header.Get("Authorization")
		if strings.HasPrefix(authHeader, "Bearer ") {
			token = strings.TrimPrefix(authHeader, "Bearer ")
		}
	}
	return token == h.authToken
}

// HandleSignal processes WebRTC signaling messages via HTTP POST.
// URL query: ?room=<roomCode>&peer=<peerID>
func (h *HTTPHandler) HandleSignal(w http.ResponseWriter, r *http.Request) {
	if !h.verifyAuth(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	room := r.URL.Query().Get("room")
	if room == "" {
		room = "default"
	}

	var msg SignalMessage
	if err := json.NewDecoder(r.Body).Decode(&msg); err != nil {
		http.Error(w, "Invalid JSON body", http.StatusBadRequest)
		return
	}

	peerID := r.URL.Query().Get("peer")
	if peerID == "" {
		peerID = msg.Sender
	}
	if peerID == "" {
		peerID = "client"
	}
	msg.Sender = peerID

	h.hub.Broadcast(room, msg)

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]any{"ok": true})
}

// HandleTunnelStatus returns JSON describing whether Quick Tunnel is currently active.
func (h *HTTPHandler) HandleTunnelStatus(w http.ResponseWriter, r *http.Request) {
	if h.tunnel == nil {
		http.Error(w, "Quick Tunnel not configured", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(h.tunnel.Status())
}

// HandleTunnelStart initiates Quick Tunnel on-demand.
func (h *HTTPHandler) HandleTunnelStart(w http.ResponseWriter, r *http.Request) {
	if !h.verifyAuth(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	if h.tunnel == nil {
		http.Error(w, "Quick Tunnel not configured", http.StatusNotFound)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 30*time.Second)
	defer cancel()

	url, err := h.tunnel.Start(ctx)
	w.Header().Set("Content-Type", "application/json")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_ = json.NewEncoder(w).Encode(map[string]any{
			"ok":    false,
			"error": err.Error(),
		})
		return
	}

	_ = json.NewEncoder(w).Encode(map[string]any{
		"ok":  true,
		"url": url,
	})
}

// HandleTunnelStop shuts down the running Quick Tunnel.
func (h *HTTPHandler) HandleTunnelStop(w http.ResponseWriter, r *http.Request) {
	if !h.verifyAuth(r) {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	if h.tunnel == nil {
		http.Error(w, "Quick Tunnel not configured", http.StatusNotFound)
		return
	}

	_ = h.tunnel.Stop()
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]any{
		"ok":     true,
		"active": false,
	})
}
