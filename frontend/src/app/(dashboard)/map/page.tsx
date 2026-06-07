'use client';

import 'leaflet/dist/leaflet.css';
import { useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api/client';
import type { Camera } from '@/types';

export default function MapPage() {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<unknown>(null);

  const { data } = useQuery<{ content: Camera[] }>({
    queryKey: ['cameras-map'],
    queryFn: () => apiClient.get('/cameras?size=200').then(r => r.data),
  });

  useEffect(() => {
    if (!mapRef.current || mapInstanceRef.current) return;

    const initMap = async () => {
      const L = (await import('leaflet')).default;

      const container = mapRef.current as HTMLDivElement & { _leaflet_id?: number };
      delete container._leaflet_id;
      const map = L.map(container).setView([-15.78, -47.93], 5);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
      }).addTo(map);
      mapInstanceRef.current = map;
    };
    initMap();

    return () => {
      if (mapInstanceRef.current) {
        (mapInstanceRef.current as { remove: () => void }).remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!mapInstanceRef.current || !data?.content) return;

    const addMarkers = async () => {
      const L = (await import('leaflet')).default;
      const map = mapInstanceRef.current as { addLayer: (l: unknown) => void };

      data.content
        .filter((cam) => cam.lat != null && cam.lng != null)
        .forEach((cam) => {
          const icon = L.divIcon({
            html: `<div style="width:14px;height:14px;border-radius:50%;background:${cam.status === 'ONLINE' ? '#22c55e' : '#ef4444'};border:2px solid white;box-shadow:0 0 4px rgba(0,0,0,0.3)"></div>`,
            className: '',
            iconSize: [14, 14],
            iconAnchor: [7, 7],
          });
          L.marker([cam.lat!, cam.lng!], { icon })
            .bindPopup(`<b>${cam.name}</b><br>Status: <b style="color:${cam.status === 'ONLINE' ? 'green' : 'red'}">${cam.status}</b>`)
            .addTo(map as unknown as L.Map);
        });
    };
    addMarkers();
  }, [data]);

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-4">Mapa de Câmeras</h1>
      <div ref={mapRef} className="w-full rounded-xl border overflow-hidden" style={{ height: '600px' }} />
    </div>
  );
}
