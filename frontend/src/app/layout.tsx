// @path frontend/src/app/layout.tsx
// @owner frontend
// @responsibility Root layout — providers globais, fontes, metadata
// @see docs/CODE_STYLE.md#estrutura-frontend
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import { Providers } from './providers';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'Fofoqueiro — Monitoramento',
  description: 'Plataforma SaaS de videomonitoramento multi-tenant',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body className={inter.className}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
