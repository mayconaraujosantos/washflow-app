import { ServicePriceModel } from '@/domain/models/service-price-model'

export interface ListServicePrices {
  list: () => Promise<ListServicePrices.Model>
}

export namespace ListServicePrices {
  export type Model = ServicePriceModel[]
}
