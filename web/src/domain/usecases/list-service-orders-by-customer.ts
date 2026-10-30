import { ServiceOrderModel } from '@/domain/models/service-order-model'

export interface ListServiceOrdersByCustomer {
  list: (
    params: ListServiceOrdersByCustomer.Params,
  ) => Promise<ListServiceOrdersByCustomer.Model>
}

export namespace ListServiceOrdersByCustomer {
  export type Params = {
    customerId: string
  }

  export type Model = ServiceOrderModel[]
}
