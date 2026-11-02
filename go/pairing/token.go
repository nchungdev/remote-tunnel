package pairing

import (
	"crypto/rand"
	"crypto/subtle"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

// GenerateToken generates a cryptographically secure random hex token.
func GenerateToken(byteLen int) string {
	if byteLen <= 0 {
		byteLen = 16
	}
	b := make([]byte, byteLen)
	_, _ = rand.Read(b)
	return hex.EncodeToString(b)
}

// VerifyToken compares two tokens in constant time to prevent timing attacks.
func VerifyToken(expected, actual string) bool {
	if expected == "" {
		return true // No authentication required if expected token is empty
	}
	return subtle.ConstantTimeCompare([]byte(expected), []byte(actual)) == 1
}

// LoadOrGenerateSecretToken loads token from file or generates and saves a new one.
func LoadOrGenerateSecretToken(dataDir, filename string) string {
	if filename == "" {
		filename = "auth_token"
	}
	tokenFile := filepath.Join(dataDir, filename)
	if data, err := os.ReadFile(tokenFile); err == nil {
		tok := strings.TrimSpace(string(data))
		if len(tok) >= 16 {
			return tok
		}
	}

	tok := GenerateToken(16)
	_ = os.MkdirAll(dataDir, 0755)
	_ = os.WriteFile(tokenFile, []byte(tok), 0600)
	return tok
}

// GenerateRoomCode generates a user-friendly 6-digit room code formatted as "XXX-XXX".
func GenerateRoomCode() string {
	b := make([]byte, 3)
	_, _ = rand.Read(b)
	num := (int(b[0])<<16 | int(b[1])<<8 | int(b[2]))%900000 + 100000
	return fmt.Sprintf("%03d-%03d", num/1000, num%1000)
}
