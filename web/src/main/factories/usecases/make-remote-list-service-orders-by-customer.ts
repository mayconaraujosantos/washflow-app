import { RemoteListServiceOrdersByCustomer } from '@/data/usecases/list-service-orders-by-customer/remote-list-service-orders-by-customer'
import { ListServiceOrdersByCustomer } from '@/domain/usecases/list-service-orders-by-customer'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteListServiceOrdersByCustomer =
  (): ListServiceOrdersByCustomer => {
    return new RemoteListServiceOrdersByCustomer(
      `${env.apiUrl}/api/customers`,
      new FetchHttpClient(),
    )
  }
