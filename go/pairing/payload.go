package pairing

import (
	"encoding/json"
	"time"
)

// PairingPayload is the standardized JSON object encoded inside a pairing QR code.
type PairingPayload struct {
	Version   int    `json:"v"`
	App       string `json:"app"` // "adb-remote" or "agent-remote"
	URL       string `json:"url"` // Quick Tunnel URL
	LAN       string `json:"lan,omitempty"` // Local LAN IP/Host (e.g. 192.168.1.10:8080)
	Token     string `json:"token,omitempty"`
	RoomCode  string `json:"room,omitempty"`
	CreatedAt int64  `json:"ts"`
}

// BuildPairingJSON generates the standardized QR code payload string.
func BuildPairingJSON(app, url, token, roomCode string, lan ...string) (string, error) {
	lanAddr := ""
	if len(lan) > 0 {
		lanAddr = lan[0]
	}
	p := PairingPayload{
		Version:   1,
		App:       app,
		URL:       url,
		LAN:       lanAddr,
		Token:     token,
		RoomCode:  roomCode,
		CreatedAt: time.Now().Unix(),
	}
	bytes, err := json.Marshal(p)
	if err != nil {
		return "", err
	}
	return string(bytes), nil
}

// ParsePairingJSON parses a QR code payload string into PairingPayload.
func ParsePairingJSON(raw string) (*PairingPayload, error) {
	var p PairingPayload
	if err := json.Unmarshal([]byte(raw), &p); err != nil {
		return nil, err
	}
	return &p, nil
}
