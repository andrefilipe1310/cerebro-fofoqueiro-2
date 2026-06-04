// @path frontend/src/lib/hooks/useCameras.ts
// @owner frontend
// @responsibility Hook de câmeras — lista e detalhe com React Query
// @see docs/CODE_STYLE.md#padroes-obrigatorios | docs/API_CONTRACTS.md#cameras-list
'use client';
import { useQuery } from '@tanstack/react-query';
import { camerasApi } from '@/lib/api/cameras';

export function useCameras(params?: { status?: string; location_id?: string }) {
  return useQuery({
    queryKey: ['cameras', params],
    queryFn: () => camerasApi.list(params).then((r) => r.data),
    staleTime: 30_000,
  });
}

export function useCamera(id: string) {
  return useQuery({
    queryKey: ['cameras', id],
    queryFn: () => camerasApi.get(id).then((r) => r.data),
    staleTime: 30_000,
    enabled: !!id,
  });
}

export function useLiveUrl(cameraId: string) {
  return useQuery({
    queryKey: ['cameras', cameraId, 'live'],
    queryFn: () => camerasApi.getLiveUrl(cameraId).then((r) => r.data),
    staleTime: 50 * 60 * 1000,  // 50 minutos (token expira em 1h)
    enabled: !!cameraId,
  });
}
