// @path frontend/src/lib/api/auth.ts
// @owner frontend
// @responsibility Funções de autenticação — login, 2FA, refresh, logout
// @see docs/API_CONTRACTS.md#auth-endpoints
import { apiClient } from './client';
import type { LoginResponse } from '@/types';

export const authApi = {
  login: (email: string, password: string, tenantSlug: string) =>
    apiClient.post<LoginResponse>('/auth/login', { email, password, tenant_slug: tenantSlug }),

  verify2fa: (code: string) =>
    apiClient.post<LoginResponse>('/auth/2fa/verify', { code }),

  setup2fa: () =>
    apiClient.post<{ qr_code_url: string; secret: string; backup_codes: string[] }>('/auth/2fa/setup'),

  refresh: (refreshToken: string) =>
    apiClient.post<{ access_token: string; expires_in: number }>('/auth/refresh', {
      refresh_token: refreshToken,
    }),

  logout: () => apiClient.post('/auth/logout'),
};
