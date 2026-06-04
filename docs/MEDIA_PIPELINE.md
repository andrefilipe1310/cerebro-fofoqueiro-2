<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [Arquitetura](./ARCHITECTURE.md)
  - [Data Model](./DATA_MODEL.md)
  - [ENV Config](./ENV_CONFIG.md)
  - [Multi-Tenancy](./MULTI_TENANCY.md)
  - [API Contracts](./API_CONTRACTS.md)
-->

# Pipeline de Mídia {#media-pipeline}

## 1. Diagrama de Fluxo Completo {#diagrama}

```
Câmera IP (RTSP/RTMP)
        │
        │ rtsp://user:pass@192.168.x.x:554/stream
        ▼
┌───────────────────────────────────────────────────────────────┐
│                      MediaMTX                                  │
│  Path: /tenant_{id}/camera_{id}/main   (stream principal)     │
│  Path: /tenant_{id}/camera_{id}/sub    (sub-stream)           │
│                                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ WebRTC/WHEP  │  │  HLS Segm.   │  │  FFmpeg Worker   │   │
│  │ (live view)  │  │  (playback)  │  │  (gravação+thumb)│   │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘   │
└─────────┼─────────────────┼───────────────────┼─────────────┘
          │                 │                   │
          │ < 500ms         │ HLS segments       │ upload
          ▼                 ▼                   ▼
      Browser           Browser          Cloudflare R2
    (mosaico /          (timeline /     /tenant/camera/
     fullscreen)         playback)      YYYY/MM/DD/HH/
                                        seg_N.ts
                                        thumb_N.jpg
                                              │
                                              ▼
                                     Backend registra metadados
                                     em recordings + thumbnails
```

---

## 2. Configuração do MediaMTX {#mediamtx-config}

Arquivo de configuração: `mediamtx.yml`

```yaml
# mediamtx.yml
logLevel: info
logDestinations: [stdout]

# API REST de administração (usada pelo CHMS do backend)
api: yes
apiAddress: :9997

# Autenticação via webhook — backend valida acesso por tenant
authMethod: http
authHTTPAddress: http://backend:8080/internal/media/auth
authHTTPExclude:
  - action: publish
    # câmeras publicam sem auth (o path já garante isolamento)

# Configuração de protocols disponíveis
protocols: [tcp, udp, webrtc, hls]

# Configuração WebRTC
webrtc: yes
webrtcAddress: :8889
webrtcICEServers:
  - urls: [stun:stun.l.google.com:19302]
  # Adicionar TURN server para produção:
  # - urls: [turn:turn.domain.com:3478]
  #   username: ${TURN_USERNAME}
  #   credential: ${TURN_CREDENTIAL}

# Configuração HLS
hls: yes
hlsAddress: :8888
hlsSegmentDuration: 2s      # segmentos de 2s para low-latency HLS
hlsSegmentMaxSize: 50MB
hlsAllowOrigin: "*"

# Paths dinâmicos por câmera
pathDefaults:
  # Record tudo que chegar via RTSP
  record: yes
  recordPath: /tmp/recordings/%path/%Y-%m-%d_%H-%M-%S-%f
  recordFormat: fmp4
  recordSegmentDuration: 60s  # segmentos de 1 min (FFmpeg reagrupa para HLS)

  # Source: câmera publica, nós consumimos
  sourceOnDemand: yes
  sourceOnDemandStartTimeout: 10s
  sourceOnDemandCloseAfter: 30s

paths:
  # Padrão para todos os paths de tenant
  "~^tenant_[a-f0-9-]+/camera_[a-f0-9-]+/.*$":
    source: publisher
    readUser: ""        # autenticação feita pelo webhook, não por usuário fixo
    readPass: ""
```

---

## 3. Resolution Ladder {#resolution-ladder}

Para economizar banda no mosaico, cada câmera ideal oferece 2 streams:

| Stream | Resolução alvo | Uso | Bitrate típico |
|---|---|---|---|
| `main` | 1080p (1920×1080) | Fullscreen, gravação | 2–4 Mbps |
| `sub` | 480p (854×480) | Células do mosaico | 400–800 Kbps |

**Configuração no cadastro da câmera:**
- `rtsp_url_encrypted` → main stream (H.264/H.265 nativo da câmera)
- `sub_stream_url_encrypted` → sub stream (configurado na câmera, normalmente stream 2)

