import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  retries: 0,
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080',
    extraHTTPHeaders: {
      'Content-Type': 'application/json'
    }
  },
  projects: [
    {
      name: 'api-smoke',
      testMatch: /smoke\.spec\.js/
    },
    {
      name: 'admin-governance',
      testMatch: /admin-governance\.spec\.js/
    },
    {
      name: 'offline-order',
      testMatch: /offline-order\.spec\.js/
    },
    {
      name: 'cart-edge',
      testMatch: /cart-edge-cases\.spec\.js/
    }
  ]
})
