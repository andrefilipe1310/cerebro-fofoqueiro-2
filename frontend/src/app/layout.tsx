// @path frontend/src/app/layout.tsx
// @owner frontend
// @responsibility Root layout — providers globais, fontes, metadata
// @see docs/CODE_STYLE.md#estrutura-frontend
import type { Metadata } from 'next';
import { Space_Grotesk } from 'next/font/google';
import './globals.css';
import { Providers } from './providers';

const spaceGrotesk = Space_Grotesk({
  subsets: ['latin'],
  variable: '--font-space-grotesk',
});

export const metadata: Metadata = {
  title: 'Fofoqueiro — Monitoramento',
  description: 'Plataforma SaaS de videomonitoramento multi-organização',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" suppressHydrationWarning className={spaceGrotesk.variable}>
      <body className={spaceGrotesk.className}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
