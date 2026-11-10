import {
  devices,
  expect,
  type Browser,
  type BrowserContext,
  type Page,
} from '@playwright/test'

const port = process.env.E2E_PORT ?? '7100'
export const baseURL = `http://localhost:${port}`

/**
 * One person = one phone: its own browser context, so sessions, the
 * remembered lava-jato and localStorage never leak between people. Each
 * phone records its own video under test-results/people/<name>; the trace
 * of every test (config `trace: 'on'`) covers all phones.
 */
export class Phones {
  private readonly open: { context: BrowserContext }[] = []

  constructor(private readonly browser: Browser) {}

  async of(name: string): Promise<Page> {
    const context = await this.browser.newContext({
      ...devices['Pixel 7'],
      baseURL,
      locale: 'pt-BR',
      timezoneId: 'America/Manaus',
      recordVideo: { dir: `test-results/people/${name}` },
    })
    this.open.push({ context })
    return context.newPage()
  }

  async closeAll(): Promise<void> {
    for (const { context } of this.open) await context.close()
    this.open.length = 0
  }
}

/** The toast/notice the app shows after an action. */
export async function expectMessage(page: Page, text: string | RegExp) {
  await expect(page.getByText(text).first()).toBeVisible()
}

/** A card (order, vehicle, account...) that mentions the given text. */
export function cardWith(page: Page, text: string) {
  return page.locator('li').filter({ hasText: text }).first()
}