**Fallback:** Se a câmera não oferece sub-stream nativo, o MediaMTX pode transcodificar via FFmpeg, porém isso aumenta o uso de CPU. Preferir câmeras com 2 streams nativos.

---

## 4. Latência WebRTC — Garantindo < 500ms {#latencia-webrtc}

### 4.1 Protocolo WHEP (WebRTC-HTTP Egress Protocol)

WHEP é o padrão que MediaMTX usa para servir WebRTC aos browsers. O flow:

```
Browser                          MediaMTX
   │                                │
   │── POST /path/whep ────────────►│
   │   (SDP Offer)                  │
   │                                │
   │◄── 201 Created ────────────────│
   │   (SDP Answer + ICE candidates)│
   │                                │
   │── ICE connectivity checks ────►│
   │◄──────────────────────────────│
   │                                │
   │◄══ RTP video stream ═══════════│ (< 500ms após SDP answer)
```

### 4.2 Configurações Críticas para Baixa Latência

```typescript
// Frontend — configuração do RTCPeerConnection
const pc = new RTCPeerConnection({
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    // TURN para NAT restrito:
    { urls: 'turn:turn.domain.com:3478', username: '...', credential: '...' }
  ],
  bundlePolicy: 'max-bundle',
  rtcpMuxPolicy: 'require',
});

// Desabilitar buffer de jitter (aumenta latência):
// O MediaMTX já configura isso no lado do servidor com playoutDelay=0
```

```yaml
# mediamtx.yml — reduzir latência WebRTC
webrtcLocalUDPAddress: :8189    # UDP preferido sobre TCP para menor latência
webrtcIPsFromInterfaces: yes    # coleta IPs de todas as interfaces para ICE
```

### 4.3 Fallback HLS quando WebRTC Falha

```typescript
// Lógica de fallback no player (frontend)
async function loadCameraStream(cameraId: string) {
  try {
    const { url } = await api.get(`/cameras/${cameraId}/stream/live`);
    await initWebRTCPlayer(url); // tenta WebRTC primeiro
  } catch (err) {
    console.warn('WebRTC falhou, usando HLS', err);
    const { url } = await api.get(`/cameras/${cameraId}/stream/hls/main`);
    initHLSPlayer(url); // fallback para HLS via hls.js
  }
}
```

---

## 5. Gravação Contínua {#gravacao-continua}

### 5.1 Segmentação e Naming Convention no R2

```
Cloudflare R2 bucket: {R2_BUCKET_NAME}

Estrutura de paths:
  {tenant_id}/
    {camera_id}/
      2026/
        06/
          04/
            00/              ← hora UTC
              seg_0001.ts    ← segmento HLS (6 segundos)
              seg_0002.ts
              seg_0003.ts
              index.m3u8     ← playlist HLS da hora
            01/
              ...
          04/
            index.m3u8       ← playlist do dia (referencia as horas)
      thumbs/
        2026/06/04/00/
          thumb_0001.jpg     ← thumbnail gerado a cada 30s
```

### 5.2 Processo de Upload para R2

```
MediaMTX grava segmento em disco local (/tmp/recordings/...)
        │ evento de segmento completado
        ▼
RecordingWorker (Spring @Scheduled ou consumer de fila)
        │ lê o arquivo .ts
        ├── Faz upload para R2 via SDK S3-compatible
        │   key: {tenant_id}/{camera_id}/YYYY/MM/DD/HH/seg_{N}.ts
        ├── Insere registro em recordings [ver DATA_MODEL.md#recordings]
        │   com: camera_id, tenant_id, started_at, ended_at, r2_key, size_bytes
        └── Deleta arquivo local após upload confirmado
```

### 5.3 Configuração FFmpeg para HLS

```bash
# Comando FFmpeg gerado pelo backend/MediaMTX para gravar e segmentar
ffmpeg \
  -i rtsp://... \                          # input: stream da câmera
  -c:v copy \                              # sem reencoding (passa direto)
  -c:a aac \                               # audio para AAC se necessário
  -f hls \                                 # formato HLS
  -hls_time 6 \                            # segmentos de 6 segundos
  -hls_list_size 0 \                       # manter todos os segmentos na playlist
  -hls_segment_filename "seg_%04d.ts" \    # naming
  -hls_flags append_list+split_by_time \  # flags para gravação contínua
  index.m3u8
```

