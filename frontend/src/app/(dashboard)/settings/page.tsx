'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, X, QrCode, Shield } from 'lucide-react';
import { apiClient } from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth.store';

interface TenantSettings { name: string; domain: string; logo_url: string | null; css_override: string | null; }
interface UserRecord { id: string; email: string; role: string; active: boolean; }

type Tab = 'tenant' | 'users' | 'security';

interface NewUserForm { email: string; password: string; role: string; }

interface TotpSetupData { secret: string; qr_code_url: string; }

export default function SettingsPage() {
  const [tab, setTab] = useState<Tab>('tenant');
  const [showNewUserModal, setShowNewUserModal] = useState(false);
  const [newUserForm, setNewUserForm] = useState<NewUserForm>({ email: '', password: '', role: 'OPERATOR' });
  const [totpSetup, setTotpSetup] = useState<TotpSetupData | null>(null);
  const qc = useQueryClient();
  const user = useAuthStore((s) => s.user);

  const { data: tenant } = useQuery<TenantSettings>({
    queryKey: ['tenant-settings'],
    queryFn: () => apiClient.get('/tenants/me').then(r => r.data),
  });

  const { data: usersData, isLoading: loadingUsers } = useQuery<{ content: UserRecord[] }>({
    queryKey: ['users'],
    queryFn: () => apiClient.get('/users').then(r => r.data),
    enabled: tab === 'users',
  });

  const updateTenant = useMutation({
    mutationFn: (payload: Partial<TenantSettings>) => apiClient.put('/tenants/me', payload).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tenant-settings'] }),
  });

  const createUser = useMutation({
    mutationFn: (payload: NewUserForm) => apiClient.post('/users', payload).then(r => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] });
      setShowNewUserModal(false);
      setNewUserForm({ email: '', password: '', role: 'OPERATOR' });
    },
  });

  const [tenantForm, setTenantForm] = useState({ name: '', logo_url: '', css_override: '' });

  const handleTenantSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateTenant.mutate({ name: tenantForm.name || undefined, logo_url: tenantForm.logo_url || null, css_override: tenantForm.css_override || null });
  };

  const handleSetup2FA = async () => {
    try {
      const { data } = await apiClient.post('/auth/2fa/setup');
      setTotpSetup(data);
    } catch {
      alert('Erro ao configurar 2FA.');
    }
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
          {isAdmin && (
            <div className="flex justify-end mb-4">
              <button
                onClick={() => setShowNewUserModal(true)}
                className="flex items-center gap-2 px-4 py-2 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90"
              >
                <Plus className="h-4 w-4" /> Novo Usuário
              </button>
            </div>
          )}
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
                {loadingUsers ? (
                  <tr><td colSpan={3} className="px-4 py-6 text-center text-muted-foreground">Carregando...</td></tr>
                ) : (usersData?.content ?? []).length === 0 ? (
                  <tr><td colSpan={3} className="px-4 py-6 text-center text-muted-foreground">Nenhum usuário encontrado.</td></tr>
                ) : (
                  (usersData?.content ?? []).map((u) => (
                    <tr key={u.id} className="hover:bg-muted/25">
                      <td className="px-4 py-3">{u.email}</td>
                      <td className="px-4 py-3">
                        <span className="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-muted">{u.role}</span>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${u.active ? 'bg-green-900/40 text-green-400' : 'bg-red-900/40 text-red-400'}`}>
                          {u.active ? 'Ativo' : 'Inativo'}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'security' && (
        <div className="max-w-lg space-y-6">
          <div className="rounded-lg border p-5">
            <div className="flex items-center gap-2 mb-2">
              <Shield className="h-4 w-4 text-primary" />
              <h3 className="font-medium">Autenticação em Dois Fatores</h3>
            </div>
            <p className="text-sm text-muted-foreground mb-4">
              Configure o TOTP (Google Authenticator, Authy) para aumentar a segurança da sua conta.
            </p>
            {totpSetup ? (
              <div className="space-y-3">
                <p className="text-sm text-muted-foreground">Escaneie o QR code com seu aplicativo autenticador:</p>
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={totpSetup.qr_code_url} alt="QR Code 2FA" className="w-48 h-48 rounded-md bg-white p-2" />
                <div className="rounded-md bg-muted p-3">
                  <p className="text-xs text-muted-foreground mb-1">Ou insira o código manualmente:</p>
                  <code className="text-sm font-mono tracking-widest">{totpSetup.secret}</code>
                </div>
                <button onClick={() => setTotpSetup(null)} className="text-xs text-muted-foreground hover:text-foreground">
                  Fechar
                </button>
              </div>
            ) : (
              <button
                onClick={handleSetup2FA}
                className="flex items-center gap-2 px-4 py-2 rounded-md border text-sm font-medium hover:bg-accent"
              >
                <QrCode className="h-4 w-4" /> Configurar 2FA
              </button>
            )}
          </div>
        </div>
      )}

      {showNewUserModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
          <div className="bg-card rounded-xl border shadow-lg w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold">Novo Usuário</h2>
              <button onClick={() => setShowNewUserModal(false)} className="p-1 rounded hover:bg-accent">
                <X className="h-4 w-4" />
              </button>
            </div>
            <form onSubmit={e => { e.preventDefault(); createUser.mutate(newUserForm); }} className="space-y-3">
              <div>
                <label className="block text-sm font-medium mb-1">Email</label>
                <input
                  type="email"
                  value={newUserForm.email}
                  onChange={e => setNewUserForm(f => ({ ...f, email: e.target.value }))}
                  placeholder="usuario@empresa.com"
                  required
                  className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Senha</label>
                <input
                  type="password"
                  value={newUserForm.password}
                  onChange={e => setNewUserForm(f => ({ ...f, password: e.target.value }))}
                  placeholder="••••••••"
                  required
                  minLength={8}
                  className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Perfil</label>
                <select
                  value={newUserForm.role}
                  onChange={e => setNewUserForm(f => ({ ...f, role: e.target.value }))}
                  className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                >
                  <option value="ADMIN">Admin</option>
                  <option value="OPERATOR">Operador</option>
                  <option value="VIEWER">Visualizador</option>
                </select>
              </div>
              {createUser.isError && (
                <p className="text-destructive text-xs">Erro ao criar usuário. Verifique os dados e tente novamente.</p>
              )}
              <div className="flex gap-2 pt-2">
                <button type="button" onClick={() => setShowNewUserModal(false)}
                  className="flex-1 py-2 rounded-md border text-sm hover:bg-accent">Cancelar</button>
                <button type="submit" disabled={createUser.isPending}
                  className="flex-1 py-2 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 disabled:opacity-50">
                  {createUser.isPending ? 'Criando...' : 'Criar Usuário'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
