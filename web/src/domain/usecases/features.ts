import type {
  ChecklistItem,
  ChecklistType,
  ServiceOrderChecklist,
  ServiceOrderPhoto,
  CashTransaction,
  CashTransactionReport,
  ExpenseCategory,
} from '@/domain/models/feature-models'

export interface Features {
  // Checklists
  saveChecklist: (
    serviceOrderId: string,
    type: ChecklistType,
    items: ChecklistItem[],
    signatureImage: string | null,
  ) => Promise<ServiceOrderChecklist>
  getChecklists: (serviceOrderId: string) => Promise<ServiceOrderChecklist[]>

  // Photos
  savePhoto: (
    serviceOrderId: string,
    type: 'ENTRY' | 'EXIT',
    imageData: string,
    description: string | null,
  ) => Promise<ServiceOrderPhoto>
  getPhotos: (serviceOrderId: string) => Promise<ServiceOrderPhoto[]>
  deletePhoto: (photoId: string) => Promise<void>

  // Cash transactions
  createTransaction: (
    type: 'INCOME' | 'EXPENSE',
    amount: string,
    referenceDate: string,
    categoryId: string | null,
    description: string | null,
    serviceOrderId: string | null,
  ) => Promise<CashTransaction>
  listTransactions: (
    from: string | null,
    to: string | null,
  ) => Promise<CashTransaction[]>
  updateTransaction: (
    id: string,
    updates: Partial<CashTransaction>,
  ) => Promise<CashTransaction>
  deleteTransaction: (id: string) => Promise<void>
  transactionReport: (
    from: string,
    to: string,
  ) => Promise<CashTransactionReport>

  // Expense categories
  createCategory: (name: string, color: string) => Promise<ExpenseCategory>
  listCategories: () => Promise<ExpenseCategory[]>
}
