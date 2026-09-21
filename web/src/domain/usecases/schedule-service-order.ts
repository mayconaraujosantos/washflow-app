import { ServiceOrderModel } from '@/domain/models/service-order-model'

export interface ScheduleServiceOrder {
  schedule: (
    params: ScheduleServiceOrder.Params,
  ) => Promise<ScheduleServiceOrder.Model>
}

export namespace ScheduleServiceOrder {
  export type Params = {
    vehicleId: string
    servicePriceId: number
    scheduledAt: string
  }

  export type Model = ServiceOrderModel
}
