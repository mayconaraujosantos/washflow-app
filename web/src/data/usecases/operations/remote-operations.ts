import { HttpClient } from '@/data/protocols/http/http-client'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { OperationsReportModel } from '@/domain/models/operations-report-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { UserModel, UserProfile } from '@/domain/models/user-model'
import { VehicleLookupModel } from '@/domain/models/vehicle-lookup-model'
import { SaleModel, SaleType } from '@/domain/models/sale-model'
import {
  CreateSaleParams,
  Operations,
  PauseReason,
  PaymentMethod,
  RegisterEntryParams,
} from '@/domain/usecases/operations'

type ErrorBody = { error?: string }

export class RemoteOperations implements Operations {
  constructor(
    private readonly baseUrl: string,
    private readonly httpClient: HttpClient,
  ) {}

  loadQueue(): Promise<ServiceOrderModel[]> {
    return this.request('/api/operations/queue', 'get') as Promise<
      ServiceOrderModel[]
    >
  }

  loadMyOrders(): Promise<ServiceOrderModel[]> {
    return this.request('/api/operations/my-orders', 'get') as Promise<
      ServiceOrderModel[]
    >
  }

  loadYard(): Promise<ServiceOrderModel[]> {
    return this.request('/api/operations/yard', 'get') as Promise<
      ServiceOrderModel[]
    >
  }

  loadServicePrices(): Promise<ServicePriceModel[]> {
    return this.request('/api/services', 'get') as Promise<ServicePriceModel[]>
  }

  loadReport(from: string, to: string): Promise<OperationsReportModel> {
    const query = new URLSearchParams({ from, to })
    return this.request(
      `/api/operations/report?${query.toString()}`,
      'get',
    ) as Promise<OperationsReportModel>
  }

  loadUsers(): Promise<UserModel[]> {
    return this.request('/api/users', 'get') as Promise<UserModel[]>
  }

  createWasher(params: {
    name: string
    phone: string
    password: string
  }): Promise<UserModel> {
    return this.request('/api/users', 'post', params) as Promise<UserModel>
  }

  updateUserProfile(
    userId: string,
    managerId: string,
    profile: UserProfile,
  ): Promise<UserModel> {
    return this.request(`/api/users/${userId}/profile`, 'patch', {
      managerId,
      newProfile: profile,
    }) as Promise<UserModel>
  }

  setUserActive(userId: string, active: boolean): Promise<UserModel> {
    return this.request(`/api/users/${userId}/active`, 'patch', {
      active,
    }) as Promise<UserModel>
  }

  async checkIn(serviceOrderId: string): Promise<void> {
    await this.request(
      `/api/service-orders/${serviceOrderId}/check-in`,
      'patch',
    )
  }

  async start(serviceOrderId: string, washerId: string): Promise<void> {
    await this.request(`/api/service-orders/${serviceOrderId}/start`, 'patch', {
      washerId,
    })
  }

  async complete(serviceOrderId: string): Promise<void> {
    await this.request(
      `/api/service-orders/${serviceOrderId}/complete`,
      'patch',
    )
  }

  async pause(
    serviceOrderId: string,
    reason: PauseReason,
    description?: string,
  ): Promise<void> {
    await this.request(`/api/service-orders/${serviceOrderId}/pause`, 'patch', {
      reason,
      description,
    })
  }

  async resume(serviceOrderId: string): Promise<void> {
    await this.request(`/api/service-orders/${serviceOrderId}/resume`, 'patch')
  }

  async finalize(
    serviceOrderId: string,
    paymentMethod: PaymentMethod,
  ): Promise<void> {
    await this.request(
      `/api/service-orders/${serviceOrderId}/finalize`,
      'patch',
      {
        paymentMethod,
      },
    )
  }

  registerEntry(params: RegisterEntryParams): Promise<ServiceOrderModel> {
    return this.request(
      '/api/operations/entries',
      'post',
      params,
    ) as Promise<ServiceOrderModel>
  }

  searchVehicles(query: string): Promise<VehicleLookupModel[]> {
    const search = new URLSearchParams({ query })
    return this.request(
      `/api/operations/vehicles?${search.toString()}`,
      'get',
    ) as Promise<VehicleLookupModel[]>
  }

  loadAgenda(from: string, to: string): Promise<ServiceOrderModel[]> {
    const query = new URLSearchParams({ from, to })
    return this.request(
      `/api/operations/agenda?${query.toString()}`,
      'get',
    ) as Promise<ServiceOrderModel[]>
  }

  createSale(params: CreateSaleParams): Promise<SaleModel> {
    return this.request('/api/sales', 'post', params) as Promise<SaleModel>
  }

  listSales(from: string, to: string, type?: SaleType): Promise<SaleModel[]> {
    const query = new URLSearchParams({ from, to })
    if (type) query.set('type', type)
    return this.request(`/api/sales?${query.toString()}`, 'get') as Promise<
      SaleModel[]
    >
  }

  createServicePrice(params: {
    name: string
    price: number
    washerCommission?: number
  }): Promise<ServicePriceModel> {
    return this.request(
      '/api/services',
      'post',
      params,
    ) as Promise<ServicePriceModel>
  }

  private async request<T>(
    path: string,
    method: 'get' | 'patch' | 'post',
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
