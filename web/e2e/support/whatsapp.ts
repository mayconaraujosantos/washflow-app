import { expect } from '@playwright/test'
import { e2eHost, ports } from './host'

const api = `http://${e2eHost()}:${ports.whatsapp}/test`

type Sent = { instance: string; number: string; text: string }

/**
 * The e2e pod's fake Evolution API (support/fake-evolution.mjs): the test plays
 * the WhatsApp side - scans the QR code, writes as a customer to the
 * lava-jato's number and reads what the assistant answered.
 */
export const whatsapp = {
  /** The instance the app created for a lava-jato that is waiting for the scan. */
  async waitingInstance(): Promise<string> {
    let name = ''
    await expect
      .poll(
        async () => {
          const instances = (await (
            await fetch(`${api}/instances`)
          ).json()) as { instanceName: string; state: string }[]
          name =
            instances.find((instance) => instance.state === 'connecting')
              ?.instanceName ?? ''
          return name
        },
        { message: 'instância esperando a leitura do QR Code' },
      )
      .not.toBe('')
    return name
  },

  /** The phone scans the QR code: the number is linked. */
  async scan(instance: string): Promise<void> {
    const response = await fetch(
      `${api}/scan/${encodeURIComponent(instance)}`,
      {
        method: 'POST',
      },
    )
    expect(response.ok, 'leitura do QR Code').toBe(true)
  },

  /** Everything the app sent to this WhatsApp number so far. */
  async sentTo(number: string): Promise<string[]> {
    const messages = (await (
      await fetch(`${api}/messages?number=${number}`)
    ).json()) as Sent[]
    return messages.map((message) => message.text)
  },

  /**
   * The customer writes `text` to the lava-jato's number and gets the
   * assistant's answers (the messages sent since), joined in one string.
   */
  async chat(instance: string, number: string, text: string): Promise<string> {
    const before = (await this.sentTo(number)).length
    const response = await fetch(`${api}/receive`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ instance, number, text }),
    })
    const { webhookStatus } = (await response.json()) as {
      webhookStatus: number
    }
    expect(webhookStatus, `webhook do app para "${text}"`).toBe(200)
    let replies: string[] = []
    await expect
      .poll(
        async () => {
          replies = (await this.sentTo(number)).slice(before)
          return replies.length
        },
        { message: `resposta do assistente para "${text}"` },
      )
      .toBeGreaterThan(0)
    return replies.join('\n\n')
  },

  /** The customer writes and the assistant stays quiet (handed over to the staff). */
  async chatWithoutReply(
    instance: string,
    number: string,
    text: string,
  ): Promise<void> {
    const before = (await this.sentTo(number)).length
    const response = await fetch(`${api}/receive`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ instance, number, text }),
    })
    // The app answers within the webhook call, so nothing sent by now means no answer.
    expect(
      ((await response.json()) as { webhookStatus: number }).webhookStatus,
    ).toBe(200)
    expect(await this.sentTo(number)).toHaveLength(before)
  },

  /** Waits for a message to `number` matching `text` (a notice the app sends on its own). */
  async expectMessage(number: string, text: RegExp): Promise<void> {
    await expect
      .poll(async () => (await this.sentTo(number)).join('\n\n'), {
        message: `mensagem ${text} para ${number}`,
      })
      .toMatch(text)
  },
}
