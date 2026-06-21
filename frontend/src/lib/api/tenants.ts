import { apiClient } from './client';
import type { Tenant } from '@/types';

export interface TenantConfig {
  id: string;
  slug: string;
  name: string;
  domain: string | null;
  plan: string | null;
  logo_url: string | null;
  css_override: string | null;
  max_cameras: number;
  max_users: number;
  retention_days: number;
  status: string;
}

export const tenantsApi = {
  getCurrent: () => apiClient.get<Tenant>('/tenants/me'),

  getConfigByDomain: (domain: string) =>
    apiClient.get<TenantConfig>(`/tenants/config?domain=${domain}`),

  getConfigBySlug: (slug: string) =>
    apiClient.get<TenantConfig>(`/tenants/config?slug=${slug}`),
};
