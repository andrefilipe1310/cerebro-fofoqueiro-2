'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api/auth';
import { orgsApi } from '@/lib/api/orgs';
import { useAuthStore } from '@/stores/auth.store';
import type { OrgOption, UserRole, OrgPlan } from '@/types';

interface MeResponse {
  id: string;
  email: string;
  role: string;
  totp_enabled: boolean;
}

function Initials({ name }: { name: string }) {
  const parts = name.trim().split(/\s+/);
  const letters = parts.length >= 2
    ? parts[0][0] + parts[parts.length - 1][0]
    : name.slice(0, 2);
  return (
    <span className="text-3xl font-bold text-[#E8A020] uppercase">{letters}</span>
  );
}

const ROLE_LABEL: Record<string, string> = {
  ADMIN: 'Administrador',
  OPERATOR: 'Operador',
  VIEWER: 'Visualizador',
};

export default function WorkspacePage() {
  const router = useRouter();
  const setUser = useAuthStore((s) => s.setUser);

  const [loading, setLoading] = useState(true);
  const [entering, setEntering] = useState<string | null>(null);
  const [orgs, setOrgs] = useState<OrgOption[]>([]);
  const [me, setMe] = useState<MeResponse | null>(null);
  const [tempToken, setTempToken] = useState<string | null>(null);
  const [needsSelection, setNeedsSelection] = useState(false);

  useEffect(() => {
    const storedOrgs = sessionStorage.getItem('pending_orgs');
    const storedTemp = sessionStorage.getItem('pending_temp_token');

    if (storedOrgs && storedTemp) {
      // Usuário tem múltiplas orgs — mostra o picker
      setOrgs(JSON.parse(storedOrgs));
      setTempToken(storedTemp);
      setNeedsSelection(true);
      setLoading(false);
      return;
    }

    // Usuário já tem token scoped (1 org ou veio do 2FA com auto-seleção)
    Promise.all([
      authApi.me().then((r) => r.data as MeResponse),
      orgsApi.getCurrent().then((r) => r.data),
    ])
      .then(([user, org]) => {
        setMe(user);
        setUser(
          { id: user.id, email: user.email, role: user.role as UserRole, totp_enabled: user.totp_enabled },
          {
            id: org.id, name: org.name, slug: org.slug,
            plan: (org as any).plan as OrgPlan,
            logo_url: (org as any).logo_url ?? null,
            limits: {
              cameras_max: (org as any).max_cameras ?? 10,
              cameras_current: 0,
              users_max: (org as any).max_users ?? 5,
              retention_days: (org as any).retention_days ?? 30,
            },
          }
        );
        router.replace('/cameras');
      })
      .catch(() => router.replace('/login'));
  }, [router, setUser]);

  const handleSelectOrg = async (opt: OrgOption) => {
    if (!tempToken || entering) return;
    setEntering(opt.id);
    try {
      const { data: authData } = await authApi.selectOrg(tempToken, opt.id);

      sessionStorage.setItem('access_token', authData.access_token);
      if (authData.refresh_token) sessionStorage.setItem('refresh_token', authData.refresh_token);
      document.cookie = `access_token=${authData.access_token}; path=/; SameSite=Lax`;
      sessionStorage.removeItem('pending_orgs');
      sessionStorage.removeItem('pending_temp_token');

      // Busca dados completos da org para hidratar o store
      const orgData = await orgsApi.getCurrent().then((r) => r.data);
      const meData = await authApi.me().then((r) => r.data as MeResponse);

      setUser(
        { id: meData.id, email: meData.email, role: opt.role as UserRole, totp_enabled: meData.totp_enabled },
        {
          id: orgData.id, name: orgData.name, slug: orgData.slug,
          plan: (orgData as any).plan as OrgPlan,
          logo_url: (orgData as any).logo_url ?? null,
          limits: {
            cameras_max: (orgData as any).max_cameras ?? 10,
            cameras_current: 0,
            users_max: (orgData as any).max_users ?? 5,
            retention_days: (orgData as any).retention_days ?? 30,
          },
        }
      );

      router.push('/cameras');
    } catch {
      setEntering(null);
      alert('Erro ao entrar na organização. Tente novamente.');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <div className="h-8 w-8 rounded-full border-2 border-[#E8A020] border-t-transparent animate-spin" />
      </div>
    );
  }

  return (
    <div className="w-full max-w-md px-4">
      <div className="mb-10 text-center">
        <h1 className="text-3xl font-bold tracking-widest uppercase text-[#E8A020]">Fofoqueiro</h1>
        <p className="text-[#E8A020]/70 text-sm mt-1 italic">Tudo em foco. Nada escapa.</p>
      </div>

      <p className="text-center text-sm text-muted-foreground mb-6">
        {orgs.length > 1 ? 'Selecione sua organização' : 'Selecione seu grupo'}
      </p>

      <div className="space-y-3">
        {orgs.map((opt) => (
          <button
            key={opt.id}
            onClick={() => handleSelectOrg(opt)}
            disabled={!!entering}
            className="w-full group flex items-center gap-4 p-5 rounded-2xl border border-[#2A2D3E] bg-[#1E2130] hover:border-[#E8A020]/50 hover:bg-[#252840] transition-all duration-200 disabled:opacity-60 disabled:cursor-not-allowed shadow-lg text-left"
          >
            {/* Logo ou iniciais */}
            <div className="w-14 h-14 rounded-xl bg-[#2A2D3E] group-hover:bg-[#333654] flex items-center justify-center transition-colors shrink-0 overflow-hidden">
              {opt.logo_url ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={opt.logo_url} alt={opt.name} className="w-full h-full object-cover" />
              ) : (
                <Initials name={opt.name} />
              )}
            </div>

            <div className="flex-1 min-w-0">
              <p className="font-semibold truncate">{opt.name}</p>
              <span className="inline-flex mt-1 px-2 py-0.5 rounded-full text-xs font-medium bg-[#E8A020]/15 text-[#E8A020]">
                {ROLE_LABEL[opt.role] ?? opt.role}
              </span>
            </div>

            <div className="text-muted-foreground text-xs shrink-0">
              {entering === opt.id ? 'Entrando...' : '→'}
            </div>
          </button>
        ))}
      </div>

      <div className="mt-6 text-center">
        <button
          onClick={() => {
            sessionStorage.clear();
            document.cookie = 'access_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
            router.push('/login');
          }}
          className="text-xs text-muted-foreground hover:text-foreground transition-colors"
        >
          Usar outra conta
        </button>
      </div>
    </div>
  );
}
