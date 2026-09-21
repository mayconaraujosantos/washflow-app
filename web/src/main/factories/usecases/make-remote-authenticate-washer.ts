import { RemoteAuthenticateWasher } from '@/data/usecases/authenticate-washer/remote-authenticate-washer'
import { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteAuthenticateWasher = (): AuthenticateWasher => {
  return new RemoteAuthenticateWasher(
    `${env.apiUrl}/api/auth/washers`,
    new FetchHttpClient(),
  )
}
