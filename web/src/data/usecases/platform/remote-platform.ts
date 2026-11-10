import type { HttpClient } from '@/data/protocols/http/http-client'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type {
  EstablishmentListingModel,
  EstablishmentSummaryModel,
} from '@/domain/models/platform-model'
import type {
  NewEstablishmentParams,
  Platform,
} from '@/domain/usecases/platform'

type ErrorBody = { error?: string }

export class RemotePlatform implements Platform {
  constructor(
    private readonly baseUrl: string,
    private readonly httpClient: HttpClient,
  ) {}

  search(query: string): Promise<EstablishmentListingModel[]> {
    const params = new URLSearchParams({ query })
    return this.request(`/api/establishments?${params.toString()}`, 'get')
  }

  signIn(email: string, password: string): Promise<AuthenticatedSessionModel> {
    return this.request('/api/auth/admins', 'post', { email, password })
  }

  list(): Promise<EstablishmentSummaryModel[]> {
    return this.request('/api/admin/establishments', 'get')
  }

  create(params: NewEstablishmentParams): Promise<EstablishmentSummaryModel> {
    return this.request('/api/admin/establishments', 'post', params)
  }

  setActive(id: string, active: boolean): Promise<EstablishmentSummaryModel> {
    return this.request(
      `/api/admin/establishments/${encodeURIComponent(id)}/active`,
      'patch',
      { active },
    )
  }

  resetManagerPassword(
    id: string,
    password: string,
  ): Promise<EstablishmentSummaryModel> {
    return this.request(
      `/api/admin/establishments/${encodeURIComponent(id)}/manager-password`,
      'patch',
      { password },
    )
  }

  private async request<T>(
    path: string,
    method: 'get' | 'post' | 'patch',
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
