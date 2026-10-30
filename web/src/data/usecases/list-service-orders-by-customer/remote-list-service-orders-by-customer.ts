import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { ListServiceOrdersByCustomer } from '@/domain/usecases/list-service-orders-by-customer'

export class RemoteListServiceOrdersByCustomer implements ListServiceOrdersByCustomer {
  constructor(
    private readonly urlPrefix: string,
    private readonly httpClient: HttpClient<ListServiceOrdersByCustomer.Model>,
  ) {}

  async list(
    params: ListServiceOrdersByCustomer.Params,
  ): Promise<ListServiceOrdersByCustomer.Model> {
    const httpResponse = await this.httpClient.request({
      url: `${this.urlPrefix}/${params.customerId}/service-orders`,
      method: 'get',
    })

    switch (httpResponse.statusCode) {
      case HttpStatusCode.ok:
        return httpResponse.body as ListServiceOrdersByCustomer.Model
      default:
        throw new Error('Erro inesperado ao carregar os agendamentos')
    }
  }
}
