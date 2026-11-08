import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_PROXY_TARGET || 'http://localhost:8080'
  const proxy = { target, changeOrigin: true, secure: false }

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: { '/api': proxy, '/swagger-ui': proxy, '/v3': proxy },
    },
    build: { outDir: 'dist', sourcemap: false },
    test: { environment: 'jsdom', include: ['src/**/*.test.{js,jsx}'] },
  }
})
