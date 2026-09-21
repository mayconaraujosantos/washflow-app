import { RemoteListServicePrices } from '@/data/usecases/list-service-prices/remote-list-service-prices'
import { ListServicePrices } from '@/domain/usecases/list-service-prices'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteListServicePrices = (): ListServicePrices => {
  return new RemoteListServicePrices(
    `${env.apiUrl}/api/services`,
    new FetchHttpClient(),
  )
}
