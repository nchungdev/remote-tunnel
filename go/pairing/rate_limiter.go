package pairing

import (
	"sync"
	"time"
)

// IPRateLimiter provides sliding-window request throttling per IP address.
type IPRateLimiter struct {
	mu          sync.Mutex
	limits      map[string][]time.Time
	maxRequests int
	window      time.Duration
}

func NewIPRateLimiter(maxRequests int, window time.Duration) *IPRateLimiter {
	return &IPRateLimiter{
		limits:      make(map[string][]time.Time),
		maxRequests: maxRequests,
		window:      window,
	}
}

// Allow reports whether a request from ip is permitted under the rate limit.
func (rl *IPRateLimiter) Allow(ip string) bool {
	if ip == "" {
		return true
	}
	rl.mu.Lock()
	defer rl.mu.Unlock()

	now := time.Now()
	cutoff := now.Add(-rl.window)

	// Clean expired timestamps for this IP
	timestamps := rl.limits[ip]
	valid := timestamps[:0]
	for _, t := range timestamps {
		if t.After(cutoff) {
			valid = append(valid, t)
		}
	}

	if len(valid) >= rl.maxRequests {
		rl.limits[ip] = valid
		return false
	}

	rl.limits[ip] = append(valid, now)
	return true
}

// Cleanup purges stale IP entries from memory periodically.
func (rl *IPRateLimiter) Cleanup() {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	now := time.Now()
	cutoff := now.Add(-rl.window)
	for ip, timestamps := range rl.limits {
		valid := timestamps[:0]
		for _, t := range timestamps {
			if t.After(cutoff) {
				valid = append(valid, t)
			}
		}
		if len(valid) == 0 {
			delete(rl.limits, ip)
		} else {
			rl.limits[ip] = valid
		}
	}
}