---

## 6. Geração de Thumbnails {#thumbnails}

```bash
# FFmpeg extrai um frame a cada 30 segundos
ffmpeg \
  -i rtsp://... \
  -vf fps=1/30 \                    # 1 frame a cada 30 segundos
  -vframes 1 \                      # apenas 1 frame por execução (cron)
  -q:v 2 \                          # qualidade JPEG alta
  -s 320x180 \                      # resolução do thumbnail
  thumb_%04d.jpg
```

Os thumbnails são usados em:
- Mosaico (para câmeras sem stream ativo)
- Timeline (preview ao hover)
- Listagem de câmeras (coluna de preview)
- E-mails de alerta (último frame antes do incidente)

---

## 7. CHMS — Detecção de Câmera Offline {#chms}

O Camera Health Monitoring System usa a API REST do MediaMTX para verificar o status de cada câmera.

### 7.1 Polling a cada 30 segundos

```java
// CameraHealthService.java
@Scheduled(fixedDelay = 30_000)  // 30 segundos
public void checkCameraHealth() {
    // 1. Buscar todos os paths ativos no MediaMTX
    List<String> activePaths = mediaServerClient.getActivePaths();
    // GET {MEDIAMTX_API_URL}/v3/paths/list → retorna paths com readers > 0

    // 2. Para cada câmera ONLINE no banco, verificar se tem path ativo
    List<Camera> onlineCameras = cameraRepository.findByStatus(CameraStatus.ONLINE);

    for (Camera camera : onlineCameras) {
        String expectedPath = "tenant_" + camera.getTenantId()
                            + "/camera_" + camera.getId() + "/main";

        boolean isActive = activePaths.contains(expectedPath);

        if (!isActive) {
            // Câmera saiu do ar
            handleCameraOffline(camera);
        }
    }

    // 3. Para câmeras OFFLINE, verificar se voltaram
    List<Camera> offlineCameras = cameraRepository.findByStatus(CameraStatus.OFFLINE);
    for (Camera camera : offlineCameras) {
        // verificar se o path voltou...
        handleCameraRestored(camera);
    }
}
```

### 7.2 API MediaMTX usada pelo CHMS

```
GET {MEDIAMTX_API_URL}/v3/paths/list

Response:
{
  "items": [
    {
      "name": "tenant_abc123/camera_xyz789/main",
      "source": { "type": "rtspSession", "id": "..." },
      "readers": [{ "type": "webrtcSession" }]
    }
  ]
}
```

Ver variável `MEDIAMTX_API_URL` em [ENV_CONFIG.md](./ENV_CONFIG.md).

---

## 8. Recording Confidence Score {#confidence-score}

Mede a saúde da gravação. Calculado diariamente pelo CHMS:

```
Confidence Score = (tempo_gravado / tempo_esperado) × 100

Onde:
  tempo_esperado = 24 horas (ou período de monitoramento configurado)
  tempo_gravado  = soma de duration_seconds de todos os recordings
                   da câmera nas últimas 24h

Exemplo:
  Uma câmera gravou 23h de 24h esperadas → Score = 95.8%
  Uma câmera ficou offline 4h → Score = 83.3%
```

Câmeras com `score < 90%` geram um `health_event` do tipo `LOW_CONFIDENCE`. Ver [DATA_MODEL.md#health_events](./DATA_MODEL.md#health_events).

---

## 9. Referências Cruzadas

- Diagrama de arquitetura: [ARCHITECTURE.md#mediamtx](./ARCHITECTURE.md#mediamtx)
- Eventos de saúde gerados: [DATA_MODEL.md#health_events](./DATA_MODEL.md#health_events)
- Variáveis de ambiente do MediaMTX: [ENV_CONFIG.md](./ENV_CONFIG.md)
- Autenticação de streams por tenant: [MULTI_TENANCY.md#isolamento-midia](./MULTI_TENANCY.md#isolamento-midia)
- Endpoints de stream: [API_CONTRACTS.md#cameras-stream-live](./API_CONTRACTS.md#cameras-stream-live)
