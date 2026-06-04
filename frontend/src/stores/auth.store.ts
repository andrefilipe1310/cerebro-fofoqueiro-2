// @path frontend/src/stores/auth.store.ts
// @owner frontend
// @responsibility Estado de autenticação (Zustand) — user, tenant, tokens
// @see docs/CODE_STYLE.md#padroes-obrigatorios (Zustand apenas para estado de UI)
// @see docs/TECH_STACK.md#frontend
import { create } from 'zustand';
import type { User, Tenant } from '@/types';

interface AuthStore {
  user: User | null;
  tenant: Tenant | null;
  isAuthenticated: boolean;
  setUser: (user: User, tenant: Tenant) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  tenant: null,
  isAuthenticated: false,
  setUser: (user, tenant) => set({ user, tenant, isAuthenticated: true }),
  clearAuth: () => set({ user: null, tenant: null, isAuthenticated: false }),
}));
