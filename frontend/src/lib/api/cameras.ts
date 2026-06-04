// @path frontend/src/lib/api/cameras.ts
// @owner frontend
// @responsibility Funções de câmeras — CRUD, stream URL, health, timeline
// @see docs/API_CONTRACTS.md#camera-endpoints
import { apiClient } from './client';
import type { Camera, CameraDetail, PaginatedResponse, StreamUrlResponse, Timeline } from '@/types';

export const camerasApi = {
  list: (params?: { status?: string; location_id?: string; limit?: number; cursor?: string }) =>
    apiClient.get<PaginatedResponse<Camera>>('/cameras', { params }),

  get: (id: string) =>
    apiClient.get<CameraDetail>(`/cameras/${id}`),

  getLiveUrl: (id: string) =>
    apiClient.get<StreamUrlResponse>(`/cameras/${id}/stream/live`),

  getTimeline: (id: string, from: string, to: string) =>
    apiClient.get<Timeline>(`/cameras/${id}/timeline`, { params: { from, to } }),
};
