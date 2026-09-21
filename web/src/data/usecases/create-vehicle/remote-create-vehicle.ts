import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { CreateVehicle } from '@/domain/usecases/create-vehicle'

type ErrorBody = { error?: string }

export class RemoteCreateVehicle implements CreateVehicle {
  constructor(
    private readonly url: string,
    private readonly httpClient: HttpClient<CreateVehicle.Model>,
  ) {}

  async create(params: CreateVehicle.Params): Promise<CreateVehicle.Model> {
    const httpResponse = await this.httpClient.request({
      url: this.url,
      method: 'post',
      body: params,
    })

    switch (httpResponse.statusCode) {
      case HttpStatusCode.ok:
        return httpResponse.body as CreateVehicle.Model
      case HttpStatusCode.badRequest:
        throw new Error(
          (httpResponse.body as ErrorBody)?.error ??
            'Campo obrigatório ausente',
        )
      case HttpStatusCode.notFound:
        throw new Error(
          (httpResponse.body as ErrorBody)?.error ?? 'Cliente não encontrado',
        )
      case HttpStatusCode.conflict:
        throw new Error(
          (httpResponse.body as ErrorBody)?.error ?? 'Placa já cadastrada',
        )
      default:
        throw new Error('Erro inesperado ao cadastrar o veículo')
    }
  }
}
