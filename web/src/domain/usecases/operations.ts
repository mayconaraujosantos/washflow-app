import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { OperationsReportModel } from '@/domain/models/operations-report-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { UserModel, UserProfile } from '@/domain/models/user-model'

export type PaymentMethod = 'CASH' | 'PIX' | 'DEBIT_CARD' | 'CREDIT_CARD'
export type PauseReason = 'LUNCH' | 'RESTROOM' | 'OTHER'

export interface Operations {
  loadQueue: () => Promise<ServiceOrderModel[]>
  loadMyOrders: () => Promise<ServiceOrderModel[]>
  loadYard: () => Promise<ServiceOrderModel[]>
  loadServicePrices: () => Promise<ServicePriceModel[]>
  loadReport: (from: string, to: string) => Promise<OperationsReportModel>
  loadUsers: () => Promise<UserModel[]>
  createWasher: (params: {
    name: string
    phone: string
    password: string
  }) => Promise<UserModel>
  updateUserProfile: (
    userId: string,
    managerId: string,
    profile: UserProfile,
  ) => Promise<UserModel>
  setUserActive: (userId: string, active: boolean) => Promise<UserModel>
  checkIn: (serviceOrderId: string) => Promise<void>
  start: (serviceOrderId: string, washerId: string) => Promise<void>
  pause: (
    serviceOrderId: string,
    reason: PauseReason,
    description?: string,
  ) => Promise<void>
  resume: (serviceOrderId: string) => Promise<void>
  complete: (serviceOrderId: string) => Promise<void>
  finalize: (
    serviceOrderId: string,
    paymentMethod: PaymentMethod,
  ) => Promise<void>
}
