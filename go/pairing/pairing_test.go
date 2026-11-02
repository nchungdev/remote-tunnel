package pairing

import (
	"testing"
)

func TestToken_GenerationAndVerification(t *testing.T) {
	tok := GenerateToken(16)
	if len(tok) != 32 {
		t.Fatalf("expected 32 hex chars, got %d", len(tok))
	}
	if !VerifyToken(tok, tok) {
		t.Fatal("expected token to match itself")
	}
	if VerifyToken(tok, "wrong-token") {
		t.Fatal("expected token mismatch")
	}
}

func TestPairingPayload_SerializeDeserialize(t *testing.T) {
	raw, err := BuildPairingJSON("adb-remote", "https://test.trycloudflare.com", "my-token", "123-456")
	if err != nil {
		t.Fatalf("failed to build json: %v", err)
	}

	parsed, err := ParsePairingJSON(raw)
	if err != nil {
		t.Fatalf("failed to parse json: %v", err)
	}

	if parsed.App != "adb-remote" || parsed.URL != "https://test.trycloudflare.com" || parsed.Token != "my-token" || parsed.RoomCode != "123-456" {
		t.Fatalf("unexpected parsed values: %+v", parsed)
	}
}

func TestQR_GeneratePNGAndAscii(t *testing.T) {
	png, err := GenerateQRPNG("https://test.trycloudflare.com", 128)
	if err != nil {
		t.Fatalf("failed to generate qr png: %v", err)
	}
	if len(png) < 50 {
		t.Fatalf("png too small: %d bytes", len(png))
	}

	ascii, err := GenerateQRAscii("https://test.trycloudflare.com", false)
	if err != nil {
		t.Fatalf("failed to generate qr ascii: %v", err)
	}
	if len(ascii) == 0 {
		t.Fatal("expected non-empty ascii qr")
	}
}
