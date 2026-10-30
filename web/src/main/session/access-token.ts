const SESSION_KEYS = {
  customer: 'washflow:customer-session',
  washer: 'washflow:washer-session',
  manager: 'washflow:manager-session',
} as const

export function loadAccessToken(): string | null {
  const profile = profileForPath(window.location.pathname)
  if (profile) return readToken(SESSION_KEYS[profile])

  const availableTokens = Object.values(SESSION_KEYS)
    .map(readToken)
    .filter((token): token is string => token !== null)
  return availableTokens.length === 1 ? availableTokens[0] : null
}

function profileForPath(path: string): keyof typeof SESSION_KEYS | null {
  if (/^\/(?:qrcodes\/|login\/)?cliente(?:\/|$)/.test(path)) return 'customer'
  if (/^\/(?:qrcodes\/|login\/)?lavador(?:\/|$)/.test(path)) return 'washer'
  if (/^\/(?:qrcodes\/|login\/)?gerente(?:\/|$)/.test(path)) return 'manager'
  return null
}

function readToken(key: string): string | null {
  const raw = localStorage.getItem(key)
  if (!raw) return null

  try {
    const token = (JSON.parse(raw) as { token?: unknown }).token
    return typeof token === 'string' && token.length > 0 ? token : null
  } catch {
    localStorage.removeItem(key)
    return null
  }
}
