// @path frontend/src/stores/ui.store.ts
// @owner frontend
// @responsibility Estado de UI (Zustand) — sidebar, tema
// @see docs/CODE_STYLE.md#padroes-obrigatorios (Zustand apenas para UI state)
import { create } from 'zustand';

interface UiStore {
  sidebarOpen: boolean;
  theme: 'light' | 'dark' | 'system';
  setSidebarOpen: (open: boolean) => void;
  toggleSidebar: () => void;
  setTheme: (theme: 'light' | 'dark' | 'system') => void;
}

export const useUiStore = create<UiStore>((set) => ({
  sidebarOpen: true,
  theme: 'system',
  setSidebarOpen: (open) => set({ sidebarOpen: open }),
  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  setTheme: (theme) => set({ theme }),
}));
