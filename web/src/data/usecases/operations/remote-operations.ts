import { HttpClient } from '@/data/protocols/http/http-client'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { OperationsReportModel } from '@/domain/models/operations-report-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { UserModel, UserProfile } from '@/domain/models/user-model'
import {
  Operations,
  PauseReason,
  PaymentMethod,
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
