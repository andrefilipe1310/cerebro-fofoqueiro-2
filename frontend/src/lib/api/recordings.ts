// @path frontend/src/lib/api/recordings.ts
// @owner frontend
// @responsibility Funções de gravações — timeline, download URL
// @see docs/API_CONTRACTS.md#cameras-recordings
import { apiClient } from './client';
import type { PaginatedResponse, Recording } from '@/types';

export const recordingsApi = {
  listByCamera: (
    cameraId: string,
    params?: { from?: string; to?: string; limit?: number; cursor?: string }
  ) => apiClient.get<PaginatedResponse<Recording>>(`/cameras/${cameraId}/recordings`, { params }),

  getDownloadUrl: (cameraId: string, recordingId: string) =>
    apiClient.get<{ download_url: string; expires_at: string; filename: string }>(
      `/cameras/${cameraId}/recordings/${recordingId}/download`
    ),
};
