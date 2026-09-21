import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { ListServicePrices } from '@/domain/usecases/list-service-prices'

export class RemoteListServicePrices implements ListServicePrices {
  constructor(
    private readonly url: string,
    private readonly httpClient: HttpClient<ListServicePrices.Model>,
  ) {}

  async list(): Promise<ListServicePrices.Model> {
    const httpResponse = await this.httpClient.request({
      url: this.url,
      method: 'get',
    })

    switch (httpResponse.statusCode) {
      case HttpStatusCode.ok:
        return httpResponse.body as ListServicePrices.Model
      default:
        throw new Error('Erro inesperado ao carregar os serviços')
    }
  }
}
