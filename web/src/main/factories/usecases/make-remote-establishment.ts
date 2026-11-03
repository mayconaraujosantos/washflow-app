import { RemoteEstablishment } from '@/data/usecases/establishment/remote-establishment'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteEstablishment = () =>
  new RemoteEstablishment(env.apiUrl, new FetchHttpClient())
