import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { ListVehiclesByCustomer } from '@/domain/usecases/list-vehicles-by-customer'

export class RemoteListVehiclesByCustomer implements ListVehiclesByCustomer {
  constructor(
    private readonly urlPrefix: string,
    private readonly httpClient: HttpClient<ListVehiclesByCustomer.Model>,
  ) {}

  async list(
    params: ListVehiclesByCustomer.Params,
  ): Promise<ListVehiclesByCustomer.Model> {
    const httpResponse = await this.httpClient.request({
      url: `${this.urlPrefix}/${params.customerId}/vehicles`,
      method: 'get',
    })

    switch (httpResponse.statusCode) {
      case HttpStatusCode.ok:
        return httpResponse.body as ListVehiclesByCustomer.Model
      default:
        throw new Error('Erro inesperado ao carregar os veículos')
    }
  }
}
