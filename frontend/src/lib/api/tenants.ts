// @path frontend/src/lib/api/tenants.ts
// @owner frontend
// @responsibility Funções de tenant — config, white-label
// @see docs/API_CONTRACTS.md#tenant-endpoints
import { apiClient } from './client';
import type { Tenant } from '@/types';

export const tenantsApi = {
  getCurrent: () =>
    apiClient.get<Tenant>('/tenants/current'),

  getConfigByDomain: (domain: string) =>
    apiClient.get<{ logo_url: string | null; css_override: string | null; name: string }>(
      `/tenants/config?domain=${domain}`
    ),
};
