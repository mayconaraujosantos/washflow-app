import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end tests that use the app like people do: platform admin, managers,
 * washers and customers, each on their own "phone" (browser context).
 *
 * They run against a throwaway environment (scripts/e2e-env.sh: fresh
 * PostgreSQL + the app image built from the working tree, on :7100), never
 * against the development app on :7000.
 *
 *   make e2e        watch it in a visible browser, slowed down
 *   make e2e-ci     headless
 *   E2E_KEEP=1      keep the environment running after the tests
 */
const port = process.env.E2E_PORT ?? '7100'
const headed = process.env.E2E_HEADED === '1'
const slowMo = Number(process.env.E2E_SLOWMO ?? (headed ? 400 : 0))

export default defineConfig({
  testDir: './e2e',
  // The story is one continuous journey: steps share state and run in order.
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 120_000,
  expect: { timeout: 15_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  globalSetup: './e2e/global-setup.ts',
  globalTeardown: './e2e/global-teardown.ts',
  use: {
    ...devices['Pixel 7'],
    baseURL: `http://localhost:${port}`,
    locale: 'pt-BR',
    timezoneId: 'America/Manaus',
    headless: !headed,
    launchOptions: { slowMo },
    trace: 'on',
    video: 'on',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium-mobile' }],
})
