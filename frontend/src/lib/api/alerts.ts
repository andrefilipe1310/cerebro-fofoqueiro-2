// @path frontend/src/lib/api/alerts.ts
// @owner frontend
// @responsibility Funções de alertas — listar, acknowledger
// @see docs/API_CONTRACTS.md#alert-endpoints
import { apiClient } from './client';
import type { Alert, PaginatedResponse } from '@/types';

export const alertsApi = {
  list: (params?: { acknowledged?: boolean; camera_id?: string; limit?: number; cursor?: string }) =>
    apiClient.get<PaginatedResponse<Alert>>('/alerts', { params }),

  acknowledge: (id: string) =>
    apiClient.patch<{ id: string; acknowledged_at: string }>(`/alerts/${id}/acknowledge`),
};
