import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Pre-bundle the lazy chart dependency before a chart route is opened.
  optimizeDeps: { include: ['lightweight-charts'] },
  server: {
    // Bind IPv4 loopback explicitly so emailed 127.0.0.1 links work as well as localhost.
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
  },
})
