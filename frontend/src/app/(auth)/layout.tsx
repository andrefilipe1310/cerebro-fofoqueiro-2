// @path frontend/src/app/(auth)/layout.tsx
// @owner frontend
// @responsibility Layout limpo para páginas de autenticação (sem sidebar)
export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="w-full max-w-md">{children}</div>
    </div>
  );
}
