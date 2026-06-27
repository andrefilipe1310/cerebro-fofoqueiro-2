// @path frontend/next.config.js
// @owner frontend
// @responsibility Configuração do Next.js 14 App Router
// @see docs/TECH_STACK.md#frontend

/** @type {import('next').NextConfig} */
const nextConfig = {
  // standalone apenas no Docker — cria symlinks que o Windows bloqueia sem Developer Mode
  ...(process.env.NEXT_BUILD_STANDALONE === 'true' ? { output: 'standalone' } : {}),
  reactStrictMode: true,
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: '**.r2.dev',  // Cloudflare R2 para thumbnails
      },
      {
        protocol: 'http',
        hostname: 'localhost',  // MinIO em dev
      },
    ],
  },
};

module.exports = nextConfig;
