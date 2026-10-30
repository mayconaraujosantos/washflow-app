import { RemoteAuthenticateCustomer } from '@/data/usecases/authenticate-customer/remote-authenticate-customer'
import { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteAuthenticateCustomer = (): AuthenticateCustomer => {
  return new RemoteAuthenticateCustomer(
    `${env.apiUrl}/api/auth/customers`,
    new FetchHttpClient(),
  )
}
