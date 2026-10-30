import { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'

const STORAGE_KEY = 'washflow:manager-session'

export function saveManagerSession(session: AuthenticatedSessionModel): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function loadManagerSession(): AuthenticatedSessionModel | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as AuthenticatedSessionModel
  } catch {
    return null
  }
}

export function clearManagerSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
