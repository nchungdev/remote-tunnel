package ui

import "fmt"

type RemoteCardData struct {
	TunnelURL string
	RoomCode  string
	LanURL    string
	UptimeSec int64
	MemoryMB  float64
	GoVersion string
}

func RenderRemoteChannelCard(d RemoteCardData) string {
	hrs := d.UptimeSec / 3600
	mins := (d.UptimeSec % 3600) / 60
	secs := d.UptimeSec % 60
	statsText := fmt.Sprintf("%dh %dm %ds • %.1f MB RAM (Go %s)", hrs, mins, secs, d.MemoryMB, d.GoVersion)

	tunnelVal := d.TunnelURL
	if tunnelVal == "" {
		tunnelVal = "Chưa kích hoạt (On-Demand)"
	}

	return fmt.Sprintf(`
        <div class="card remote-channel-card">
            <div class="card-title">
                <span>🌐 Kênh Kết Nối Từ Xa</span>
                <button class="btn" style="padding: 4px 10px; font-size: 12px;" onclick="refreshStatus()">🔄 Cập nhật</button>
            </div>
            <div class="info-row">
                <div>
                    <div class="label">☁️ Cloudflare Quick Tunnel (4G/5G)</div>
                    <div class="val" id="tunnelUrlText">%s</div>
                </div>
                <div style="display: flex; gap: 6px;">
                    <button class="copy-btn" onclick="copyText('tunnelUrlText')">Sao chép</button>
                    <button class="copy-btn" style="color: var(--orange);" onclick="restartTunnel()">Làm mới</button>
                </div>
            </div>
            <div class="info-row">
                <div>
                    <div class="label">🔑 P2P Room Code (WebRTC)</div>
                    <div class="val" id="roomCodeText" style="font-size: 16px; color: #60A5FA;">%s</div>
                </div>
                <div style="display: flex; gap: 6px;">
                    <button class="copy-btn" onclick="copyText('roomCodeText')">Sao chép</button>
                    <button class="copy-btn" style="color: var(--orange);" onclick="rotateRoom()">Đổi mã</button>
                </div>
            </div>
            <div class="info-row">
                <div>
                    <div class="label">📶 Wi-Fi LAN WebSocket</div>
                    <div class="val" id="lanUrlText">%s</div>
                </div>
                <button class="copy-btn" onclick="copyText('lanUrlText')">Sao chép</button>
            </div>
            <div class="info-row" style="margin-bottom: 0;">
                <div>
                    <div class="label">⏱️ Uptime / Bộ nhớ RAM</div>
                    <div class="val" id="serverStats">%s</div>
                </div>
            </div>
        </div>
`, tunnelVal, d.RoomCode, d.LanURL, statsText)
}

func RenderQRModal(qrEndpoint string) string {
	return fmt.Sprintf(`
    <div class="modal-overlay" id="qrModal" onclick="if(event.target===this) closeQRModal()">
        <div class="modal-content">
            <h2 style="font-size: 18px; font-weight: 700; color: #FFF;">⚡ Quét Mã Ghép Nối</h2>
            <p style="font-size: 12px; color: var(--text-secondary); margin-top: 4px;">Mở ứng dụng trên Android/iOS và quét mã này để kết nối tức thì.</p>
            <img class="qr-img" id="qrImage" src="%s" alt="QR Code">
            <div style="display: flex; justify-content: center;"><button class="btn" onclick="closeQRModal()">Đóng cửa sổ</button></div>
        </div>
    </div>
`, qrEndpoint)
}
