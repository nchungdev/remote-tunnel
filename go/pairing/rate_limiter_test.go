package pairing

import (
	"testing"
	"time"
)

func TestIPRateLimiter_Allow(t *testing.T) {
	rl := NewIPRateLimiter(3, 500*time.Millisecond)
	ip := "192.168.1.100"

	if !rl.Allow(ip) {
		t.Fatal("expected first request to be allowed")
	}
	if !rl.Allow(ip) {
		t.Fatal("expected second request to be allowed")
	}
	if !rl.Allow(ip) {
		t.Fatal("expected third request to be allowed")
	}
	if rl.Allow(ip) {
		t.Fatal("expected fourth request to be blocked")
	}

	time.Sleep(550 * time.Millisecond)
	if !rl.Allow(ip) {
		t.Fatal("expected request to be allowed after window expiration")
	}
}

func TestIPRateLimiter_Cleanup(t *testing.T) {
	rl := NewIPRateLimiter(2, 50*time.Millisecond)
	rl.Allow("1.2.3.4")
	time.Sleep(60 * time.Millisecond)
	rl.Cleanup()

	rl.mu.Lock()
	count := len(rl.limits)
	rl.mu.Unlock()
	if count != 0 {
		t.Fatalf("expected 0 entries after cleanup, got %d", count)
	}
}
