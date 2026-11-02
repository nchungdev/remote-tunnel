package pairing

import (
	"net/http/httptest"
	"testing"
)

func TestIsLoopbackRequest(t *testing.T) {
	reqLocal := httptest.NewRequest("POST", "/api/pair/confirm", nil)
	reqLocal.RemoteAddr = "127.0.0.1:54321"
	if !IsLoopbackRequest(reqLocal) {
		t.Fatal("expected 127.0.0.1 to be loopback")
	}

	reqTunnel := httptest.NewRequest("POST", "/api/pair/confirm", nil)
	reqTunnel.RemoteAddr = "127.0.0.1:54321"
	reqTunnel.Header.Set("Cf-Connecting-Ip", "203.0.113.50")
	if IsLoopbackRequest(reqTunnel) {
		t.Fatal("expected Cloudflare proxied request to NOT be treated as direct loopback")
	}

	reqLan := httptest.NewRequest("POST", "/api/pair/confirm", nil)
	reqLan.RemoteAddr = "10.0.0.25:54321"
	if IsLoopbackRequest(reqLan) {
		t.Fatal("expected LAN IP to NOT be loopback")
	}
}

func TestIsAuthorizedRequest(t *testing.T) {
	secret := "my-secret-token-123456"

	reqTunnelNoTok := httptest.NewRequest("POST", "/api/shell", nil)
	reqTunnelNoTok.RemoteAddr = "127.0.0.1:54321"
	reqTunnelNoTok.Header.Set("CF-Ray", "8abc123")
	if IsAuthorizedRequest(reqTunnelNoTok, secret) {
		t.Fatal("expected unauthenticated tunnel request to be rejected")
	}

	reqTunnelWithTok := httptest.NewRequest("POST", "/api/shell", nil)
	reqTunnelWithTok.RemoteAddr = "127.0.0.1:54321"
	reqTunnelWithTok.Header.Set("CF-Ray", "8abc123")
	reqTunnelWithTok.Header.Set("Authorization", "Bearer "+secret)
	if !IsAuthorizedRequest(reqTunnelWithTok, secret) {
		t.Fatal("expected bearer-authenticated tunnel request to be accepted")
	}
}

func TestIsSafeWebSocketOrigin(t *testing.T) {
	reqNative := httptest.NewRequest("GET", "/ws", nil)
	if !IsSafeWebSocketOrigin(reqNative) {
		t.Fatal("expected empty Origin from native mobile app to be allowed")
	}

	reqLocal := httptest.NewRequest("GET", "/ws", nil)
	reqLocal.Header.Set("Origin", "http://127.0.0.1:8765")
	if !IsSafeWebSocketOrigin(reqLocal) {
		t.Fatal("expected localhost Origin to be allowed")
	}

	reqEvil := httptest.NewRequest("GET", "/ws", nil)
	reqEvil.Host = "127.0.0.1:8765"
	reqEvil.Header.Set("Origin", "https://evil-attacker.example.com")
	if IsSafeWebSocketOrigin(reqEvil) {
		t.Fatal("expected cross-site Origin to be blocked")
	}
}
