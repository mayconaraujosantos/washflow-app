import { RemoteOperations } from '@/data/usecases/operations/remote-operations'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteOperations = () =>
  new RemoteOperations(env.apiUrl, new FetchHttpClient())
