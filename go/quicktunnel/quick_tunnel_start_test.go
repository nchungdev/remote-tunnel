package quicktunnel

import (
	"context"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

const fakeURL = "https://fake-host.trycloudflare.com"

// installFakeCloudflared puts a script first on PATH that logs like cloudflared and counts its launches.
func installFakeCloudflared(t *testing.T) (launches string) {
	t.Helper()
	dir := t.TempDir()
	launches = filepath.Join(dir, "launches")
	script := "#!/bin/sh\necho x >> '" + launches + "'\n" +
		"echo 'INF |  " + fakeURL + "  |' >&2\nsleep 0.2\n" +
		"echo 'INF Registered tunnel connection connIndex=0' >&2\nsleep 30\n"
	if err := os.WriteFile(filepath.Join(dir, "cloudflared"), []byte(script), 0o755); err != nil {
		t.Fatal(err)
	}
	t.Setenv("PATH", dir+string(os.PathListSeparator)+os.Getenv("PATH"))
	return launches
}

func TestStart_WaitsForRegistrationAndDNS(t *testing.T) {
	installFakeCloudflared(t)
	tun := New(9)
	t.Cleanup(func() { _ = tun.Stop() })

	var dnsChecks atomic.Int32
	tun.resolves = func(_ context.Context, host string) bool {
		if host != "fake-host.trycloudflare.com" {
			t.Errorf("unexpected host %q", host)
		}
		if tun.GetURL() != "" {
			t.Error("URL visible before DNS is ready")
		}
		return dnsChecks.Add(1) >= 3
	}

	begin := time.Now()
	url, err := tun.Start(context.Background())
	if err != nil || url != fakeURL {
		t.Fatalf("Start = %q, %v", url, err)
	}
	if time.Since(begin) < 200*time.Millisecond {
		t.Fatal("returned before the tunnel registered")
	}
	if dnsChecks.Load() != 3 {
		t.Fatalf("dns checks = %d, want 3", dnsChecks.Load())
	}
	if !tun.IsActive() {
		t.Fatal("tunnel should be active after Start")
	}
}

func TestStart_ConcurrentCallsShareOneProcess(t *testing.T) {
	launches := installFakeCloudflared(t)
	tun := New(9)
	t.Cleanup(func() { _ = tun.Stop() })
	tun.resolves = func(context.Context, string) bool { return true }

	var wg sync.WaitGroup
	for range 4 {
		wg.Add(1)
		go func() {
			defer wg.Done()
			if url, err := tun.Start(context.Background()); err != nil || url != fakeURL {
				t.Errorf("Start = %q, %v", url, err)
			}
		}()
	}
	wg.Wait()

	raw, err := os.ReadFile(launches)
	if err != nil {
		t.Fatal(err)
	}
	if n := strings.Count(string(raw), "x"); n != 1 {
		t.Fatalf("cloudflared launched %d times, want 1", n)
	}
}

func TestWaitForDNS_GivesUpWithoutBlockingForever(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 300*time.Millisecond)
	defer cancel()
	begin := time.Now()
	waitForDNS(ctx, fakeURL, func(context.Context, string) bool { return false })
	if time.Since(begin) > 2*time.Second {
		t.Fatal("waitForDNS did not honour its deadline")
	}
}
