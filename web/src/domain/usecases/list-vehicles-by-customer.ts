import { VehicleModel } from '@/domain/models/vehicle-model'

export interface ListVehiclesByCustomer {
  list: (
    params: ListVehiclesByCustomer.Params,
  ) => Promise<ListVehiclesByCustomer.Model>
}

export namespace ListVehiclesByCustomer {
  export type Params = {
    customerId: string
  }

  export type Model = VehicleModel[]
}
