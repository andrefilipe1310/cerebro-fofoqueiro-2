// @path frontend/src/app/(auth)/2fa/page.tsx
// @owner frontend
// @responsibility Página de verificação 2FA — implementação completa na Fase 2
// @see docs/API_CONTRACTS.md#auth-2fa
export default function TwoFactorPage() {
  return (
    <div className="p-8 rounded-lg border bg-card shadow-sm">
      <h1 className="text-2xl font-semibold mb-6">Verificação 2FA</h1>
      <p className="text-muted-foreground text-sm">
        Verificação TOTP — implementação na Fase 2
      </p>
    </div>
  );
}
