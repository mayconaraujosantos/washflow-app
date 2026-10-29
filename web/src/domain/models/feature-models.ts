export interface ChecklistItem {
  label: string
  checked: boolean
  note: string | null
}

export type ChecklistType = 'ENTRY' | 'EXIT'

export interface ServiceOrderChecklist {
  id: string
  serviceOrderId: string
  type: ChecklistType
  items: ChecklistItem[]
  signatureImage: string | null
  createdAt: string
  createdBy: string | null
}

export interface ServiceOrderPhoto {
  id: string
  serviceOrderId: string
  type: 'ENTRY' | 'EXIT'
  imageData: string
  description: string | null
  createdAt: string
  createdBy: string | null
}

export type TransactionType = 'INCOME' | 'EXPENSE'

export interface CashTransaction {
  id: string
  type: TransactionType
  amount: string
  categoryId: string | null
  description: string | null
  referenceDate: string
  serviceOrderId: string | null
  createdAt: string
  createdBy: string | null
}

export interface ExpenseCategory {
  id: string
  name: string
  color: string
  createdAt: string
}

export interface CashTransactionReport {
  from: string
  to: string
  totalIncome: string
  totalExpense: string
  netBalance: string
  totalCount: number
  byCategory: Array<{
    category_name: string
    category_color: string
    transaction_type: string
    total: string
    count: number
  }>
}
