// @path frontend/src/middleware.ts
// @owner frontend
// @responsibility Proteção de rotas — redireciona para /login se não autenticado
// @see docs/SECURITY_LGPD.md#autenticacao
import { NextRequest, NextResponse } from 'next/server';

const PUBLIC_PATHS = ['/login', '/2fa', '/first-access'];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const isPublicPath = PUBLIC_PATHS.some((p) => pathname.startsWith(p));

  if (isPublicPath) {
    return NextResponse.next();
  }

  // Em produção (Fase 2+): verificar JWT via cookie HttpOnly
  // Por ora (Fase 1), apenas estrutura de roteamento
  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|api/).*)'],
};
