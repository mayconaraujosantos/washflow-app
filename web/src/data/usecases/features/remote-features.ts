import { HttpClient } from '@/data/protocols/http/http-client'
import type { Features } from '@/domain/usecases/features'
import type {
  ChecklistItem,
  ChecklistType,
  ServiceOrderChecklist,
  ServiceOrderPhoto,
  CashTransaction,
  CashTransactionReport,
  ExpenseCategory,
} from '@/domain/models/feature-models'

type ErrorBody = { error?: string }

export class RemoteFeatures implements Features {
  constructor(
    private readonly baseUrl: string,
    private readonly httpClient: HttpClient,
  ) {}

  // ==================== CHECKLISTS ====================

  saveChecklist(
    serviceOrderId: string,
    type: ChecklistType,
    items: ChecklistItem[],
    signatureImage: string | null,
  ): Promise<ServiceOrderChecklist> {
    return this.request(
      `/api/service-orders/${serviceOrderId}/checklist`,
      'post',
      { type, items, signatureImage },
    ) as Promise<ServiceOrderChecklist>
  }

  getChecklists(serviceOrderId: string): Promise<ServiceOrderChecklist[]> {
    return this.request(
      `/api/service-orders/${serviceOrderId}/checklist`,
      'get',
    ) as Promise<ServiceOrderChecklist[]>
  }

  // ==================== PHOTOS ====================

  savePhoto(
    serviceOrderId: string,
    type: 'ENTRY' | 'EXIT',
    imageData: string,
    description: string | null,
  ): Promise<ServiceOrderPhoto> {
    return this.request(
      `/api/service-orders/${serviceOrderId}/photos`,
      'post',
      { type, imageData, description },
    ) as Promise<ServiceOrderPhoto>
  }

  getPhotos(serviceOrderId: string): Promise<ServiceOrderPhoto[]> {
    return this.request(
      `/api/service-orders/${serviceOrderId}/photos`,
      'get',
    ) as Promise<ServiceOrderPhoto[]>
  }

  deletePhoto(photoId: string): Promise<void> {
    return this.request(`/api/service-orders/photos/${photoId}`, 'delete')
  }

  // ==================== CASH TRANSACTIONS ====================

  createTransaction(
    type: 'INCOME' | 'EXPENSE',
    amount: string,
    referenceDate: string,
    categoryId: string | null,
    description: string | null,
    serviceOrderId: string | null,
  ): Promise<CashTransaction> {
    return this.request('/api/cash-transactions', 'post', {
      type,
      amount,
      referenceDate,
      categoryId,
      description,
      serviceOrderId,
    }) as Promise<CashTransaction>
  }

  listTransactions(
    from: string | null,
    to: string | null,
  ): Promise<CashTransaction[]> {
    const params = new URLSearchParams()
    if (from) params.set('from', from)
    if (to) params.set('to', to)
    const query = params.toString()
    const url = query ? `/api/cash-transactions?${query}` : '/api/cash-transactions'
    return this.request(url, 'get') as Promise<CashTransaction[]>
  }

  updateTransaction(
    id: string,
    updates: Partial<CashTransaction>,
  ): Promise<CashTransaction> {
    return this.request(`/api/cash-transactions/${id}`, 'put', updates) as Promise<
      CashTransaction
    >
  }

  deleteTransaction(id: string): Promise<void> {
    return this.request(`/api/cash-transactions/${id}`, 'delete')
  }

  transactionReport(from: string, to: string): Promise<CashTransactionReport> {
    const query = new URLSearchParams({ from, to })
    return this.request(
      `/api/cash-transactions/report?${query.toString()}`,
      'get',
    ) as Promise<CashTransactionReport>
  }

  // ==================== EXPENSE CATEGORIES ====================

  createCategory(name: string, color: string): Promise<ExpenseCategory> {
    return this.request('/api/expense-categories', 'post', { name, color }) as Promise<
      ExpenseCategory
    >
  }

  listCategories(): Promise<ExpenseCategory[]> {
    return this.request('/api/expense-categories', 'get') as Promise<ExpenseCategory[]>
  }

  // ==================== INTERNALS ====================

  private async request<T>(
    path: string,
    method: 'get' | 'post' | 'put' | 'delete',
    body?: unknown,
  ): Promise<T> {
    const response = await this.httpClient.request({
      url: `${this.baseUrl}${path}`,
      method,
      body,
    })
    if (response.statusCode < 200 || response.statusCode >= 300) {
      const message = (response.body as ErrorBody | undefined)?.error
      throw new Error(message ?? 'Não foi possível concluir a operação')
    }
    return response.body as T
  }
}
