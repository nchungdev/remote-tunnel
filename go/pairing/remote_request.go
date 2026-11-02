package pairing

import (
	"time"
)

type RemoteTunnelRequest struct {
	DeviceID   string    `json:"device_id"`
	DeviceName string    `json:"device_name"`
	Status     string    `json:"status"` // "pending", "approved", "rejected"
	TunnelURL  string    `json:"tunnel_url,omitempty"`
	RoomCode   string    `json:"room_code,omitempty"`
	CreatedAt  time.Time `json:"created_at"`
}

func (h *Hub) SubmitRemoteTunnelRequest(deviceID, deviceName string) *RemoteTunnelRequest {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.cleanupExpiredLocked()

	req := &RemoteTunnelRequest{
		DeviceID:   deviceID,
		DeviceName: deviceName,
		Status:     "pending",
		CreatedAt:  time.Now(),
	}
	if h.remoteRequests == nil {
		h.remoteRequests = make(map[string]*RemoteTunnelRequest)
	}
	h.remoteRequests[deviceID] = req
	return req
}

func (h *Hub) GetPendingRemoteTunnelRequests() []*RemoteTunnelRequest {
	h.mu.RLock()
	defer h.mu.RUnlock()

	var result []*RemoteTunnelRequest
	now := time.Now()
	for _, req := range h.remoteRequests {
		if req.Status == "pending" && now.Sub(req.CreatedAt) <= h.ttl {
			result = append(result, req)
		}
	}
	return result
}

func (h *Hub) ConfirmRemoteTunnelRequest(deviceID string, approved bool, tunnelURL, roomCode string) bool {
	h.mu.Lock()
	defer h.mu.Unlock()

	if h.remoteRequests == nil {
		return false
	}
	req, ok := h.remoteRequests[deviceID]
	if !ok {
		return false
	}
	if approved {
		req.Status = "approved"
		req.TunnelURL = tunnelURL
		req.RoomCode = roomCode
	} else {
		req.Status = "rejected"
	}
	return true
}

func (h *Hub) CheckRemoteTunnelStatus(deviceID string) (status, url, room string) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	if h.remoteRequests == nil {
		return "none", "", ""
	}
	req, ok := h.remoteRequests[deviceID]
	if !ok {
		return "none", "", ""
	}
	if time.Since(req.CreatedAt) > h.ttl {
		return "expired", "", ""
	}
	return req.Status, req.TunnelURL, req.RoomCode
}
