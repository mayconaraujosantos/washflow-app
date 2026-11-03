/** Badge token from a scanned QR: the login link (?cracha=...) or the raw token. */
export function badgeTokenFrom(value: string): string | null {
  try {
    return new URL(value).searchParams.get('cracha')
  } catch {
    return /^[A-Za-z0-9_-]{20,}$/.test(value) ? value : null
  }
}
