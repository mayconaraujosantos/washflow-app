import { RemoteListVehiclesByCustomer } from '@/data/usecases/list-vehicles-by-customer/remote-list-vehicles-by-customer'
import { ListVehiclesByCustomer } from '@/domain/usecases/list-vehicles-by-customer'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteListVehiclesByCustomer = (): ListVehiclesByCustomer => {
  return new RemoteListVehiclesByCustomer(
    `${env.apiUrl}/api/customers`,
    new FetchHttpClient(),
  )
}
