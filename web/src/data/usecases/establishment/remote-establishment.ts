import type { HttpClient } from '@/data/protocols/http/http-client'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type {
  EstablishmentModel,
  ScheduleDayModel,
  SetupStatusModel,
  WhatsappConnectionModel,
} from '@/domain/models/establishment-model'
import type {
  Establishment,
  EstablishmentParams,
  SetupParams,
} from '@/domain/usecases/establishment'

type ErrorBody = { error?: string }

const WHATSAPP_CONNECTION = '/api/whatsapp/connection'

export class RemoteEstablishment implements Establishment {
  constructor(
    private readonly baseUrl: string,
    private readonly httpClient: HttpClient,
  ) {}

  setupStatus(): Promise<SetupStatusModel> {
    return this.request<SetupStatusModel>('/api/setup', 'get')
  }

  setup(params: SetupParams): Promise<AuthenticatedSessionModel> {
    return this.request<AuthenticatedSessionModel>('/api/setup', 'post', params)
  }

  load(): Promise<EstablishmentModel> {
    return this.request<EstablishmentModel>('/api/establishment', 'get')
  }

  update(params: EstablishmentParams): Promise<EstablishmentModel> {
    return this.request<EstablishmentModel>('/api/establishment', 'put', params)
  }

  availability(date: string): Promise<ScheduleDayModel> {
    const query = new URLSearchParams({ date })
    return this.request<ScheduleDayModel>(
      `/api/schedule/availability?${query.toString()}`,
      'get',
    )
  }

  whatsappStatus(): Promise<WhatsappConnectionModel> {
    return this.request<WhatsappConnectionModel>(WHATSAPP_CONNECTION, 'get')
  }

  connectWhatsapp(): Promise<WhatsappConnectionModel> {
    return this.request<WhatsappConnectionModel>(WHATSAPP_CONNECTION, 'post')
  }

  disconnectWhatsapp(): Promise<WhatsappConnectionModel> {
    return this.request<WhatsappConnectionModel>(WHATSAPP_CONNECTION, 'delete')
  }

  private async request<T>(
    path: string,
    method: 'get' | 'post' | 'put' | 'delete',
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
