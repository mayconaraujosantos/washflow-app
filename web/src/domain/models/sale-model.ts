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
  refunds: SaleRefundModel[]
  refundedAmount: number
}

// venda_estornos: money given back in one method; payments never change.
export type SaleRefundModel = {
  id: string
  saleId: string
  method: PaymentMethod
  amount: number
  reason: string
  description: string | null
  createdBy: string
  createdAt: string
}

const cents = (value: number) => Math.round(value * 100) / 100

/** What each method can still give back: paid through it minus refunded in it (zeros dropped). */
export function refundableByMethod(
  sale: Pick<SaleModel, 'payments' | 'refunds'>,
): Partial<Record<PaymentMethod, number>> {
  const left: Partial<Record<PaymentMethod, number>> = {}
  for (const payment of sale.payments) {
    left[payment.method] = (left[payment.method] ?? 0) + Number(payment.amount)
  }
  for (const refund of sale.refunds ?? []) {
    left[refund.method] = (left[refund.method] ?? 0) - Number(refund.amount)
  }
  for (const method of Object.keys(left) as PaymentMethod[]) {
    left[method] = cents(left[method] ?? 0)
    if ((left[method] ?? 0) <= 0) delete left[method]
  }
  return left
}
