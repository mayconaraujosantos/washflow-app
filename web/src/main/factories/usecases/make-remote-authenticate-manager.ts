import { RemoteAuthenticateManager } from '@/data/usecases/authenticate-manager/remote-authenticate-manager'
import { AuthenticateManager } from '@/domain/usecases/authenticate-manager'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteAuthenticateManager = (): AuthenticateManager => {
  return new RemoteAuthenticateManager(
    `${env.apiUrl}/api/auth/managers`,
    new FetchHttpClient(),
  )
}
