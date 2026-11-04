/**
 * Sends browser errors (uncaught exceptions, rejected promises, crashed
 * screens) to POST /api/client-errors so they show up in the server log next
 * to the API requests. Best effort: never throws, deduplicates repeats and
 * caps how many reports one page load can send.
 */

const MAX_REPORTS_PER_PAGE = 10

type Report = {
  message: string
  stack?: string
  source: string
  page: string
  userAgent: string
}

type Send = (url: string, report: Report) => void

const defaultSend: Send = (url, report) => {
  void fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(report),
    keepalive: true,
  }).catch(() => undefined)
}

export class ErrorReporter {
  private readonly seen = new Set<string>()
  private sent = 0

  constructor(
    private readonly url: string,
    private readonly send: Send = defaultSend,
  ) {}

  report(error: unknown, source: string): void {
    if (this.sent >= MAX_REPORTS_PER_PAGE) return
    const { message, stack } = describe(error)
    const key = `${source}|${message}`
    if (this.seen.has(key)) return
    this.seen.add(key)
    this.sent += 1
    try {
      this.send(this.url, {
        message,
        stack,
        source,
        page: window.location.pathname,
        userAgent: navigator.userAgent,
      })
    } catch {
      // Reporting must never break the app.
    }
  }

  /** Listens for errors nothing else caught. */
  install(target: Window = window): void {
    target.addEventListener('error', (event) =>
      this.report(event.error ?? event.message, 'window.onerror'),
    )
    target.addEventListener('unhandledrejection', (event) =>
      this.report(event.reason, 'unhandledrejection'),
    )
  }
}

const describe = (error: unknown): { message: string; stack?: string } => {
  if (error instanceof Error) {
    return { message: error.message || error.name, stack: error.stack }
  }
  return { message: typeof error === 'string' ? error : String(error) }
}
