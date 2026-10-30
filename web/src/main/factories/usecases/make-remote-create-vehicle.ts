import { RemoteCreateVehicle } from '@/data/usecases/create-vehicle/remote-create-vehicle'
import { CreateVehicle } from '@/domain/usecases/create-vehicle'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteCreateVehicle = (): CreateVehicle => {
  return new RemoteCreateVehicle(
    `${env.apiUrl}/api/vehicles`,
    new FetchHttpClient(),
  )
}
