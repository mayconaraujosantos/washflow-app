import { RemoteAccess } from '@/data/usecases/access/remote-access'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteAccess = () =>
  new RemoteAccess(env.apiUrl, new FetchHttpClient())
