import { RemoteLoadHealthStatus } from '@/data/usecases/load-health-status/remote-load-health-status'
import { LoadHealthStatus } from '@/domain/usecases/load-health-status'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteLoadHealthStatus = (): LoadHealthStatus => {
  return new RemoteLoadHealthStatus(
    `${env.apiUrl}/api/health`,
    new FetchHttpClient(),
  )
}
