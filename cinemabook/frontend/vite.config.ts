import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // All /api requests (including /api/v1/files/stream) go to Spring Boot.
      // Using a relative stream URL on the <video> element means the browser
      // fetches /api/v1/files/stream?token=... from localhost:3000 — Vite
      // proxies it to localhost:8082 transparently. No CORS issues at all.
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});