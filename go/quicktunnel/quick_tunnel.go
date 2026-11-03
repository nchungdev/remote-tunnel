package quicktunnel

import (
	"bufio"
	"context"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strings"
	"sync"
	"time"
)

var cloudflareURLRegex = regexp.MustCompile(`https://[a-zA-Z0-9-]+\.trycloudflare\.com`)

// Status represents the current state of the Quick Tunnel.
type Status struct {
	Active    bool   `json:"active"`
	URL       string `json:"url,omitempty"`
	Port      int    `json:"port"`
	StartedAt int64  `json:"started_at,omitempty"`
	Error     string `json:"error,omitempty"`
}

// QuickTunnel manages an on-demand ephemeral Cloudflare Quick Tunnel process.
type QuickTunnel struct {
	port           int
	url            string
	cmd            *exec.Cmd
	cancel         context.CancelFunc
	mu             sync.RWMutex
	startedAt      time.Time
	lastError      string
	onURLFound     func(url string)
	onStateChanged func(active bool, url string)
	starting       *startAttempt
	resolves       func(ctx context.Context, host string) bool
}

// startAttempt lets concurrent Start calls share one cloudflared process.
type startAttempt struct {
	done chan struct{}
	url  string
	err  error
}

// Option configures QuickTunnel options.
type Option func(*QuickTunnel)

// WithURLCallback sets a callback triggered when the tunnel URL is discovered.
func WithURLCallback(cb func(url string)) Option {
	return func(t *QuickTunnel) {
		t.onURLFound = cb
	}
}

// WithStateCallback sets a callback triggered when active state changes.
func WithStateCallback(cb func(active bool, url string)) Option {
	return func(t *QuickTunnel) {
		t.onStateChanged = cb
	}
}

// New creates a new QuickTunnel instance for the specified local port.
// Note: It does NOT start automatically; call Start() explicitly when needed.
func New(port int, opts ...Option) *QuickTunnel {
	t := &QuickTunnel{
		port:     port,
		resolves: hostnameResolves,
	}
	for _, opt := range opts {
		opt(t)
	}
	return t
}

// Start launches cloudflared tunnel on-demand and returns the trycloudflare.com URL
// once clients can use it: the tunnel is registered and its hostname resolves.
func (t *QuickTunnel) Start(parentCtx context.Context) (string, error) {
	t.mu.Lock()
	if t.cmd != nil && t.url != "" {
		curURL := t.url
		t.mu.Unlock()
		return curURL, nil
	}
	if pending := t.starting; pending != nil {
		t.mu.Unlock()
		<-pending.done
		return pending.url, pending.err
	}
	attempt := &startAttempt{done: make(chan struct{})}
	t.starting = attempt
	t.lastError = ""
	t.mu.Unlock()

	attempt.url, attempt.err = t.launch()

	t.mu.Lock()
	t.starting = nil
	t.mu.Unlock()
	close(attempt.done)
	return attempt.url, attempt.err
}

