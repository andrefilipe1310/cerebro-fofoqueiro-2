'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { Camera, Grid2x2, Map, Bell, Settings, Video, LogOut } from 'lucide-react';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/stores/auth.store';

const navItems = [
  { href: '/cameras', label: 'Câmeras', icon: Camera },
  { href: '/mosaic', label: 'Mosaico', icon: Grid2x2 },
  { href: '/map', label: 'Mapa', icon: Map },
  { href: '/alerts', label: 'Alertas', icon: Bell },
  { href: '/recordings', label: 'Gravações', icon: Video },
  { href: '/settings', label: 'Configurações', icon: Settings },
];

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, clearAuth } = useAuthStore();

  const handleLogout = async () => {
    try { await authApi.logout(); } catch {}
    sessionStorage.clear();
    document.cookie = 'access_token=; path=/; max-age=0';
    clearAuth();
    router.push('/login');
  };

  return (
    <div className="flex h-screen bg-background">
      <aside className="w-64 border-r bg-card flex flex-col">
        <div className="p-4 border-b">
          <span className="font-bold text-lg">Fofoqueiro</span>
        </div>
        <nav className="flex-1 p-3 space-y-1">
          {navItems.map(({ href, label, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors ${
                pathname === href
                  ? 'bg-accent text-accent-foreground font-medium'
                  : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
              }`}
            >
              <Icon className="h-4 w-4" />
              {label}
            </Link>
          ))}
        </nav>
        <div className="p-3 border-t">
          <div className="flex items-center gap-2 px-3 py-2 text-sm text-muted-foreground">
            <span className="flex-1 truncate">{user?.email ?? 'Usuário'}</span>
            <button onClick={handleLogout} title="Sair" className="hover:text-foreground">
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>
      <main className="flex-1 overflow-auto">
        <div className="p-6">{children}</div>
      </main>
    </div>
  );
}
