// @path frontend/src/stores/auth.store.ts
// @owner frontend
// @responsibility Estado de autenticação (Zustand) — user, org, tokens
import { create } from 'zustand';
import type { User, Org } from '@/types';

interface AuthStore {
  user: User | null;
  org: Org | null;
  isAuthenticated: boolean;
  setUser: (user: User, org: Org) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  org: null,
  isAuthenticated: false,
  setUser: (user, org) => set({ user, org, isAuthenticated: true }),
  clearAuth: () => set({ user: null, org: null, isAuthenticated: false }),
}));
