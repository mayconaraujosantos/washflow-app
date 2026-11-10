import { AuditEntryModel } from '@/domain/models/audit-entry-model'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { OperationsReportModel } from '@/domain/models/operations-report-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { UserModel, UserProfile } from '@/domain/models/user-model'
import { VehicleLookupModel } from '@/domain/models/vehicle-lookup-model'
import { SaleModel, SaleType } from '@/domain/models/sale-model'

export type PaymentMethod = 'CASH' | 'PIX' | 'DEBIT_CARD' | 'CREDIT_CARD'
export type PauseReason = 'LUNCH' | 'RESTROOM' | 'OTHER'

export type RegisterEntryParams = {
  plate: string
  servicePriceId: number
  vehicleModel?: string
  vehicleColor?: string
  customerName?: string
  customerPhone?: string
  // ISO instants; without scheduledAt the car goes straight to the yard.
  scheduledAt?: string
  expectedDeliveryAt?: string
  notes?: string
}

export type CreateSaleParams = {
  type: SaleType
  items: { servicePriceId: number; quantity: number }[]
  discount?: number
  payments?: { method: PaymentMethod; amount: number }[]
  vehicleId?: string
  customerName?: string
  customerPhone?: string
  notes?: string
}

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
  updateUserEmail: (userId: string, email: string | null) => Promise<UserModel>
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
  registerEntry: (params: RegisterEntryParams) => Promise<ServiceOrderModel>
  searchVehicles: (query: string) => Promise<VehicleLookupModel[]>
  loadAgenda: (from: string, to: string) => Promise<ServiceOrderModel[]>
  createSale: (params: CreateSaleParams) => Promise<SaleModel>
  listSales: (from: string, to: string, type?: SaleType) => Promise<SaleModel[]>
  loadAudit: (from: string, to: string) => Promise<AuditEntryModel[]>
  createServicePrice: (params: {
    name: string
    price: number
    washerCommission?: number
  }) => Promise<ServicePriceModel>
}
