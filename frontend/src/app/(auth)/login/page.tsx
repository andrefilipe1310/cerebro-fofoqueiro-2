'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { tenantsApi } from '@/lib/api/tenants';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/stores/auth.store';

const schema = z.object({
  tenantSlug: z.string().min(1, 'Informe o slug do tenant'),
  email: z.string().email('Email inválido'),
  password: z.string().min(1, 'Informe a senha'),
});

type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const setUser = useAuthStore((s) => s.setUser);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    setError('');
    setLoading(true);
    try {
      const { data: tenantConfig } = await tenantsApi.getConfigBySlug(data.tenantSlug);
      const { data: authData } = await authApi.login(tenantConfig.id, data.email, data.password);

      sessionStorage.setItem('access_token', authData.access_token);
      if (authData.refresh_token) {
        sessionStorage.setItem('refresh_token', authData.refresh_token);
      }
      sessionStorage.setItem('tenant_id', tenantConfig.id);
      document.cookie = `access_token=${authData.access_token}; path=/; SameSite=Lax`;

      if (authData.requires_2fa) {
        sessionStorage.setItem('temp_token', authData.access_token);
        router.push('/2fa');
        return;
      }

      if (authData.user) {
        setUser(authData.user, { ...tenantConfig, plan: 'STARTER', limits: { cameras_max: 10, cameras_current: 0, users_max: 5, retention_days: 30 } });
      }

      router.push('/cameras');
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e?.response?.data?.message ?? 'Credenciais inválidas');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md p-8 rounded-xl border border-[#2A2D3E] bg-[#1E2130] shadow-2xl">
      <div className="mb-8 text-center">
        <h1 className="text-3xl font-bold tracking-widest uppercase text-[#E8A020]">Fofoqueiro</h1>
        <p className="text-[#E8A020]/70 text-sm mt-1 italic">Tudo em foco. Nada escapa.</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Organização</label>
          <input
            {...register('tenantSlug')}
            placeholder="minha-empresa"
            className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
          {errors.tenantSlug && <p className="text-destructive text-xs mt-1">{errors.tenantSlug.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">Email</label>
          <input
            {...register('email')}
            type="email"
            placeholder="usuario@empresa.com"
            className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
          {errors.email && <p className="text-destructive text-xs mt-1">{errors.email.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">Senha</label>
          <input
            {...register('password')}
            type="password"
            placeholder="••••••••"
            className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
          {errors.password && <p className="text-destructive text-xs mt-1">{errors.password.message}</p>}
        </div>

        {error && (
          <div className="rounded-md bg-destructive/10 border border-destructive/20 px-3 py-2 text-sm text-destructive">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2 px-4 rounded-md bg-[#E8A020] text-[#0C0E13] font-semibold text-sm hover:bg-[#E8A020]/90 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </div>
  );
}
