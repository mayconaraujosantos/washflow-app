import type {
  ChecklistItem,
  ChecklistType,
  ServiceOrderChecklist,
  ServiceOrderPhoto,
  CashTransaction,
  CashTransactionReport,
  ExpenseCategory,
} from '@/domain/models/feature-models'
import { loadAccessToken } from '@/main/session/access-token'

const BASE = '/api'

// ==================== CHECKLISTS ====================

export async function saveChecklist(
  serviceOrderId: string,
  type: ChecklistType,
  items: ChecklistItem[],
  signatureImage: string | null,
): Promise<ServiceOrderChecklist> {
  const response = await fetch(`${BASE}/service-orders/${serviceOrderId}/checklist`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${loadAccessToken()}`,
    },
    body: JSON.stringify({ type, items, signatureImage }),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: 'Failed to save checklist' }))
    throw new Error(error.error)
  }
  return response.json()
}

export async function getChecklists(
  serviceOrderId: string,
): Promise<ServiceOrderChecklist[]> {
  const response = await fetch(`${BASE}/service-orders/${serviceOrderId}/checklist`)
  if (!response.ok) throw new Error('Failed to load checklists')
  return response.json()
}

// ==================== PHOTOS ====================

export async function savePhoto(
  serviceOrderId: string,
  type: 'ENTRY' | 'EXIT',
  imageData: string,
  description: string | null,
): Promise<ServiceOrderPhoto> {
  const response = await fetch(`${BASE}/service-orders/${serviceOrderId}/photos`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${loadAccessToken()}`,
    },
    body: JSON.stringify({ type, imageData, description }),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: 'Failed to save photo' }))
    throw new Error(error.error)
  }
  return response.json()
}

export async function getPhotos(
  serviceOrderId: string,
): Promise<ServiceOrderPhoto[]> {
  const response = await fetch(`${BASE}/service-orders/${serviceOrderId}/photos`)
  if (!response.ok) throw new Error('Failed to load photos')
  return response.json()
}

export async function deletePhoto(photoId: string): Promise<void> {
  const response = await fetch(`${BASE}/service-orders/photos/${photoId}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${loadAccessToken()}`,
    },
  })
  if (!response.ok) throw new Error('Failed to delete photo')
}

// ==================== CASH TRANSACTIONS ====================

export async function createTransaction(
  type: 'INCOME' | 'EXPENSE',
  amount: string,
  referenceDate: string,
  categoryId: string | null,
  description: string | null,
  serviceOrderId: string | null,
): Promise<CashTransaction> {
  const response = await fetch(`${BASE}/cash-transactions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${loadAccessToken()}`,
    },
    body: JSON.stringify({ type, amount, referenceDate, categoryId, description, serviceOrderId }),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: 'Failed to create transaction' }))
    throw new Error(error.error)
  }
  return response.json()
}

export async function listTransactions(
  from: string | null,
  to: string | null,
): Promise<CashTransaction[]> {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const response = await fetch(`${BASE}/cash-transactions?${params}`)
  if (!response.ok) throw new Error('Failed to load transactions')
  return response.json()
}

export async function updateTransaction(
  id: string,
  updates: Partial<CashTransaction>,
): Promise<CashTransaction> {
  const response = await fetch(`${BASE}/cash-transactions/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${loadAccessToken()}`,
    },
    body: JSON.stringify(updates),
  })
  if (!response.ok) throw new Error('Failed to update transaction')
  return response.json()
}

export async function deleteTransaction(id: string): Promise<void> {
  const response = await fetch(`${BASE}/cash-transactions/${id}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${loadAccessToken()}`,
    },
  })
  if (!response.ok) throw new Error('Failed to delete transaction')
}

export async function transactionReport(
  from: string,
  to: string,
): Promise<CashTransactionReport> {
  const response = await fetch(`${BASE}/cash-transactions/report?from=${from}&to=${to}`)
  if (!response.ok) throw new Error('Failed to load report')
  return response.json()
}

// ==================== EXPENSE CATEGORIES ====================

export async function createCategory(
  name: string,
  color: string,
): Promise<ExpenseCategory> {
  const response = await fetch(`${BASE}/expense-categories`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${loadAccessToken()}`,
    },
    body: JSON.stringify({ name, color }),
  })
  if (!response.ok) throw new Error('Failed to create category')
  return response.json()
}

export async function listCategories(): Promise<ExpenseCategory[]> {
  const response = await fetch(`${BASE}/expense-categories`)
  if (!response.ok) throw new Error('Failed to load categories')
  return response.json()
}
