import { execFileSync } from 'node:child_process'
import { existsSync } from 'node:fs'

/**
 * Where the tests reach the e2e pod. On Linux and macOS Podman publishes the
 * ports on localhost; on Windows the Podman VM (WSL) doesn't forward localhost,
 * so the tests talk to the VM's own IP. E2E_HOST overrides both.
 *
 * Resolved once in the Playwright runner (config) and stored in E2E_HOST, so
 * the test workers inherit it instead of asking Podman again.
 */
export function e2eHost(): string {
  process.env.E2E_HOST ??= detectHost()
  return process.env.E2E_HOST
}

/** Ports must be published on the VM's address too when it isn't localhost. */
export function e2eBind(): string {
  return e2eHost() === 'localhost' ? '127.0.0.1' : '0.0.0.0'
}

export const ports = {
  app: process.env.E2E_PORT ?? '7100',
  mail: process.env.E2E_MAIL_PORT ?? '7125',
  whatsapp: process.env.E2E_WHATSAPP_PORT ?? '7182',
}

/** Git Bash on Windows (the WSL bash.exe in System32 can't see Podman). */
export function bash(): string {
  if (process.env.E2E_BASH) return process.env.E2E_BASH
  const gitBash = 'C:\\Program Files\\Git\\bin\\bash.exe'
  return process.platform === 'win32' && existsSync(gitBash) ? gitBash : 'bash'
}

function detectHost(): string {
  if (process.platform !== 'win32') return 'localhost'
  try {
    const address = execFileSync(
      'podman',
      ['machine', 'ssh', 'ip -4 -o addr show eth0'],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] },
    )
    return /inet (\d+\.\d+\.\d+\.\d+)/.exec(address)?.[1] ?? 'localhost'
  } catch {
    return 'localhost'
  }
}
