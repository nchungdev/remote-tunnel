package pairing

import (
	"net"
	"net/http"
	"strings"
)

// IsLoopbackRequest returns true if the HTTP request originates directly from
// the local machine (127.0.0.1 / ::1) and is NOT proxied through Cloudflare Quick Tunnel
// or a reverse proxy (checks Cf-Connecting-Ip and X-Forwarded-For).
func IsLoopbackRequest(r *http.Request) bool {
	if r == nil {
		return false
	}
	if r.Header.Get("Cf-Connecting-Ip") != "" ||
		r.Header.Get("CF-Ray") != "" ||
		r.Header.Get("X-Forwarded-For") != "" {
		return false
	}
	host := r.RemoteAddr
	if h, _, err := net.SplitHostPort(r.RemoteAddr); err == nil {
		host = h
	}
	host = strings.Trim(host, "[]")
	if host == "localhost" {
		return true
	}
	ip := net.ParseIP(host)
	if ip == nil {
		return host == "127.0.0.1" || host == "::1" || host == ""
	}
	return ip.IsLoopback()
}

// ExtractBearerOrQueryToken extracts a token from Authorization: Bearer <token>,
// X-Auth-Token header, or ?token=<token> query parameter.
func ExtractBearerOrQueryToken(r *http.Request) string {
	if r == nil {
		return ""
	}
	authHeader := strings.TrimSpace(r.Header.Get("Authorization"))
	if strings.HasPrefix(strings.ToLower(authHeader), "bearer ") {
		return strings.TrimSpace(authHeader[7:])
	}
	if xTok := strings.TrimSpace(r.Header.Get("X-Auth-Token")); xTok != "" {
		return xTok
	}
	return strings.TrimSpace(r.URL.Query().Get("token"))
}

// IsAuthorizedRequest returns true if the request is from localhost (unproxied)
// OR carries a valid token matching expectedToken.
func IsAuthorizedRequest(r *http.Request, expectedToken string) bool {
	if IsLoopbackRequest(r) {
		return true
	}
	if expectedToken == "" {
		return true
	}
	actual := ExtractBearerOrQueryToken(r)
	if actual == "" {
		return false
	}
	return VerifyToken(expectedToken, actual)
}

// IsSafeWebSocketOrigin protects against cross-site WebSocket hijacking (CSWSH)
// from arbitrary third-party websites opened in the host's browser.
// Native mobile/desktop clients omit Origin or send a local/tunnel origin.
func IsSafeWebSocketOrigin(r *http.Request) bool {
	if r == nil {
		return false
	}
	origin := strings.TrimSpace(r.Header.Get("Origin"))
	if origin == "" {
		return true
	}
	lower := strings.ToLower(origin)
	if strings.HasPrefix(lower, "http://127.0.0.1") ||
		strings.HasPrefix(lower, "http://localhost") ||
		strings.HasPrefix(lower, "https://127.0.0.1") ||
		strings.HasPrefix(lower, "https://localhost") ||
		strings.Contains(lower, ".trycloudflare.com") {
		return true
	}
	// Allow same-host LAN origin if Origin host matches r.Host
	if r.Host != "" && strings.Contains(lower, strings.ToLower(r.Host)) {
		return true
	}
	return false
}
