import { apiClient } from './client';
import type { LoginResponse } from '@/types';

export const authApi = {
  login: (tenantId: string, email: string, password: string) =>
    apiClient.post<LoginResponse>('/auth/login', { tenant_id: tenantId, email, password }),

  verify2fa: (tempToken: string, totpCode: string) =>
    apiClient.post<LoginResponse>('/auth/2fa/verify', { temp_token: tempToken, totp_code: totpCode }),

  setup2fa: () =>
    apiClient.post<{ qr_code_url: string; secret: string; backup_codes: string[] }>('/auth/2fa/setup'),

  refresh: (refreshToken: string) =>
    apiClient.post<{ access_token: string; expires_in: number }>('/auth/refresh', {
      refresh_token: refreshToken,
    }),

  logout: () => apiClient.post('/auth/logout'),

  me: () => apiClient.get('/auth/me'),
};
