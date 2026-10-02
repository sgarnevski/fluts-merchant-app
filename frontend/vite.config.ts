import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

/** Everything the browser sends to flut-app; same-origin, so the session and CSRF cookies just work. */
const BACKEND_PATHS = ['/api', '/oauth2', '/login', '/logout', '/swagger-ui', '/v3/api-docs']
const backend = process.env.FLUT_APP_URL ?? 'http://localhost:8081'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Host header is kept (no changeOrigin), so OAuth redirects come back to the dev server.
    proxy: Object.fromEntries(BACKEND_PATHS.map((path) => [path, { target: backend, xfwd: true }])),
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
    restoreMocks: true,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/main.tsx', 'src/api/schema.gen.ts', 'src/api/types.ts', 'src/test/**', 'src/**/*.test.{ts,tsx}'],
      thresholds: { 100: true },
    },
  },
})
