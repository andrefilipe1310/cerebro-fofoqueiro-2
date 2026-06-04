// @path frontend/src/lib/hooks/useAuth.ts
// @owner frontend
// @responsibility Hook de autenticação — wraps authApi com React Query
// @see docs/CODE_STYLE.md#padroes-obrigatorios (React Query para todo fetch)
'use client';
import { useMutation } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/auth.store';
import { authApi } from '@/lib/api/auth';

export function useLogin() {
  const setUser = useAuthStore((s) => s.setUser);

  return useMutation({
    mutationFn: ({ email, password, tenantSlug }: { email: string; password: string; tenantSlug: string }) =>
      authApi.login(email, password, tenantSlug),
    onSuccess: ({ data }) => {
      if (!data.requires_2fa) {
        sessionStorage.setItem('access_token', data.access_token);
        sessionStorage.setItem('refresh_token', data.refresh_token);
      }
    },
  });
}

export function useLogout() {
  const clearAuth = useAuthStore((s) => s.clearAuth);

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSuccess: () => {
      sessionStorage.clear();
      clearAuth();
    },
  });
}
