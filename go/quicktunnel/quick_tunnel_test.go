package quicktunnel

import (
	"testing"
)

func TestQuickTunnel_InitialState(t *testing.T) {
	qt := New(8080)
	if qt.IsActive() {
		t.Fatal("expected tunnel to be inactive upon creation")
	}
	if qt.GetURL() != "" {
		t.Fatalf("expected empty URL initially, got %s", qt.GetURL())
	}
	st := qt.Status()
	if st.Active {
		t.Fatal("expected status to report inactive")
	}
	if st.Port != 8080 {
		t.Fatalf("expected port 8080, got %d", st.Port)
	}
}

func TestQuickTunnel_RegexMatch(t *testing.T) {
	sampleOutput := "2026-11-01T04:00:00Z INF +--------------------------------------------------------------------------------------------+\n" +
		"2026-11-01T04:00:00Z INF |  Your quick Tunnel has been created! Visit it at (it may take some time to be reachable):  |\n" +
		"2026-11-01T04:00:00Z INF |  https://alpha-beta-gamma.trycloudflare.com                                                  |\n" +
		"2026-11-01T04:00:00Z INF +--------------------------------------------------------------------------------------------+\n"

	match := cloudflareURLRegex.FindString(sampleOutput)
	if match != "https://alpha-beta-gamma.trycloudflare.com" {
		t.Fatalf("unexpected match: %s", match)
	}
}
