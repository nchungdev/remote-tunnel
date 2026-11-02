package signaling

import (
	"encoding/json"
	"errors"
	"sync"
)

// SignalMessage represents an SDP or ICE candidate exchange payload.
type SignalMessage struct {
	Type      string          `json:"type"`                // "offer", "answer", "candidate", "ping", "pong"
	Sender    string          `json:"sender,omitempty"`    // "host" or "client"
	Recipient string          `json:"recipient,omitempty"` // targeted recipient ID
	Payload   json.RawMessage `json:"payload,omitempty"`   // SDP or ICE JSON object
	Room      string          `json:"room,omitempty"`
}

type peerSubscriber struct {
	ch chan SignalMessage
}

// MessageHub handles in-memory message routing between WebRTC peers within rooms.
type MessageHub struct {
	mu    sync.RWMutex
	rooms map[string]map[string]*peerSubscriber
}

func NewMessageHub() *MessageHub {
	return &MessageHub{
		rooms: make(map[string]map[string]*peerSubscriber),
	}
}

func (h *MessageHub) Register(room, peerID string) (<-chan SignalMessage, error) {
	if room == "" || peerID == "" {
		return nil, errors.New("room and peerID must not be empty")
	}

	h.mu.Lock()
	defer h.mu.Unlock()

	peers, exists := h.rooms[room]
	if !exists {
		peers = make(map[string]*peerSubscriber)
		h.rooms[room] = peers
	}

	ch := make(chan SignalMessage, 64)
	peers[peerID] = &peerSubscriber{ch: ch}
	return ch, nil
}

func (h *MessageHub) Unregister(room, peerID string) {
	h.mu.Lock()
	defer h.mu.Unlock()

	peers, exists := h.rooms[room]
	if !exists {
		return
	}

	if sub, ok := peers[peerID]; ok {
		close(sub.ch)
		delete(peers, peerID)
	}

	if len(peers) == 0 {
		delete(h.rooms, room)
	}
}

func (h *MessageHub) Broadcast(room string, msg SignalMessage) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	peers, exists := h.rooms[room]
	if !exists {
		return
	}

	msg.Room = room
	for peerID, sub := range peers {
		if msg.Recipient != "" && peerID != msg.Recipient {
			continue
		}
		if peerID == msg.Sender {
			continue
		}

		select {
		case sub.ch <- msg:
		default:
			// avoid blocking if channel buffer is full
		}
	}
}
