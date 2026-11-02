package pairing

import (
	"fmt"

	"github.com/skip2/go-qrcode"
)

// GenerateQRPNG generates PNG image bytes for the given payload string.
func GenerateQRPNG(content string, size int) ([]byte, error) {
	if size <= 0 {
		size = 256
	}
	return qrcode.Encode(content, qrcode.Medium, size)
}

// GenerateQRAscii generates terminal-printable ASCII string representation of QR code.
func GenerateQRAscii(content string, invert bool) (string, error) {
	qr, err := qrcode.New(content, qrcode.Medium)
	if err != nil {
		return "", fmt.Errorf("failed to generate qr: %w", err)
	}
	return qr.ToSmallString(invert), nil
}
