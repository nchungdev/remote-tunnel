package pairing

import (
	"sync"
	"time"
)

type PairingRequest struct {
	DeviceID   string    `json:"device_id"`
	DeviceName string    `json:"device_name"`
	Code       string    `json:"code"`
	Status     string    `json:"status"` // "pending", "approved", "rejected"
	CreatedAt  time.Time `json:"created_at"`
}

type Hub struct {
	mu             sync.RWMutex
	requests       map[string]*PairingRequest
	remoteRequests map[string]*RemoteTunnelRequest
	ttl            time.Duration
}

func NewHub(ttl time.Duration) *Hub {
	if ttl <= 0 {
		ttl = 3 * time.Minute
	}
	return &Hub{
		requests:       make(map[string]*PairingRequest),
		remoteRequests: make(map[string]*RemoteTunnelRequest),
		ttl:            ttl,
	}
}

func (h *Hub) SubmitRequest(deviceID, deviceName, code string) *PairingRequest {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.cleanupExpiredLocked()

	req := &PairingRequest{
		DeviceID:   deviceID,
		DeviceName: deviceName,
		Code:       code,
		Status:     "pending",
		CreatedAt:  time.Now(),
	}
	h.requests[deviceID] = req
	return req
}

func (h *Hub) GetPending() []*PairingRequest {
	h.mu.RLock()
	defer h.mu.RUnlock()

	var result []*PairingRequest
	now := time.Now()
	for _, req := range h.requests {
		if req.Status == "pending" && now.Sub(req.CreatedAt) <= h.ttl {
			result = append(result, req)
		}
	}
	return result
}

func (h *Hub) Confirm(deviceID string, approved bool) bool {
	h.mu.Lock()
	defer h.mu.Unlock()

	req, ok := h.requests[deviceID]
	if !ok {
		return false
	}
	if approved {
		req.Status = "approved"
	} else {
		req.Status = "rejected"
	}
	return true
}

func (h *Hub) CheckStatus(deviceID string) string {
	h.mu.RLock()
	defer h.mu.RUnlock()

	req, ok := h.requests[deviceID]
	if !ok {
		return "none"
	}
	if time.Since(req.CreatedAt) > h.ttl {
		return "expired"
	}
	return req.Status
}

func (h *Hub) cleanupExpiredLocked() {
	now := time.Now()
	for id, req := range h.requests {
		if now.Sub(req.CreatedAt) > h.ttl {
			delete(h.requests, id)
		}
	}
	for id, req := range h.remoteRequests {
		if now.Sub(req.CreatedAt) > h.ttl {
			delete(h.remoteRequests, id)
		}
	}
}
