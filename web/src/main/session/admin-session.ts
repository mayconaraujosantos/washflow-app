import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'

const STORAGE_KEY = 'washflow:admin-session'

export function saveAdminSession(session: AuthenticatedSessionModel): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function loadAdminSession(): AuthenticatedSessionModel | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthenticatedSessionModel
  } catch {
    return null
  }
}

export function clearAdminSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
