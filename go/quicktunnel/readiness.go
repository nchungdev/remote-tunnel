package quicktunnel

import (
	"context"
	"net"
	"strings"
	"time"
)

const (
	// A new hostname usually resolves within ~2s of cloudflared printing it.
	dnsReadyTimeout = 8 * time.Second
	dnsPollInterval = 250 * time.Millisecond
)

// publicResolver asks a public DNS server directly. Polling through the system
// resolver would cache the early NXDOMAIN: the very failure this wait avoids.
var publicResolver = &net.Resolver{
	PreferGo: true,
	Dial: func(ctx context.Context, network, _ string) (net.Conn, error) {
		var d net.Dialer
		return d.DialContext(ctx, network, "1.1.1.1:53")
	},
}

func hostnameResolves(ctx context.Context, host string) bool {
	addrs, err := publicResolver.LookupHost(ctx, host)
	return err == nil && len(addrs) > 0
}

// waitForDNS blocks until host resolves. On timeout it returns anyway: a network
// that blocks 1.1.1.1 must not stop the tunnel from being published.
func waitForDNS(ctx context.Context, url string, resolves func(context.Context, string) bool) {
	host := strings.TrimPrefix(url, "https://")
	ctx, cancel := context.WithTimeout(ctx, dnsReadyTimeout)
	defer cancel()
	for !resolves(ctx, host) {
		select {
		case <-ctx.Done():
			return
		case <-time.After(dnsPollInterval):
		}
	}
}
