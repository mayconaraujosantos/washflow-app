import { VehicleModel } from '@/domain/models/vehicle-model'

export interface ListVehiclesByCustomer {
  list: (
    params: ListVehiclesByCustomer.Params,
  ) => Promise<ListVehiclesByCustomer.Model>
  /**
   * Cars the counter entered with this customer's phone and no account yet;
   * registering the same plate (CreateVehicle) links one to the customer.
   */
  listUnclaimed: (
    params: ListVehiclesByCustomer.Params,
  ) => Promise<ListVehiclesByCustomer.Model>
}

export namespace ListVehiclesByCustomer {
  export type Params = {
    customerId: string
  }

  export type Model = VehicleModel[]
}
