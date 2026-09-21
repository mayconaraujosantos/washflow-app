import { VehicleModel } from '@/domain/models/vehicle-model'

export interface CreateVehicle {
  create: (params: CreateVehicle.Params) => Promise<CreateVehicle.Model>
}

export namespace CreateVehicle {
  export type Params = {
    customerId: string
    plate: string
    model?: string
    color?: string
  }

  export type Model = VehicleModel
}
