// @path frontend/src/app/(dashboard)/layout.tsx
// @owner frontend
// @responsibility Layout principal do dashboard — sidebar + header
// @see docs/CODE_STYLE.md#estrutura-frontend
export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-screen bg-background">
      {/* Sidebar placeholder — implementação completa na Fase 5 */}
      <aside className="w-64 border-r bg-card flex flex-col">
        <div className="p-4 border-b">
          <span className="font-bold text-lg">Fofoqueiro</span>
        </div>
        <nav className="flex-1 p-4 space-y-1">
          <a href="/cameras" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm hover:bg-accent">
            Câmeras
          </a>
          <a href="/mosaic" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm hover:bg-accent">
            Mosaico
          </a>
          <a href="/map" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm hover:bg-accent">
            Mapa
          </a>
          <a href="/alerts" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm hover:bg-accent">
            Alertas
          </a>
          <a href="/recordings" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm hover:bg-accent">
            Gravações
          </a>
          <a href="/settings" className="flex items-center gap-2 px-3 py-2 rounded-md text-sm hover:bg-accent">
            Configurações
          </a>
        </nav>
      </aside>
      {/* Conteúdo principal */}
      <main className="flex-1 overflow-auto">
        <header className="border-b bg-card px-6 py-4">
          <p className="text-sm text-muted-foreground">Dashboard</p>
        </header>
        <div className="p-6">{children}</div>
      </main>
    </div>
  );
}
