import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { ScheduleServiceOrder } from '@/domain/usecases/schedule-service-order'

type ErrorBody = { error?: string }

export class RemoteScheduleServiceOrder implements ScheduleServiceOrder {
  constructor(
    private readonly url: string,
    private readonly httpClient: HttpClient<ScheduleServiceOrder.Model>,
  ) {}

  async schedule(
    params: ScheduleServiceOrder.Params,
  ): Promise<ScheduleServiceOrder.Model> {
    const httpResponse = await this.httpClient.request({
      url: this.url,
      method: 'post',
      body: params,
    })

    switch (httpResponse.statusCode) {
      case HttpStatusCode.ok:
        return httpResponse.body as ScheduleServiceOrder.Model
      case HttpStatusCode.badRequest:
        throw new Error(
          (httpResponse.body as ErrorBody)?.error ??
            'Horário inválido para agendamento',
        )
      case HttpStatusCode.notFound:
        throw new Error(
          (httpResponse.body as ErrorBody)?.error ??
            'Veículo ou serviço não encontrado',
        )
      case HttpStatusCode.conflict:
        throw new Error(
          (httpResponse.body as ErrorBody)?.error ??
            'Horário sem vagas disponíveis',
        )
      default:
        throw new Error('Erro inesperado ao agendar')
    }
  }
}
