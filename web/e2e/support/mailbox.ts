import { expect } from '@playwright/test'

const mailPort = process.env.E2E_MAIL_PORT ?? '7125'
const api = `http://localhost:${mailPort}/api/v1`

type Summary = { ID: string; Created: string }

/**
 * The e2e pod's Mailpit inbox (scripts/e2e-env.sh): the app sends real SMTP
 * there and the test reads the message like the person would.
 */
export const mailbox = {
  /** How many e-mails this address has received so far. */
  async count(to: string): Promise<number> {
    return (await search(to)).length
  },

  /** Waits for a new e-mail to `to` (beyond `seen`) and returns its 6-digit code. */
  async nextCode(to: string, seen: number): Promise<string> {
    let messages: Summary[] = []
    await expect
      .poll(
        async () => {
          messages = await search(to)
          return messages.length
        },
        { message: `e-mail com código para ${to}`, timeout: 15_000 },
      )
      .toBeGreaterThan(seen)
    const newest = messages[0]
    const message = (await (
      await fetch(`${api}/message/${newest.ID}`)
    ).json()) as { Text: string; Subject: string }
    const code = /\b(\d{6})\b/.exec(message.Text)?.[1]
    expect(code, `código no e-mail "${message.Subject}"`).toBeTruthy()
    return code!
  },
}

async function search(to: string): Promise<Summary[]> {
  const response = await fetch(
    `${api}/search?query=${encodeURIComponent(`to:"${to}"`)}`,
  )
  const body = (await response.json()) as { messages: Summary[] }
  return body.messages
}
