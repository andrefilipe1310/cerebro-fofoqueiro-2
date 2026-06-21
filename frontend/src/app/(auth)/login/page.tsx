'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { authApi } from '@/lib/api/auth';

const schema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(1, 'Informe a senha'),
});

type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    setError('');
    setLoading(true);
    try {
      const { data: authData } = await authApi.login(data.email, data.password);

      if (authData.requires_2fa) {
        sessionStorage.setItem('temp_token', authData.temp_token ?? '');
        router.push('/2fa');
        return;
      }

      if (authData.requires_org_selection && authData.orgs && authData.orgs.length > 0) {
        // Múltiplas orgs — guarda dados no sessionStorage e vai para o picker
        sessionStorage.setItem('pending_orgs', JSON.stringify(authData.orgs));
        sessionStorage.setItem('pending_temp_token', authData.temp_token ?? '');
        router.push('/workspace');
        return;
      }

      // Org única — auto-selecionada pelo backend, token já é scoped
      sessionStorage.setItem('access_token', authData.access_token);
      if (authData.refresh_token) sessionStorage.setItem('refresh_token', authData.refresh_token);
      document.cookie = `access_token=${authData.access_token}; path=/; SameSite=Lax`;

      router.push('/workspace');
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
