import { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'

const STORAGE_KEY = 'washflow:washer-session'

export function saveWasherSession(session: AuthenticatedSessionModel): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function loadWasherSession(): AuthenticatedSessionModel | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as AuthenticatedSessionModel
  } catch {
    return null
  }
}

export function clearWasherSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
