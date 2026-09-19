import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { LoadHealthStatus } from '@/domain/usecases/load-health-status'

export class RemoteLoadHealthStatus implements LoadHealthStatus {
  constructor(
    private readonly url: string,
    private readonly httpClient: HttpClient<LoadHealthStatus.Model>
  ) {}

  async load(): Promise<LoadHealthStatus.Model> {
    const httpResponse = await this.httpClient.request({ url: this.url, method: 'get' })

    switch (httpResponse.statusCode) {
      case HttpStatusCode.ok:
        return httpResponse.body as LoadHealthStatus.Model
      default:
        throw new Error('Unexpected error while loading health status')
    }
  }
}
