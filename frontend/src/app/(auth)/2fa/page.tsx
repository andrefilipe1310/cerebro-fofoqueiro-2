'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api/auth';

export default function TwoFactorPage() {
  const router = useRouter();
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const tempToken = sessionStorage.getItem('temp_token') ?? '';
      const { data } = await authApi.verify2fa(tempToken, code);
      sessionStorage.setItem('access_token', data.access_token);
      if (data.refresh_token) sessionStorage.setItem('refresh_token', data.refresh_token);
      sessionStorage.removeItem('temp_token');
      router.push('/cameras');
    } catch {
      setError('Código inválido. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-sm p-8 rounded-xl border bg-card shadow-sm">
      <h1 className="text-2xl font-bold mb-2">Verificação 2FA</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Digite o código de 6 dígitos do seu aplicativo autenticador.
      </p>
      <form onSubmit={handleSubmit} className="space-y-4">
        <input
          value={code}
          onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
          placeholder="000000"
          maxLength={6}
          className="w-full px-3 py-3 rounded-md border bg-background text-center text-2xl tracking-widest font-mono focus:outline-none focus:ring-2 focus:ring-ring"
        />
        {error && <p className="text-destructive text-sm text-center">{error}</p>}
        <button
          type="submit"
          disabled={loading || code.length !== 6}
          className="w-full py-2 px-4 rounded-md bg-primary text-primary-foreground font-medium text-sm hover:bg-primary/90 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Verificando...' : 'Verificar'}
        </button>
        <button type="button" onClick={() => router.push('/login')}
          className="w-full py-2 text-sm text-muted-foreground hover:text-foreground">
          Voltar ao login
        </button>
      </form>
    </div>
  );
}
