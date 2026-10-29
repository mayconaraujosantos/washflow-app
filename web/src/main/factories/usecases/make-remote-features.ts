import { RemoteFeatures } from '@/data/usecases/features/remote-features'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteFeatures = () =>
  new RemoteFeatures(env.apiUrl, new FetchHttpClient())
