import { defineConfig, devices } from '@playwright/test'

// Runs against a live stack: `docker compose up --build`, then `npm run test:e2e`.
export default defineConfig({
  testDir: './e2e',
  timeout: 60000,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    trace: 'retain-on-failure',
    ...devices['Desktop Chrome'],
  },
})
