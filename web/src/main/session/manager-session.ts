import { UserModel } from '@/domain/models/user-model'

const STORAGE_KEY = 'washflow:manager-session'

export function saveManagerSession(user: UserModel): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

export function loadManagerSession(): UserModel | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as UserModel
  } catch {
    return null
  }
}

export function clearManagerSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
