import { apiClient } from './client';
import type { Org } from '@/types';

export interface OrgConfig {
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

export const orgsApi = {
  getCurrent: () => apiClient.get<Org>('/organizations/me'),

  getConfigByDomain: (domain: string) =>
    apiClient.get<OrgConfig>(`/organizations/config?domain=${domain}`),

  getConfigBySlug: (slug: string) =>
    apiClient.get<OrgConfig>(`/organizations/config?slug=${slug}`),
};
