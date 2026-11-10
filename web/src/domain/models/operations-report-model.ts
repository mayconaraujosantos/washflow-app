export type OperationsReportModel = {
  from: string
  to: string
  finalizedOrders: number
  revenue: number
  commissions: number
  averageWashingMinutes: number | null
  services: {
    serviceName: string
    orders: number
    revenue: number
    commissions: number
  }[]
  paymentMethods: {
    method: string
    orders: number
    amount: number
  }[]
  pauses: {
    reason: 'LUNCH' | 'RESTROOM' | 'OTHER'
    count: number
  }[]
  // Refunds made in the period (by refund date): net = gross - refunds.
  refundedOrders: number
  refunds: number
  reversedCommissions: number
  netRevenue: number
  netCommissions: number
  refundReasons: { reason: string; count: number; amount: number }[]
}
