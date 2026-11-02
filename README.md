# Remote Tunnel (`remote-tunnel`)

Shared On-Demand Tunneling, WebRTC Signaling & Device Pairing Kit across the Duinch remote management ecosystem (`adb-remote` & `agent-remote`).

---

## Architecture & Responsibilities

- **Zero 3rd-party MQTT Broker**: Eliminates dependence on public brokers (e.g. HiveMQ) by providing self-hosted, authenticated HTTP Signaling endpoints.
- **On-Demand Cloudflare Quick Tunnel**: Ephemeral ingress via `cloudflared tunnel --url http://127.0.0.1:<port>` with manual start/stop lifecycle.
- **Direct P2P WebRTC**: Google STUN NAT hole-punching for maximum throughput with zero cloud proxy overhead.
- **Unified QR & Token Pairing**: Cryptographically secure token authentication with standard JSON QR payloads.

---

## Modules

```text
remote-tunnel/
├── go/                     # Go Module: github.com/nchungdev/remote-tunnel/go
│   ├── quicktunnel/        # On-demand Cloudflare Quick Tunnel manager
│   ├── signaling/          # In-memory signal hub & HTTP router (/api/tunnel/*, /api/webrtc/signal)
│   └── pairing/            # Secret token generator, pairing payload, QR code PNG & ASCII
│
└── kotlin/                 # Kotlin Multiplatform Module: app.duinch:remote-tunnel
    └── src/commonMain/
        ├── parser/         # RemoteQrPayloadParser (JSON, URL, Room Code)
        ├── network/        # ComputeReconnectBackoff with jitter
        └── webrtc/         # Default Google STUN list & WebRTC constants
```

---

## Integration Guide

### 1. In Go Servers (`adb-remote/server`, `agent-remote/server`)
```go
// go.mod
replace github.com/nchungdev/remote-tunnel/go => ../shared/remote-tunnel/go

// main.go
import (
    "github.com/nchungdev/remote-tunnel/go/quicktunnel"
    "github.com/nchungdev/remote-tunnel/go/signaling"
    "github.com/nchungdev/remote-tunnel/go/pairing"
)
```

### 2. In Android / KMP Client Apps
```kotlin
// settings.gradle.kts
includeBuild("../shared/remote-tunnel/kotlin")

// build.gradle.kts
dependencies {
    implementation("app.duinch:remote-tunnel")
}
```

---

## License
MIT
