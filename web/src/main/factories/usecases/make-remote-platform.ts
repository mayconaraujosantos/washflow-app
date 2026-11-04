import { RemotePlatform } from '@/data/usecases/platform/remote-platform'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemotePlatform = () =>
  new RemotePlatform(env.apiUrl, new FetchHttpClient())
