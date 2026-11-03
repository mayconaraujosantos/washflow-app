import type { HttpClient } from '@/data/protocols/http/http-client'
import type {
  AuthOptionsModel,
  TicketModel,
  WasherBadgeModel,
} from '@/domain/models/access-model'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import { NeedsNameError, type Access } from '@/domain/usecases/access'

type ErrorBody = { error?: string; needsName?: boolean }

export class RemoteAccess implements Access {
  constructor(
    private readonly baseUrl: string,
    private readonly httpClient: HttpClient,
  ) {}

  issueWasherBadge(washerId: string): Promise<WasherBadgeModel> {
    return this.request(`/api/users/${washerId}/badge`, 'post')
  }

  washerPinLogin(
    phone: string,
    pin: string,
  ): Promise<AuthenticatedSessionModel> {
    return this.request('/api/auth/washers/pin', 'post', { phone, pin })
  }

  washerBadgeLogin(badgeToken: string): Promise<AuthenticatedSessionModel> {
    return this.request('/api/auth/washers/badge', 'post', { badgeToken })
  }

  async issueTicket(serviceOrderId: string): Promise<string> {
    const body = await this.request<{ token: string }>(
      `/api/service-orders/${serviceOrderId}/ticket`,
      'post',
    )
    return body.token
  }

  loadTicket(token: string): Promise<TicketModel> {
    return this.request(`/api/tickets/${encodeURIComponent(token)}`, 'get')
  }

  options(): Promise<AuthOptionsModel> {
    return this.request('/api/auth/options', 'get')
  }

  async requestCode(phone: string): Promise<number> {
    const body = await this.request<{ expiresInSeconds: number }>(
      '/api/auth/customers/otp',
      'post',
      { phone },
    )
    return body.expiresInSeconds
  }

  verifyCode(
    phone: string,
    code: string,
    name?: string,
  ): Promise<AuthenticatedSessionModel> {
    return this.request('/api/auth/customers/otp/verify', 'post', {
      phone,
      code,
      name,
    })
  }

  googleLogin(credential: string): Promise<AuthenticatedSessionModel> {
    return this.request('/api/auth/customers/google', 'post', { credential })
  }

  private async request<T>(
    path: string,
    method: 'get' | 'post',
    body?: unknown,
  ): Promise<T> {
    const response = await this.httpClient.request({
      url: `${this.baseUrl}${path}`,
      method,
      body,
    })
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return response.body as T
    }
    const error = response.body as ErrorBody | undefined
    if (error?.needsName) throw new NeedsNameError(error.error)
    throw new Error(error?.error ?? 'Não foi possível concluir a operação')
  }
}
