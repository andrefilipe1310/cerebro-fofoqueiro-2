'use client';

import { useState, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api/client';
import type { Camera } from '@/types';
import Hls from 'hls.js';

type GridSize = '1x1' | '2x2' | '3x3' | '4x4';

interface StreamUrlResponse {
  webrtc_url?: string;
  hls_url?: string;
  expires_at: string;
}

function HlsPlayer({ cameraId, cameraName }: { cameraId: string; cameraName: string }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);

  useEffect(() => {
    const fetchAndPlay = async () => {
      try {
        const { data } = await apiClient.get<StreamUrlResponse>(`/cameras/${cameraId}/stream-url`);
        const url = data.hls_url;
        if (!url || !videoRef.current) return;

        if (Hls.isSupported()) {
          const hls = new Hls({ lowLatencyMode: true });
          hls.loadSource(url);
          hls.attachMedia(videoRef.current);
          hls.on(Hls.Events.MANIFEST_PARSED, () => { videoRef.current?.play().catch(() => {}); });
          hlsRef.current = hls;
        } else if (videoRef.current.canPlayType('application/vnd.apple.mpegurl')) {
          videoRef.current.src = url;
          videoRef.current.play().catch(() => {});
        }
      } catch {}
    };
    fetchAndPlay();
    return () => { hlsRef.current?.destroy(); };
  }, [cameraId]);

  return (
    <div className="relative bg-black rounded-md overflow-hidden aspect-video">
      <video ref={videoRef} muted autoPlay playsInline className="w-full h-full object-contain" />
      <div className="absolute bottom-2 left-2 text-white text-xs bg-black/60 px-2 py-1 rounded">
        {cameraName}
      </div>
    </div>
  );
}

const GRID_CONFIGS: Record<GridSize, { cols: string; maxCameras: number }> = {
  '1x1': { cols: 'grid-cols-1', maxCameras: 1 },
  '2x2': { cols: 'grid-cols-2', maxCameras: 4 },
  '3x3': { cols: 'grid-cols-3', maxCameras: 9 },
  '4x4': { cols: 'grid-cols-4', maxCameras: 16 },
};

export default function MosaicPage() {
  const [gridSize, setGridSize] = useState<GridSize>('2x2');

  const { data } = useQuery<{ content: Camera[] }>({
    queryKey: ['cameras-mosaic'],
    queryFn: () => apiClient.get('/cameras?size=16').then(r => r.data),
    refetchInterval: 30_000,
  });

  const cameras = (data?.content ?? []).slice(0, GRID_CONFIGS[gridSize].maxCameras);

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold">Mosaico</h1>
        <div className="flex gap-1 border rounded-md p-1">
          {(['1x1', '2x2', '3x3', '4x4'] as GridSize[]).map((s) => (
            <button key={s} onClick={() => setGridSize(s)}
              className={`px-3 py-1 rounded text-sm font-medium transition-colors ${gridSize === s ? 'bg-primary text-primary-foreground' : 'hover:bg-accent'}`}>
              {s}
            </button>
          ))}
        </div>
      </div>

      {cameras.length === 0 ? (
        <div className="text-center py-16 text-muted-foreground">
          <p>Nenhuma câmera disponível.</p>
        </div>
      ) : (
        <div className={`grid ${GRID_CONFIGS[gridSize].cols} gap-2`}>
          {cameras.map((cam) => (
            <HlsPlayer key={cam.id} cameraId={cam.id} cameraName={cam.name} />
          ))}
        </div>
      )}
    </div>
  );
}
