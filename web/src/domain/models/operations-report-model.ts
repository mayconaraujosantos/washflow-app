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
}