func (t *QuickTunnel) launch() (string, error) {

	binPath, err := exec.LookPath("cloudflared")
	if err != nil {
		errStr := fmt.Sprintf("cloudflared chưa được cài đặt trên hệ thống: %v", err)
		t.mu.Lock()
		t.lastError = errStr
		t.mu.Unlock()
		return "", fmt.Errorf("%s", errStr)
	}

	ctx, cancel := context.WithCancel(context.Background())
	cmd := exec.CommandContext(ctx, binPath, "tunnel", "--url", fmt.Sprintf("http://127.0.0.1:%d", t.port))
	stderr, err := cmd.StderrPipe()
	if err != nil {
		cancel()
		errStr := fmt.Sprintf("không thể mở stderr pipe của cloudflared: %v", err)
		t.mu.Lock()
		t.lastError = errStr
		t.mu.Unlock()
		return "", fmt.Errorf("%s", errStr)
	}

	if err := cmd.Start(); err != nil {
		cancel()
		errStr := fmt.Sprintf("không thể khởi chạy cloudflared: %v", err)
		t.mu.Lock()
		t.lastError = errStr
		t.mu.Unlock()
		return "", fmt.Errorf("%s", errStr)
	}

	t.mu.Lock()
	t.cmd = cmd
	t.cancel = cancel
	t.startedAt = time.Now()
	t.mu.Unlock()

	urlCh := make(chan string, 1)

	go func() {
		var candidate string
		published := false
		scanner := bufio.NewScanner(stderr)
		for scanner.Scan() {
			line := scanner.Text()
			if match := cloudflareURLRegex.FindString(line); match != "" {
				candidate = match
			}
			if candidate != "" && !published && strings.Contains(line, "Registered tunnel connection") {
				published = true
				go t.publish(ctx, candidate, urlCh)
			}
		}

		_ = cmd.Wait()
		t.mu.Lock()
		if t.cmd != cmd {
			t.mu.Unlock() // Stopped and possibly restarted: this state is no longer ours.
			return
		}
		t.url = ""
		t.cmd = nil
		t.cancel = nil
		t.mu.Unlock()
		if t.onStateChanged != nil {
			t.onStateChanged(false, "")
		}
	}()

	select {
	case foundURL := <-urlCh:
		return foundURL, nil
	case <-time.After(25 * time.Second):
		t.Stop()
		errStr := "quá thời gian chờ URL từ Cloudflare Quick Tunnel (25s)"
		t.mu.Lock()
		t.lastError = errStr
		t.mu.Unlock()
		return "", fmt.Errorf("%s", errStr)
	case <-ctx.Done():
		t.Stop()
		return "", ctx.Err()
	}
}

// publish exposes the URL only when usable. Handing it out earlier makes clients
// cache NXDOMAIN for the new hostname and stay unable to connect long after it is live.
func (t *QuickTunnel) publish(ctx context.Context, url string, urlCh chan<- string) {
	waitForDNS(ctx, url, t.resolves)
	if ctx.Err() != nil {
		return
	}

	t.mu.Lock()
	t.url = url
	t.mu.Unlock()

	t.saveTunnelURL(url)
	if t.onURLFound != nil {
		t.onURLFound(url)
	}
	if t.onStateChanged != nil {
		t.onStateChanged(true, url)
	}
	urlCh <- url
	log.Printf("[QuickTunnel] Đường hầm đã kích hoạt: %s", url)
}

// Stop terminates the running cloudflared process.
func (t *QuickTunnel) Stop() error {
	t.mu.Lock()
	defer t.mu.Unlock()

	if t.cancel != nil {
		t.cancel()
		t.cancel = nil
	}
	if t.cmd != nil && t.cmd.Process != nil {
		_ = t.cmd.Process.Kill()
		t.cmd = nil
	}
	t.url = ""
	if t.onStateChanged != nil {
		t.onStateChanged(false, "")
	}
	log.Println("[QuickTunnel] Đường hầm đã đóng.")
	return nil
}

// GetURL returns the active tunnel URL, or empty string if stopped.
func (t *QuickTunnel) GetURL() string {
	t.mu.RLock()
	defer t.mu.RUnlock()
	return t.url
}

// IsActive returns whether the tunnel is actively running with a valid URL.
func (t *QuickTunnel) IsActive() bool {
	t.mu.RLock()
	defer t.mu.RUnlock()
	return t.url != "" && t.cmd != nil
}

// Status returns the current status snapshot.
func (t *QuickTunnel) Status() Status {
	t.mu.RLock()
	defer t.mu.RUnlock()

	var startedAtEpoch int64
	if !t.startedAt.IsZero() {
		startedAtEpoch = t.startedAt.Unix()
	}

	return Status{
		Active:    t.url != "" && t.cmd != nil,
		URL:       t.url,
		Port:      t.port,
		StartedAt: startedAtEpoch,
		Error:     t.lastError,
	}
}

func (t *QuickTunnel) saveTunnelURL(url string) {
	home, err := os.UserHomeDir()
	if err != nil {
		return
	}
	dir := filepath.Join(home, ".duinch-remote")
	_ = os.MkdirAll(dir, 0755)
	_ = os.WriteFile(filepath.Join(dir, "tunnel_url"), []byte(url), 0600)
}
