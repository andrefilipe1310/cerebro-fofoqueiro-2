'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth.store';

interface TenantSettings { name: string; domain: string; logo_url: string | null; css_override: string | null; }
interface UserRecord { id: string; email: string; role: string; active: boolean; }

type Tab = 'tenant' | 'users' | 'security';

export default function SettingsPage() {
  const [tab, setTab] = useState<Tab>('tenant');
  const qc = useQueryClient();
  const user = useAuthStore((s) => s.user);

  const { data: tenant } = useQuery<TenantSettings>({
    queryKey: ['tenant-settings'],
    queryFn: () => apiClient.get('/tenants/me').then(r => r.data),
  });

  const { data: usersData } = useQuery<{ content: UserRecord[] }>({
    queryKey: ['users'],
    queryFn: () => apiClient.get('/users').then(r => r.data),
    enabled: tab === 'users',
  });

  const updateTenant = useMutation({
    mutationFn: (payload: Partial<TenantSettings>) => apiClient.put('/tenants/me', payload).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tenant-settings'] }),
  });

  const [tenantForm, setTenantForm] = useState({ name: '', logo_url: '', css_override: '' });

  const handleTenantSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateTenant.mutate({ name: tenantForm.name || undefined, logo_url: tenantForm.logo_url || null, css_override: tenantForm.css_override || null });
  };

  const isAdmin = user?.role === 'ADMIN';

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Configurações</h1>

      <div className="flex gap-1 border rounded-md p-1 mb-6 w-fit">
        {(['tenant', 'users', 'security'] as Tab[]).map((t) => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-4 py-1.5 rounded text-sm font-medium capitalize transition-colors ${tab === t ? 'bg-primary text-primary-foreground' : 'hover:bg-accent'}`}>
            {t === 'tenant' ? 'Organização' : t === 'users' ? 'Usuários' : 'Segurança'}
          </button>
        ))}
      </div>

      {tab === 'tenant' && (
        <div className="max-w-lg">
          <form onSubmit={handleTenantSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Nome da Organização</label>
              <input
                value={tenantForm.name || tenant?.name || ''}
                onChange={e => setTenantForm(f => ({ ...f, name: e.target.value }))}
                placeholder={tenant?.name ?? ''}
                className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">URL do Logo</label>
              <input
                value={tenantForm.logo_url || tenant?.logo_url || ''}
                onChange={e => setTenantForm(f => ({ ...f, logo_url: e.target.value }))}
                placeholder="https://exemplo.com/logo.png"
                className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">CSS Personalizado</label>
              <textarea
                value={tenantForm.css_override || tenant?.css_override || ''}
                onChange={e => setTenantForm(f => ({ ...f, css_override: e.target.value }))}
                placeholder=":root { --primary: 220 100% 50%; }"
                rows={4}
                className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring font-mono"
              />
            </div>
            {isAdmin && (
              <button type="submit" disabled={updateTenant.isPending}
                className="px-4 py-2 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 disabled:opacity-50">
                {updateTenant.isPending ? 'Salvando...' : 'Salvar Alterações'}
              </button>
            )}
          </form>
        </div>
      )}

      {tab === 'users' && (
        <div>
          <div className="rounded-md border overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-muted/50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Email</th>
                  <th className="px-4 py-3 text-left font-medium">Perfil</th>
                  <th className="px-4 py-3 text-left font-medium">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {(usersData?.content ?? []).map((u) => (
                  <tr key={u.id} className="hover:bg-muted/25">
                    <td className="px-4 py-3">{u.email}</td>
                    <td className="px-4 py-3">
                      <span className="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-muted">{u.role}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${u.active ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {u.active ? 'Ativo' : 'Inativo'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'security' && (
        <div className="max-w-lg space-y-6">
          <div className="rounded-lg border p-4">
            <h3 className="font-medium mb-2">Autenticação em Dois Fatores</h3>
            <p className="text-sm text-muted-foreground mb-3">
              Configure o TOTP para aumentar a segurança da sua conta.
            </p>
            <button
              onClick={async () => { const { data } = await apiClient.post('/auth/2fa/setup'); alert(`Secret: ${data.secret}\nQR: ${data.qr_code_url}`); }}
              className="px-4 py-2 rounded-md border text-sm font-medium hover:bg-accent">
              Configurar 2FA
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
