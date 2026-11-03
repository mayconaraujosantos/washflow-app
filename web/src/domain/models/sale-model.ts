import type { PaymentMethod } from '@/domain/usecases/operations'

export type SaleType = 'QUICK_SALE' | 'BUDGET'

export type SaleItemModel = {
  id: string
  servicePriceId: number
  description: string
  quantity: number
  unitPrice: number
  total: number
}

export type SalePaymentModel = {
  id: string
  method: PaymentMethod
  amount: number
  receivedAt: string
}

export type SaleModel = {
  id: string
  type: SaleType
  vehicleId: string | null
  customerName: string | null
  customerPhone: string | null
  subtotal: number
  discount: number
  total: number
  notes: string | null
  createdBy: string | null
  createdAt: string
  items: SaleItemModel[]
  payments: SalePaymentModel[]
  paidAmount: number
  outstandingAmount: number
}
