import { UserModel } from '@/domain/models/user-model'

// Plain localStorage wrapper, not a swappable data.protocols port - there's
// no second implementation to abstract for yet, just the browser's own
// storage, so a "main" glue module is enough (see clean-ts-api's own
// LocalStorageAdapter for when that changes).
const STORAGE_KEY = 'washflow:customer-session'

export function saveCustomerSession(user: UserModel): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

export function loadCustomerSession(): UserModel | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as UserModel
  } catch {
    return null
  }
}

export function clearCustomerSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
