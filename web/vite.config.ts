import os from 'node:os'
import path from 'node:path'
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// Auto-detects this machine's LAN IPv4 address once, at dev-server/build
// start - the same address Vite itself prints as "Network:" when `bun run
// dev` starts (host: '0.0.0.0' below). Baked into the bundle as __LAN_IP__
// so the QR-code login page (see main/config/reachable-origin.ts) can swap
// out `localhost` for something a phone on the same Wi-Fi can actually
// reach, instead of silently generating an unscannable QR code.
function findLanAddress(): string | undefined {
  const virtualAdapterPattern =
    /vEthernet|Loopback|WSL|Docker|VirtualBox|VMware|Tailscale/i

  for (const [name, addresses] of Object.entries(os.networkInterfaces())) {
    if (virtualAdapterPattern.test(name)) continue

    for (const address of addresses ?? []) {
      if (address.family === 'IPv4' && !address.internal) {
        return address.address
      }
    }
  }

  return undefined
}

export default defineConfig({
  define: {
    __LAN_IP__: JSON.stringify(findLanAddress()),
  },
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      includeAssets: [
        'favicon.svg',
        'icons/icon.svg',
        'icons/icon-180.png',
        'icons/icon-192.png',
        'icons/icon-512.png',
      ],
      manifest: {
        name: 'Washflow',
        short_name: 'Washflow',
        description: 'Washflow app PWA',
        theme_color: '#0f172a',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'portrait',
        scope: '/',
        start_url: '/',
        id: 'washflow-pwa',
        icons: [
          {
            src: '/icons/icon-192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'any',
          },
          {
            src: '/icons/icon-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico}'],
        // Without this, the SW's catch-all NavigationRoute treats every
        // full-page navigation (including /swagger, /openapi, /api/*) as an
        // SPA route and serves index.html instead of letting it reach the
        // Javalin backend that shares this same origin/port in production.
        navigateFallbackDenylist: [
          /^\/api\//,
          /^\/swagger/,
          /^\/openapi/,
          /^\/webjars\//,
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      // In production the Javalin server serves both the built SPA and
      // /api/* from the same origin (see infra/http). This proxy makes
      // `bun run dev` behave the same way against the local backend.
      '/api': 'http://localhost:7000',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
