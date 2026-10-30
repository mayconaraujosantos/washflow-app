// Injected by vite.config.ts's `define` - this machine's LAN IPv4 address,
// auto-detected at dev-server/build start, or undefined if none was found.
declare const __LAN_IP__: string | undefined

const LOCAL_HOSTNAMES = new Set(['localhost', '127.0.0.1'])

/**
 * `window.location.origin` as-is almost everywhere - including the real
 * production domain once deployed, where it's already correct. The one case
 * it's wrong: viewing the app via `localhost`/`127.0.0.1` and generating a QR
 * code from it, since "localhost" resolves to whichever device scans the
 * code, not this machine. There, swap in __LAN_IP__ (same protocol/port)
 * instead, so the QR code page is scannable from another device on the same
 * Wi-Fi without anyone having to type an IP by hand.
 */
export function getReachableOrigin(): string {
  if (!LOCAL_HOSTNAMES.has(window.location.hostname) || !__LAN_IP__) {
    return window.location.origin
  }

  const port = window.location.port ? `:${window.location.port}` : ''
  return `${window.location.protocol}//${__LAN_IP__}${port}`
}
